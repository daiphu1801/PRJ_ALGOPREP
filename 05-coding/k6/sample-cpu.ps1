# Samples host and per-process CPU every ~2 s while a k6 run is in progress.
# Usage: powershell -File sample-cpu.ps1 -BackendPid <pid> -Out <csv> -StopFile <path>
# Percent values are of the whole machine (all logical cores) except go_judge_docker, which is
# `docker stats` percent of ONE core and can exceed 100.
param(
  [Parameter(Mandatory)][int]$BackendPid,
  [Parameter(Mandatory)][string]$Out,
  [Parameter(Mandatory)][string]$StopFile
)
$cores = [Environment]::ProcessorCount
function Snap {
  $h = @{}
  foreach ($p in Get-Process -ErrorAction SilentlyContinue) {
    if ($p.Id -eq $BackendPid -or $p.ProcessName -in @('k6', 'vmmemWSL', 'vmmem')) {
      try { $h["$($p.ProcessName)#$($p.Id)"] = $p.TotalProcessorTime.TotalSeconds } catch {}
    }
  }
  $h
}
function Pct($a, $b, $match, $secs) {
  $sum = 0.0
  foreach ($k in $b.Keys) {
    if ($k -like $match -and $a.ContainsKey($k)) { $sum += ($b[$k] - $a[$k]) }
  }
  [math]::Round(100 * $sum / $secs / $cores, 1)
}
"time,host_cpu,backend,k6,vmmem,go_judge_docker" | Out-File $Out -Encoding ascii
while (-not (Test-Path $StopFile)) {
  $a = Snap
  $t0 = Get-Date
  $host_cpu = [math]::Round((Get-Counter '\Processor(_Total)\% Processor Time' -SampleInterval 2).CounterSamples[0].CookedValue, 1)
  $b = Snap
  $secs = ((Get-Date) - $t0).TotalSeconds
  $gj = (& docker stats --no-stream --format '{{.CPUPerc}}' algoprep-go-judge 2>$null) -replace '%', ''
  $row = @((Get-Date -Format 'HH:mm:ss'), $host_cpu, (Pct $a $b 'java#*' $secs), (Pct $a $b 'k6#*' $secs), (Pct $a $b 'vmmem*' $secs), $gj) -join ','
  $row | Out-File $Out -Append -Encoding ascii
}
