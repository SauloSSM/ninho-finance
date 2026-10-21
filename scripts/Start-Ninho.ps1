param(
    [ValidateNotNullOrEmpty()][string]$DatabasePath,
    [ValidateSet('Both','Backend','Frontend')][string]$Role = 'Both',
    [ValidateNotNullOrEmpty()][string]$DatasourceUrl,
    [switch]$ShowConfiguration
)
. "$PSScriptRoot/LocalOperations.ps1"
if ($Role -eq 'Frontend' -and !$ShowConfiguration) {
    $configuration = $null
} else {
    $configuration = Resolve-StartupDatasource -DatabasePath $DatabasePath -DatasourceUrl $DatasourceUrl
    Write-Host "Datasource efetivo ($($configuration.Source)): $($configuration.DatasourceUrl)"
    Write-Host "Banco SQLite: $($configuration.DatabasePath)"
    Write-Host "Diretorio de execucao do backend: $($configuration.WorkingDirectory)"
}
if ($ShowConfiguration) { return $configuration }
Write-Host 'Acesso local: backend http://127.0.0.1:8080; frontend http://localhost:5173'
if ($Role -ne 'Frontend') { Assert-LocalPortAvailable 8080 }
if ($Role -ne 'Backend') { Assert-LocalPortAvailable 5173 -IncludeIPv6 }
if ($Role -eq 'Both') {
    # Interactive consoles let Ctrl+C reach Java/Node and show startup failures.
    $shell = (Get-Command powershell.exe).Source
    foreach ($part in @('Backend', 'Frontend')) {
        $arguments = '-NoProfile -NoExit -File "{0}" -Role {1}' -f $PSCommandPath, $part
        if ($part -eq 'Backend') { $arguments += ' -DatasourceUrl "{0}"' -f $configuration.DatasourceUrl }
        Start-Process -FilePath $shell -ArgumentList $arguments | Out-Null
    }
    Write-Host 'Duas janelas abertas; confira a prontidao em CADA janela. Para encerrar, use Ctrl+C em CADA janela e aguarde o termino.'
    return
}
if ($Role -eq 'Frontend') {
    Push-Location (Join-Path $RepoRoot 'frontend')
    try {
        if (!(Test-Path 'node_modules')) {
            & npm.cmd ci
            if ($LASTEXITCODE -ne 0) { throw 'Falha ao instalar frontend. Confira Node.js, npm e conexao.' }
        }
        & npm.cmd run dev -- --host localhost --port 5173 --strictPort
        if ($LASTEXITCODE -ne 0) { throw 'Frontend encerrou com erro. Confira as mensagens acima.' }
    } finally { Pop-Location }
    return
}
$database = $configuration.DatabasePath
[void][IO.Directory]::CreateDirectory((Split-Path $database -Parent))
$operation = Lock-DatabaseOperation $database
Push-Location $configuration.WorkingDirectory
try {
    Initialize-SqliteTool
    & ./mvnw.cmd -q -DskipTests compile
    if ($LASTEXITCODE -ne 0) { throw 'Backend nao compilou. Configure JAVA_HOME para JDK 25 e confira o erro Maven acima.' }
    & $Java --enable-native-access=ALL-UNNAMED -cp "target/classes;$AppClasspath" br.com.saulossm.ninho.NinhoApplication "--spring.datasource.url=$($configuration.DatasourceUrl)" '--server.address=127.0.0.1' '--server.port=8080'
    if ($LASTEXITCODE -ne 0) { throw 'Backend encerrou com erro. Confira Java, porta 8080 e as mensagens acima.' }
} finally {
    Pop-Location
    $operation.Dispose()
}
