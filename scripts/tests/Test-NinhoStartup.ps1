param([switch]$Integration)
. "$PSScriptRoot/../LocalOperations.ps1"
$launcher = Join-Path $RepoRoot 'scripts/Start-Ninho.ps1'
$originalUrl = $env:SPRING_DATASOURCE_URL
$testRoot = Join-Path $RepoRoot ('backend/target/startup-check-' + [guid]::NewGuid().ToString('N'))
$processes = [Collections.Generic.List[Diagnostics.Process]]::new()

function Assert-Equal($Actual, $Expected, [string]$Name) {
    if ($Actual -cne $Expected) { throw "${Name}: esperado '$Expected', recebido '$Actual'" }
}
function Assert-Rejected([scriptblock]$Action) {
    $rejected = $false
    try { & $Action | Out-Null } catch { $rejected = $true }
    if (!$rejected) { throw 'Datasource invalido/ambiguo deveria ser recusado.' }
}
function Assert-Configuration([hashtable]$Parameters, [string]$ExpectedPath, [string]$ExpectedSource) {
    $messages = @(& $launcher @Parameters -ShowConfiguration 6>&1)
    $config = @($messages | Where-Object { $_ -is [pscustomobject] -and $_.PSObject.Properties['DatasourceUrl'] })
    Assert-Equal $config.Count 1 'configuracao unica'
    Assert-Equal $config[0].DatabasePath $ExpectedPath 'caminho'
    Assert-Equal $config[0].DatasourceUrl "jdbc:sqlite:$ExpectedPath" 'URL'
    Assert-Equal $config[0].Source $ExpectedSource 'precedencia'
    Assert-Equal $config[0].WorkingDirectory (Join-Path $RepoRoot 'backend') 'diretorio Java'
    $display = ($messages | Where-Object { $_ -is [Management.Automation.InformationRecord] } | ForEach-Object { $_.ToString() }) -join "`n"
    if (!$display.Contains("Banco SQLite: $ExpectedPath") -or !$display.Contains("Datasource efetivo (${ExpectedSource}): jdbc:sqlite:$ExpectedPath")) {
        throw 'Exibicao diverge do datasource selecionado.'
    }
}
function Start-OwnedLauncher([string]$Role, [string]$Extra, [string]$Name) {
    $arguments = '-NoProfile -File "{0}" -Role {1} {2}' -f $launcher, $Role, $Extra
    $process = Start-Process -FilePath (Get-Command powershell.exe).Source -ArgumentList $arguments -WorkingDirectory $testRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $testRoot "$Name.out.log") -RedirectStandardError (Join-Path $testRoot "$Name.err.log")
    $null = $process.Handle
    $processes.Add($process)
    return $process
}
function Stop-OwnedLauncher([Diagnostics.Process]$Process) {
    # Only the still-owned launcher and its descendants; never search/kill by port/name.
    if (!$Process.HasExited) {
        & taskkill.exe /PID $Process.Id /T /F | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Falha ao encerrar arvore criada pelo teste: $($Process.Id)" }
        if (!$Process.WaitForExit(10000)) { throw 'Inicializador de teste nao encerrou.' }
    }
}
function Wait-Http([string]$Url, [Diagnostics.Process]$Process) {
    $deadline = (Get-Date).AddSeconds(90)
    while ((Get-Date) -lt $deadline) {
        if ($Process.HasExited) { throw "Inicializador falhou; veja os logs em $testRoot" }
        try { return Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 2 } catch { Start-Sleep -Milliseconds 300 }
    }
    throw "Servidor nao ficou pronto: $Url. Veja $testRoot"
}

try {
    # These checks only resolve strings. Even the default is never opened/stat'ed.
    $env:SPRING_DATASOURCE_URL = $null
    Assert-Configuration @{} (Join-Path $RepoRoot 'backend/data/ninho.db') 'padrao'
    $env:SPRING_DATASOURCE_URL = 'jdbc:sqlite:./target/ninho-start-check.db'
    Assert-Configuration @{} (Join-Path $RepoRoot 'backend/target/ninho-start-check.db') 'SPRING_DATASOURCE_URL'
    Assert-Configuration @{DatasourceUrl='jdbc:sqlite:./target/explicit.db'} (Join-Path $RepoRoot 'backend/target/explicit.db') '-DatasourceUrl'
    $absolute = Join-Path $testRoot 'absolute with spaces.db'
    Assert-Configuration @{DatasourceUrl="jdbc:sqlite:$absolute"} $absolute '-DatasourceUrl'
    Assert-Configuration @{DatabasePath=$absolute} $absolute '-DatabasePath'
    [void][IO.Directory]::CreateDirectory($testRoot)
    Push-Location $testRoot
    try {
        Assert-Configuration @{} (Join-Path $RepoRoot 'backend/target/ninho-start-check.db') 'SPRING_DATASOURCE_URL'
        Assert-Configuration @{DatabasePath='./relative.db'} (Join-Path $testRoot 'relative.db') '-DatabasePath'
        Assert-Rejected { & $launcher -DatabasePath $absolute -DatasourceUrl "jdbc:sqlite:$absolute" -ShowConfiguration }
        foreach ($url in @('jdbc:postgresql:test', 'jdbc:sqlite:', 'jdbc:sqlite::memory:', 'jdbc:sqlite:file:other.db', 'jdbc:sqlite:./target/test.db?mode=memory', 'jdbc:sqlite:C:\test.db:stream', 'jdbc:sqlite:C:relative.db', 'jdbc:sqlite:\relative.db', 'jdbc:sqlite:   ')) {
            $env:SPRING_DATASOURCE_URL = $url
            Assert-Rejected { & $launcher -ShowConfiguration }
            Assert-Rejected { & $launcher -DatasourceUrl $url -ShowConfiguration }
        }
        # Explicit paths must override even an invalid inherited URL.
        Assert-Configuration @{DatabasePath=$absolute} $absolute '-DatabasePath'
    } finally { Pop-Location }
    Write-Host 'PASS: padrao, ambiente, parametros, precedencia, exibicao, caminhos e recusas sem abrir bancos'

    # Exercise Both without opening windows: inspect the actual child command lines.
    # The only intercepted operation is process creation; datasource resolution is real.
    $captured = [Collections.Generic.List[string]]::new()
    $captureProcess = { param($FilePath, $ArgumentList) $captured.Add($ArgumentList) }.GetNewClosure()
    Set-Item Function:\Start-Process $captureProcess
    try {
        foreach ($selection in @('environment', 'url', 'path')) {
            $captured.Clear()
            $env:SPRING_DATASOURCE_URL = "jdbc:sqlite:$absolute"
            $parameters = @{}
            if ($selection -eq 'url') { $parameters.DatasourceUrl = "jdbc:sqlite:$absolute" }
            if ($selection -eq 'path') { $parameters.DatabasePath = $absolute }
            & $launcher @parameters | Out-Null
            Assert-Equal $captured.Count 2 'duas janelas'
            $expected = '-Role Backend -DatasourceUrl "jdbc:sqlite:{0}"' -f $absolute
            if (!$captured[0].Contains($expected)) { throw 'Janela backend perdeu o datasource selecionado.' }
            if (!$captured[1].EndsWith('-Role Frontend')) { throw 'Janela frontend recebeu selecao de banco desnecessaria.' }
        }
    } finally { Remove-Item Function:\Start-Process }
    Write-Host 'PASS: Both repassa o datasource absoluto do ambiente e de cada parametro para a janela backend'

    if ($Integration) {
        Assert-LocalPortAvailable 8080
        Assert-LocalPortAvailable 5173 -IncludeIPv6
        Initialize-SqliteTool
        $env:SPRING_DATASOURCE_URL = 'invalid-inherited-url'
        $frontend = Start-OwnedLauncher 'Frontend' '' 'frontend'
        $null = Wait-Http 'http://localhost:5173' $frontend
        $client = Invoke-WebRequest -UseBasicParsing 'http://localhost:5173/src/api/client.ts'
        if (!$client.Content.Contains('http://127.0.0.1:8080/api')) { throw 'Frontend nao aponta para a API local.' }
        Write-Host 'PASS: frontend Vite pronto e cliente servido aponta para backend IPv4'
        $relativeDirectory = './target/' + (Split-Path $testRoot -Leaf)
        foreach ($mode in @('environment', 'url', 'path')) {
            $db = Join-Path $testRoot "$mode with spaces.db"
            $extra = ''
            if ($mode -eq 'environment') {
                $env:SPRING_DATASOURCE_URL = "jdbc:sqlite:$relativeDirectory/$mode with spaces.db"
            } else {
                $env:SPRING_DATASOURCE_URL = 'invalid-inherited-url'
                if ($mode -eq 'url') { $extra = '-DatasourceUrl "jdbc:sqlite:{0}/{1} with spaces.db"' -f $relativeDirectory, $mode }
                else { $extra = '-DatabasePath "{0} with spaces.db"' -f $mode }
            }
            $backend = Start-OwnedLauncher 'Backend' $extra $mode
            try {
                $null = Wait-Http 'http://127.0.0.1:8080/api/people' $backend
                $response = Invoke-WebRequest -UseBasicParsing 'http://127.0.0.1:8080/api/people' -Headers @{Origin='http://localhost:5173'}
                Assert-Equal $response.Headers['Access-Control-Allow-Origin'] 'http://localhost:5173' 'CORS GET'
                $preflight = Invoke-WebRequest -UseBasicParsing 'http://127.0.0.1:8080/api/people' -Method Options -Headers @{Origin='http://localhost:5173'; 'Access-Control-Request-Method'='POST'; 'Access-Control-Request-Headers'='content-type'}
                if (!$preflight.Headers['Access-Control-Allow-Methods'].Contains('POST')) { throw 'Preflight CORS falhou.' }
                $log = Get-Content -Raw -LiteralPath (Join-Path $testRoot "$mode.out.log")
                if (!$log.Contains("Banco SQLite: $db") -or !$log.Contains("jdbc:sqlite:$db")) { throw 'Log diverge do caminho esperado.' }
                if (!$log.Contains('Started NinhoApplication') -or !(Test-Path -LiteralPath $db -PathType Leaf)) { throw 'Backend de teste nao confirmou prontidao com o banco descartavel.' }
                $marker = 'startup-' + [guid]::NewGuid().ToString('N')
                $null = Invoke-RestMethod 'http://127.0.0.1:8080/api/people' -Method Post -ContentType 'application/json' -Body (@{name=$marker; type='USER'} | ConvertTo-Json)
                # Read only this freshly generated file: a marker from the live API
                # proves that the running backend wrote to the advertised file.
                & $Java --enable-native-access=ALL-UNNAMED -cp "$(Join-Path $RepoRoot 'backend/target/classes');$AppClasspath" (Join-Path $PSScriptRoot 'OperationsFixture.java') 'assert-startup' $db $marker
                if ($LASTEXITCODE -ne 0) { throw 'Banco efetivamente utilizado diverge do anunciado.' }
                Write-Host "PASS: $mode iniciou pelo script; API, CORS e SQLite comprovados em $db"
            } finally { Stop-OwnedLauncher $backend }
        }
    }
} finally {
    foreach ($process in $processes) { Stop-OwnedLauncher $process }
    $env:SPRING_DATASOURCE_URL = $originalUrl
    Write-Host "Artefatos preservados em: $testRoot"
}
