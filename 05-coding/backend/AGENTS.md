# Backend agent instructions

Root project instructions still apply. This file adds the backend validation-log convention.

## Command logs

- Save compile, test, Spotless, mutation and verification command output under
  `05-coding/backend/logs/`, relative to this checkout. Do not write `*.log` beside `pom.xml`.
- Create the directory when needed. Use a unique name such as
  `YYYYMMDD-HHmmss-fff-<slice>-<phase>.log`; preserve failed attempts and never overwrite
  another run. Each worktree uses its own backend/logs directory, not the main checkout's.
- Capture stdout and stderr. Preserve the Maven exit code before running another command;
  a successful redirection or evidence-extraction command does not make a failed build pass.
- Choose test scope from the task and impact first. This storage convention does not require
  extra tests or a full-reactor build. A docs/log-only change does not require Maven.
- In the report, give the log path relative to the repository, command, scope, timestamp,
  exit code and actual evidence lines. Logs remain local and ignored by Git; do not force-add them.
- Historical root-level logs were moved unchanged into `logs/` on 2026-11-01. Older reports
  naming `backend/<name>.log` refer now to `backend/logs/<name>.log`.

PowerShell example, from `05-coding/backend` (adapt module, phase and test selectors to the task):

```powershell
New-Item -ItemType Directory -Path .\logs -Force | Out-Null
$runStamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$logFile = ".\logs\$runStamp-f3-p6-verify.log"
if (Test-Path -LiteralPath $logFile) { throw "Log already exists: $logFile" }
$mavenArgs = @('-pl', 'algoprep-harness', '-am', 'verify')
& .\mvnw.cmd @mavenArgs *> $logFile
$mavenExit = $LASTEXITCODE
Select-String -LiteralPath $logFile -Pattern 'Tests run:|Checkstyle violations|BugInstance size|coverage checks|BUILD (SUCCESS|FAILURE)'
Write-Output "Maven exit code: $mavenExit; log: $logFile"
if ($mavenExit -ne 0) { throw "Maven failed with exit code $mavenExit; see $logFile" }
```

This applies to build/test command logs; it does not change application runtime logging.
