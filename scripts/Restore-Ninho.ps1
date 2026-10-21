param(
    [Parameter(Mandatory=$true)][string]$BackupPath,
    [string]$DatabasePath,
    [switch]$ConfirmReplace,
    [ValidateRange(1,65535)][int]$BackendPort = 8080
)
. "$PSScriptRoot/LocalOperations.ps1"
$operation = $null
$exclusive = $null
$backupHandle = $null
$stage = $null
try {
    $database = Resolve-DatabasePath $DatabasePath
    $backup = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($BackupPath)
    Write-Host "Destino que sera substituido: $database"
    Write-Host "Backup escolhido: $backup"
    if (!$ConfirmReplace) {
        if ((Read-Host 'Digite RESTAURAR para substituir os dados') -cne 'RESTAURAR') { throw 'Restauracao cancelada; confirmacao explicita obrigatoria.' }
    }
    if ($database -eq $backup) { throw 'Backup e destino devem ser arquivos diferentes.' }
    if (!(Test-Path -LiteralPath $database -PathType Leaf)) { throw 'Destino inexistente. Este procedimento exige um banco atual para preservar.' }
    Assert-BackendStopped $BackendPort
    $operation = Lock-DatabaseOperation $database
    # Deny all readers/writers, including idle SQLite connections. Allow only the
    # atomic rename used by File.Replace; keep this handle until replacement ends.
    try { $exclusive = [IO.File]::Open($database, 'Open', 'ReadWrite', 'Delete') }
    catch { throw 'Banco em uso ou sem permissao exclusiva. Encerre completamente todos os backends e ferramentas SQLite.' }
    foreach ($suffix in @('-wal', '-shm', '-journal')) {
        if (Test-Path -LiteralPath "$database$suffix") { throw "Arquivo auxiliar presente: $database$suffix. Nao o apague. Inicie e encerre normalmente o backend antes de tentar novamente." }
    }
    Initialize-SqliteTool
    # Prevent changes to the supplied snapshot during validation and staging.
    $backupHandle = [IO.File]::Open($backup, 'Open', 'Read', 'Read')
    Invoke-SqliteTool @('validate', $backup, $Migrations)
    $stage = "$database.restore-stage-$([guid]::NewGuid().ToString('N'))"
    Invoke-SqliteTool @('backup', $backup, $stage, $Migrations)
    $flush = [IO.File]::Open($stage, 'Open', 'ReadWrite', 'None')
    try { $flush.Flush($true) } finally { $flush.Dispose() }
    $preservedDirectory = Join-Path (Split-Path $database -Parent) 'backups'
    [void][IO.Directory]::CreateDirectory($preservedDirectory)
    $preserved = Join-Path $preservedDirectory (New-OperationName 'before-restore')
    Assert-BackendStopped $BackendPort
    # Same-volume atomic replacement. The original is preserved by Windows itself.
    [IO.File]::Replace($stage, $database, $preserved)
    Write-Host "Restauracao concluida: $database"
    Write-Host "Banco anterior preservado em: $preserved"
} catch { throw "Restauracao recusada ou falhou: $($_.Exception.Message)" }
finally {
    if ($backupHandle) { $backupHandle.Dispose() }
    if ($exclusive) { $exclusive.Dispose() }
    if ($operation) { $operation.Dispose() }
    if ($stage -and (Test-Path -LiteralPath $stage)) { Remove-Item -LiteralPath $stage }
}
