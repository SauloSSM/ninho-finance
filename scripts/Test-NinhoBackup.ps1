param([Parameter(Mandatory=$true)][string]$BackupPath)
. "$PSScriptRoot/LocalOperations.ps1"
Initialize-SqliteTool
Invoke-SqliteTool @('validate', $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($BackupPath), $Migrations)
