# All database paths are generated under a fresh OS temporary directory.
. "$PSScriptRoot/../LocalOperations.ps1"
Initialize-SqliteTool
$temporary = Join-Path ([IO.Path]::GetTempPath()) ('ninho-safety-' + [guid]::NewGuid().ToString('N'))
[void][IO.Directory]::CreateDirectory($temporary)
$database = Join-Path $temporary 'test.db'
$backupDir = Join-Path $temporary 'backups'
$fixture = Join-Path $PSScriptRoot 'OperationsFixture.java'
$tools = Split-Path $PSScriptRoot -Parent
$fixtureClasspath = "$(Join-Path $RepoRoot 'backend/target/classes');$AppClasspath"
$background = $null
$stopFile = $null
$listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
$listener.Start()
$backendPort = $listener.LocalEndpoint.Port
$listener.Stop()

function Invoke-Fixture([string[]]$FixtureArguments) {
    & $Java --enable-native-access=ALL-UNNAMED -cp $fixtureClasspath $fixture @FixtureArguments
    if ($LASTEXITCODE -ne 0) { throw 'Fixture de teste falhou.' }
}
function Assert-Rejected([scriptblock]$Action, [string]$Name) {
    $rejected = $false
    try { & $Action } catch { $rejected = $true; Write-Host "Recusa esperada ($Name): $($_.Exception.Message)" }
    if (!$rejected) { throw "Operacao deveria ter sido recusada: $Name" }
    Write-Host "PASS: $Name"
}
function Start-Fixture([string]$Mode) {
    $script:readyFile = Join-Path $temporary "$Mode.ready"
    $script:stopFile = Join-Path $temporary "$Mode.stop"
    $arguments = '--enable-native-access=ALL-UNNAMED -cp "{0}" "{1}" {2} "{3}" "{4}" "{5}"' -f $fixtureClasspath, $fixture, $Mode, $database, $readyFile, $stopFile
    $script:background = Start-Process -FilePath $Java -ArgumentList $arguments -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $temporary "$Mode.out.log") -RedirectStandardError (Join-Path $temporary "$Mode.err.log")
    # Keep a process handle so Windows PowerShell can read ExitCode after exit.
    $null = $background.Handle
    $deadline = (Get-Date).AddSeconds(60)
    while (!(Test-Path -LiteralPath $readyFile)) {
        if ($background.HasExited -or (Get-Date) -gt $deadline) { throw "Fixture $Mode nao iniciou. Veja $temporary" }
        Start-Sleep -Milliseconds 200
    }
}
function Stop-Fixture {
    if ($background) {
        [IO.File]::WriteAllText($stopFile, 'stop')
        if (!$background.WaitForExit(30000)) { throw "Fixture nao encerrou: $($background.Id)" }
        $exitCode = $background.ExitCode
        $script:background = $null
        if ($exitCode -ne 0) { throw "Fixture encerrou com erro ($exitCode). Veja $temporary" }
    }
}

Push-Location $temporary
try {
    if ((Resolve-DatabasePath '.\test.db') -ne $database) { throw 'Caminho relativo ignorou o diretorio PowerShell.' }
    $portProbe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
    $portProbe.Start()
    try { Assert-Rejected { Assert-LocalPortAvailable $portProbe.LocalEndpoint.Port } 'conflito de porta' }
    finally { $portProbe.Stop() }
    Invoke-Fixture @('init', $database, $Migrations)
    Write-Host 'PASS: banco temporario novo, migrations e dados gravados'
    & "$tools/Backup-Ninho.ps1" -DatabasePath '.\test.db' -BackupDirectory '.\backups'
    $backup = (Get-ChildItem -LiteralPath $backupDir -Filter 'ninho-*.db' | Select-Object -First 1).FullName
    & "$tools/Test-NinhoBackup.ps1" -BackupPath (Join-Path '.\backups' (Split-Path $backup -Leaf))
    Invoke-Fixture @('change', $database)
    Invoke-Fixture @('assert', $database, 'after')
    & "$tools/Restore-Ninho.ps1" -DatabasePath '.\test.db' -BackupPath (Join-Path '.\backups' (Split-Path $backup -Leaf)) -ConfirmReplace -BackendPort $backendPort
    Invoke-Fixture @('assert', $database, 'before')
    $preserved = (Get-ChildItem -LiteralPath (Join-Path $temporary 'backups') -Filter 'before-restore-*.db' | Select-Object -First 1).FullName
    Invoke-Fixture @('assert', $preserved, 'after')
    & "$tools/Test-NinhoBackup.ps1" -BackupPath $database
    Write-Host 'PASS: backup, alteracao, restauracao, dados, copia anterior, integridade e foreign keys'

    $invalid = Join-Path $temporary 'invalid.db'
    [IO.File]::WriteAllText($invalid, 'not a sqlite database')
    $beforeHash = (Get-FileHash -LiteralPath $database).Hash
    $portProbe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
    $portProbe.Start()
    try { Assert-Rejected { & "$tools/Restore-Ninho.ps1" -DatabasePath $database -BackupPath $backup -ConfirmReplace -BackendPort $portProbe.LocalEndpoint.Port } 'porta de backend ocupada' }
    finally { $portProbe.Stop() }
    Assert-Rejected { & "$tools/Restore-Ninho.ps1" -DatabasePath $database -BackupPath $invalid -ConfirmReplace -BackendPort $backendPort } 'backup invalido'
    if ((Get-FileHash -LiteralPath $database).Hash -ne $beforeHash) { throw 'Backup invalido alterou destino!' }
    $incompatible = Join-Path $temporary 'incompatible.db'
    [IO.File]::Copy($backup, $incompatible)
    Invoke-Fixture @('incompatible', $incompatible)
    Assert-Rejected { & "$tools/Restore-Ninho.ps1" -DatabasePath $database -BackupPath $incompatible -ConfirmReplace -BackendPort $backendPort } 'estrutura incompativel'
    Invoke-SqliteTool @('validate', $database, $Migrations)

    $foreignKey = Join-Path $temporary 'foreign-key.db'
    [IO.File]::Copy($backup, $foreignKey)
    Invoke-Fixture @('foreign-key', $foreignKey)
    Assert-Rejected { & "$tools/Restore-Ninho.ps1" -DatabasePath $database -BackupPath $foreignKey -ConfirmReplace -BackendPort $backendPort } 'foreign key invalida'
    function Read-Host { return 'CANCELAR' }
    try { Assert-Rejected { & "$tools/Restore-Ninho.ps1" -DatabasePath $database -BackupPath $backup -BackendPort $backendPort } 'sem confirmacao explicita' }
    finally { Remove-Item Function:\Read-Host }
    if ((Get-FileHash -LiteralPath $database).Hash -ne $beforeHash) { throw 'Operacao recusada alterou destino!' }

    $idleHandle = [IO.File]::Open($database, 'Open', 'Read', 'ReadWrite')
    try { Assert-Rejected { & "$tools/Restore-Ninho.ps1" -DatabasePath $database -BackupPath $backup -ConfirmReplace -BackendPort $backendPort } 'arquivo aberto ocioso' }
    finally { $idleHandle.Dispose() }
    $operationLock = Lock-DatabaseOperation $database
    try { Assert-Rejected { & "$tools/Restore-Ninho.ps1" -DatabasePath $database -BackupPath $backup -ConfirmReplace -BackendPort $backendPort } 'bloqueio do inicializador' }
    finally { $operationLock.Dispose() }
    $journal = "$database-journal"
    [IO.File]::WriteAllText($journal, 'test')
    try { Assert-Rejected { & "$tools/Restore-Ninho.ps1" -DatabasePath $database -BackupPath $backup -ConfirmReplace -BackendPort $backendPort } 'journal pendente' }
    finally { Remove-Item -LiteralPath $journal }

    Start-Fixture 'boot'
    Write-Host 'PASS: aplicacao Spring Boot iniciou com o banco restaurado'
    Assert-Rejected { & "$tools/Restore-Ninho.ps1" -DatabasePath $database -BackupPath $backup -ConfirmReplace -BackendPort $backendPort } 'backend Spring Boot ativo'
    & "$tools/Backup-Ninho.ps1" -DatabasePath $database -BackupDirectory $backupDir
    Write-Host 'PASS: backup com backend Spring Boot ativo'
    Stop-Fixture

    Start-Fixture 'idle'
    Assert-Rejected { & "$tools/Restore-Ninho.ps1" -DatabasePath $database -BackupPath $backup -ConfirmReplace -BackendPort $backendPort } 'conexao SQLite ociosa'
    Stop-Fixture

    Start-Fixture 'wal'
    Assert-Rejected { & "$tools/Restore-Ninho.ps1" -DatabasePath $database -BackupPath $backup -ConfirmReplace -BackendPort $backendPort } 'conexao WAL ativa'
    $walBackup = Join-Path $temporary 'wal-backup.db'
    Invoke-SqliteTool @('backup', $database, $walBackup, $Migrations)
    Invoke-Fixture @('assert', $walBackup, 'wal-committed')
    Write-Host 'PASS: backup online WAL inclui commit e exclui transacao nao confirmada'
    Stop-Fixture
    Assert-Rejected { Invoke-SqliteTool @('backup', $database, $walBackup, $Migrations) } 'sobrescrita de backup'
    Invoke-Fixture @('assert', $walBackup, 'wal-committed')
    Write-Host "TODOS OS TESTES PASSARAM. Artefatos temporarios: $temporary"
} finally {
    Stop-Fixture
    Pop-Location
    # Preserve the temporary artifacts and logs for inspection; never delete a database.
    Write-Host "Diretorio temporario dos testes: $temporary"
}
