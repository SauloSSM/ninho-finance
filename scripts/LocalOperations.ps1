Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$script:RepoRoot = Split-Path $PSScriptRoot -Parent
$script:Migrations = Join-Path $RepoRoot 'backend/src/main/resources/db/migration'

function Resolve-DatabasePath([string]$Path) {
    if (!$Path) { $Path = Join-Path $RepoRoot 'backend/data/ninho.db' }
    return $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Path)
}

# Startup only: backup/restore deliberately retain their explicit path semantics.
function Resolve-StartupDatasource([string]$DatabasePath, [string]$DatasourceUrl) {
    if ($DatabasePath -and $DatasourceUrl) { throw 'Use apenas -DatabasePath ou -DatasourceUrl.' }
    $backendDirectory = Join-Path $RepoRoot 'backend'
    if ($DatabasePath) {
        $source = '-DatabasePath'
        $database = Resolve-DatabasePath $DatabasePath
    } else {
        $source = '-DatasourceUrl'
        if (!$DatasourceUrl) {
            $DatasourceUrl = $env:SPRING_DATASOURCE_URL
            $source = 'SPRING_DATASOURCE_URL'
        }
        if (!$DatasourceUrl) {
            $DatasourceUrl = 'jdbc:sqlite:./data/ninho.db'
            $source = 'padrao'
        }
        if (!$DatasourceUrl.StartsWith('jdbc:sqlite:', [StringComparison]::Ordinal)) {
            throw 'Datasource invalido: use jdbc:sqlite: seguido de um caminho de arquivo local.'
        }
        $path = $DatasourceUrl.Substring('jdbc:sqlite:'.Length)
        # URI/memory/query forms can select another file or bypass file locking.
        # Refuse them rather than guessing the file protected by backup/restore.
        if ([string]::IsNullOrWhiteSpace($path) -or $path -match '^(file:|:memory:)' -or $path.Contains('?')) {
            throw 'Use um arquivo SQLite em disco, sem URI file:, :memory: ou parametros de query.'
        }
        if ($path -match '^[A-Za-z]:($|[^\\/])' -or $path -match '^[\\/]') {
            throw 'Caminho JDBC ambiguo: informe a unidade no caminho absoluto ou use ./ para um relativo ao backend.'
        }
        if (![IO.Path]::IsPathRooted($path)) { $path = Join-Path $backendDirectory $path }
        $database = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($path)
    }
    if ($database.StartsWith('\\') -or $database -notmatch '^[A-Za-z]:[\\/]' -or
        $database.Substring(2).Contains(':') -or $database.IndexOfAny([IO.Path]::GetInvalidPathChars()) -ge 0 -or
        $database -match '[?*"<>|\x00-\x1f]' -or $database -match '[\\/]$') {
        throw 'Use um caminho de arquivo SQLite local absoluto ou relativo valido.'
    }
    [pscustomobject]@{
        DatabasePath = $database
        DatasourceUrl = "jdbc:sqlite:$database"
        Source = $source
        WorkingDirectory = $backendDirectory
    }
}

function Assert-LocalPortAvailable([int]$Port, [switch]$IncludeIPv6) {
    $addresses = @([Net.IPAddress]::Loopback)
    if ($IncludeIPv6 -and [Net.Sockets.Socket]::OSSupportsIPv6) { $addresses += [Net.IPAddress]::IPv6Loopback }
    foreach ($address in $addresses) {
        $listener = [Net.Sockets.TcpListener]::new($address, $Port)
        try {
            $listener.Server.ExclusiveAddressUse = $true
            $listener.Start()
        } catch {
            throw "Porta $Port indisponivel em ${address}. Verifique com netstat -ano; nenhum processo foi encerrado. Detalhe: $($_.Exception.GetBaseException().Message)"
        } finally { $listener.Stop() }
    }
}

function Initialize-SqliteTool {
    $script:Java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { (Get-Command java.exe -ErrorAction Stop).Source }
    if (!(Test-Path -LiteralPath $Java)) { throw 'Java nao encontrado. Configure JAVA_HOME para um JDK 25.' }
    $classpathFile = Join-Path $RepoRoot 'backend/target/local-operations.classpath'
    if (!(Test-Path -LiteralPath $classpathFile)) {
        Push-Location (Join-Path $RepoRoot 'backend')
        try {
            & ./mvnw.cmd -q dependency:build-classpath '-Dmdep.outputFile=target/local-operations.classpath'
            if ($LASTEXITCODE -ne 0) { throw 'Maven falhou ao localizar o driver SQLite. Confira JAVA_HOME (JDK 25) e acesso as dependencias.' }
        } finally { Pop-Location }
    }
    $script:AppClasspath = (Get-Content -Raw -LiteralPath $classpathFile).Trim()
    $script:SqliteClasspath = ($AppClasspath.Split(';') | Where-Object { $_ -match '[\\/]sqlite-jdbc[\\/]' }) -join ';'
    if (!$SqliteClasspath -or !(Test-Path -LiteralPath $SqliteClasspath)) {
        throw 'Driver SQLite ausente. Apague apenas backend/target/local-operations.classpath e tente novamente.'
    }
}

function Invoke-SqliteTool([string[]]$ToolArguments) {
    & $Java --enable-native-access=ALL-UNNAMED --class-path $SqliteClasspath (Join-Path $PSScriptRoot 'SqliteSafety.java') @ToolArguments
    if ($LASTEXITCODE -ne 0) { throw 'Operacao SQLite recusada; veja o erro acima.' }
}

function New-OperationName([string]$Prefix) {
    return "$Prefix-$(Get-Date -Format 'yyyyMMdd-HHmmss-fff')-$([guid]::NewGuid().ToString('N')).db"
}

function Lock-DatabaseOperation([string]$Database) {
    try { return [IO.File]::Open("$Database.operations.lock", 'OpenOrCreate', 'ReadWrite', 'None') }
    catch { throw "Backend ou outra operacao em andamento para $Database. Encerre o backend e tente novamente." }
}

function Assert-BackendStopped([int]$Port = 8080) {
    $client = New-Object Net.Sockets.TcpClient
    try {
        $connection = $client.ConnectAsync('127.0.0.1', $Port)
        try { [void]$connection.Wait(3000) } catch {
            $reason = $_.Exception.GetBaseException()
            if ($reason -is [Net.Sockets.SocketException] -and $reason.SocketErrorCode -eq 'ConnectionRefused') { return }
            throw "Falha ao verificar backend na porta ${Port}: $($reason.Message)"
        }
        if ($client.Connected) { throw "Porta $Port em uso. Encerre completamente o backend antes de restaurar." }
        if (!$connection.IsCompleted) { throw "Nao foi possivel confirmar que a porta $Port esta livre." }
    } finally { $client.Dispose() }
}
