param([string]$DatabasePath, [string]$BackupDirectory)
. "$PSScriptRoot/LocalOperations.ps1"
try {
    $database = Resolve-DatabasePath $DatabasePath
    if (!(Test-Path -LiteralPath $database -PathType Leaf)) { throw "Banco nao encontrado: $database" }
    if (!$BackupDirectory) { $BackupDirectory = Join-Path $RepoRoot 'backups' }
    $directory = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($BackupDirectory)
    [void][IO.Directory]::CreateDirectory($directory)
    Initialize-SqliteTool
    $destination = Join-Path $directory (New-OperationName 'ninho')
    Write-Host "Banco de origem: $database"
    Invoke-SqliteTool @('backup', $database, $destination, $Migrations)
} catch { throw "Backup falhou: $($_.Exception.Message)" }
