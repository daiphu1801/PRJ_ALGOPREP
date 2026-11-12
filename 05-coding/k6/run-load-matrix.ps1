# Runs A-6/A-7 only after the operator has verified the dedicated DB/Rabbit vhost/Redis DB/backend.
param(
  [Parameter(Mandatory)][string]$Fixture,
  [Parameter(Mandatory)][string]$OutDir,
  [Parameter(Mandatory)][string]$ConfigurationNote,
  [switch]$IsolatedStackConfirmed,
  [string]$BaseUrl = 'http://localhost:8081/api/v1',
  [string]$WsOrigin = 'http://localhost:3000',
  [int[]]$Levels = @(40, 80, 120),
  [int]$Repeats = 2,
  [string]$Hold = '180s',
  [int]$MaxWait = 120,
  [int]$DrainTimeout = 180,
  [int]$Cooldown = 15,
  [int]$BackendPid = 0,
  [string]$PgContainer = 'algoprep-postgres',
  [string]$PgUser = 'algoprep',
  [string]$BackendJar = ''
)
$ErrorActionPreference = 'Stop'
if (-not $IsolatedStackConfirmed) { throw 'Verify DB algoprep_load, dedicated Rabbit vhost/Redis DB and only the intended worker, then use -IsolatedStackConfirmed.' }
# Keep this measurement process awake without changing the machine's power plan.
# Windows Modern Standby invalidated an earlier row; the OS releases the request when this process exits.
if (-not ('AlgoPrepLoadExecutionState' -as [type])) {
  Add-Type -TypeDefinition @'
using System.Runtime.InteropServices;
public static class AlgoPrepLoadExecutionState {
  [DllImport("kernel32.dll")]
  public static extern uint SetThreadExecutionState(uint flags);
}
'@
}
if ([AlgoPrepLoadExecutionState]::SetThreadExecutionState([uint32]2147483649) -eq 0) {
  throw 'Cannot hold the system awake for a valid load measurement.'
}
$fixturePath = (Resolve-Path -LiteralPath $Fixture).Path
$data = Get-Content -Raw -LiteralPath $fixturePath | ConvertFrom-Json
if ($data.dbName -ne 'algoprep_load' -or $data.baseUrl.TrimEnd('/') -ne $BaseUrl.TrimEnd('/')) { throw 'Fixture DB/baseUrl differs from the confirmed load target.' }
if ($Repeats -lt 1 -or $Levels.Count -lt 1 -or $Levels.Where({ $_ -lt 1 -or $_ -gt 500 }).Count -gt 0) { throw 'Invalid matrix dimensions.' }
$outputPath = [IO.Path]::GetFullPath($OutDir)
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null
$manifestPath = Join-Path $outputPath 'matrix-manifest.json'
if (Test-Path -LiteralPath $manifestPath) { throw 'Manifest already exists; use a new output folder to preserve evidence.' }
$scriptPath = Join-Path $PSScriptRoot 'submit-ui-load.js'
$k6 = (Get-Command k6 -ErrorAction Stop).Source
$version = (& $k6 version | Out-String).Trim()
$jarHash = if ($BackendJar) { (Get-FileHash -LiteralPath $BackendJar -Algorithm SHA256).Hash } else { $null }
$manifest = [ordered]@{
  startedAt = (Get-Date).ToString('o'); completedAt = $null; dbName = 'algoprep_load'; baseUrl = $BaseUrl
  fixtureRunId = $data.runId; problemId = $data.problemId; testcaseCount = $data.testcaseCount
  languages = @('JAVA','CPP','PYTHON'); levels = $Levels; repeats = $Repeats; hold = $Hold
  maxWaitSeconds = $MaxWait; minIterationSeconds = 3.1; cooldownSeconds = $Cooldown
  configuration = $ConfigurationNote; backendPid = $BackendPid; backendJarSha256 = $jarHash
  k6Version = $version; loadGeneratorSharesHost = $true; rows = @()
  idleSleepPreventedForMeasurement = $true
}
function Save-Manifest {
  $manifest | ConvertTo-Json -Depth 9 | Set-Content -LiteralPath $manifestPath -Encoding UTF8
}
function Wait-LoadIdle([string]$Evidence) {
  $deadline = (Get-Date).AddSeconds($DrainTimeout)
  'observedAt,activeSubmissions' | Set-Content -LiteralPath $Evidence -Encoding UTF8
  do {
    $value = & docker exec $PgContainer psql -v ON_ERROR_STOP=1 -U $PgUser -d algoprep_load -At -c "SELECT count(*) FROM judge.submissions WHERE status IN ('PENDING','COMPILING','JUDGING')"
    if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect isolated queue; stop without purging.' }
    $count = 0
    if (-not [int]::TryParse(($value | Out-String).Trim(), [ref]$count)) { throw 'Queue check returned an invalid count.' }
    ((Get-Date).ToString('o') + ',' + $count) | Add-Content -LiteralPath $Evidence -Encoding UTF8
    if ($count -eq 0) { return }
    Start-Sleep -Seconds 2
  } while ((Get-Date) -lt $deadline)
  throw 'Queue drain timed out; matrix stopped. No pending work was purged.'
}
Save-Manifest
for ($repeat = 1; $repeat -le $Repeats; $repeat++) {
  # Reverse mode order on the second repetition to make time-of-run bias visible.
  $modes = if ($repeat % 2 -eq 1) { @('poll','ui') } else { @('ui','poll') }
  foreach ($vus in $Levels) {
    foreach ($mode in $modes) {
      $rowId = "$mode-r$repeat-v$vus"
      if (-not $data.accountSets.PSObject.Properties[$rowId]) { throw "Fixture lacks dedicated actors for $rowId" }
      $rowDir = Join-Path $outputPath $rowId
      New-Item -ItemType Directory -Force -Path $rowDir | Out-Null
      Wait-LoadIdle (Join-Path $rowDir 'queue-before.csv')
      $row = [ordered]@{ id = $rowId; mode = $mode; repeat = $repeat; vus = $vus; startedAt = (Get-Date).ToString('o'); endedAt = $null; k6ExitCode = $null; drain = 'pending' }
      $manifest.rows += $row
      Save-Manifest
      $stopFile = Join-Path $rowDir 'cpu-stop.flag'
      $cpuJob = $null
      if ($BackendPid -gt 0) {
        $cpuJob = Start-Job -FilePath (Join-Path $PSScriptRoot 'sample-cpu.ps1') -ArgumentList $BackendPid,(Join-Path $rowDir 'cpu.csv'),$stopFile
      }
      try {
        # Windows PowerShell wraps native stderr as ErrorRecord even for ordinary k6 warnings.
        # Preserve the native exit code instead of terminating on stderr before the manifest is saved.
        $previousPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
          & $k6 run -e "FIXTURE=$fixturePath" -e "BASE_URL=$BaseUrl" -e "WS_ORIGIN=$WsOrigin" -e "MODE=$mode" -e "VUS=$vus" -e "REPEAT=$repeat" -e "HOLD=$Hold" -e "MAX_WAIT=$MaxWait" --summary-export (Join-Path $rowDir 'summary.json') --out "csv=$(Join-Path $rowDir 'metrics.csv')" $scriptPath *> (Join-Path $rowDir 'k6.log')
          $row.k6ExitCode = $LASTEXITCODE
        } finally { $ErrorActionPreference = $previousPreference }
      } finally {
        if ($cpuJob) {
          New-Item -ItemType File -Path $stopFile -Force | Out-Null
          $cpuJob | Wait-Job -Timeout 10 | Out-Null
          if ($cpuJob.State -eq 'Running') { $cpuJob | Stop-Job }
          $cpuJob | Receive-Job *> (Join-Path $rowDir 'cpu-sampler.log')
          $cpuJob | Remove-Job
        }
        $row.endedAt = (Get-Date).ToString('o')
        Save-Manifest
      }
      try { Wait-LoadIdle (Join-Path $rowDir 'queue-after.csv'); $row.drain = 'idle' }
      catch { $row.drain = 'timeout-or-unavailable'; Save-Manifest; throw }
      Save-Manifest
      # Nonzero k6 threshold exits stay in the manifest; never relabel a failed row as passed.
      if ($row.k6ExitCode -notin @(0,99)) { throw "k6 infrastructure/script failure in $rowId (exit $($row.k6ExitCode)); see its log." }
      Start-Sleep -Seconds $Cooldown
    }
  }
}
$manifest.completedAt = (Get-Date).ToString('o')
Save-Manifest
if ($manifest.rows.Where({ $_.k6ExitCode -ne 0 }).Count -gt 0) { exit 1 }
exit 0
