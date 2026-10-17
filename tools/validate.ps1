# One deterministic foundation entry point. Add -Smokes for the complete isolated inventory.
param(
    [string]$Root = '.',
    [switch]$Smokes
)

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path -LiteralPath $Root).Path
$runId = (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssfffZ')
$logDir = Join-Path $Root "build\validation\$runId"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding($false)
$ledger = Join-Path $logDir 'status.tsv'
[System.IO.File]::WriteAllText($ledger, "check`tstarted_utc`tfinished_utc`texit_code`tlog`n", $utf8)
$engine = Join-Path $PSHOME 'powershell.exe'

function Invoke-Check([string]$Name, [scriptblock]$Action) {
    $started = (Get-Date).ToUniversalTime().ToString('o')
    $log = Join-Path $logDir "$Name.log"
    $global:LASTEXITCODE = -1
    $previousPreference = $ErrorActionPreference
    try {
        # Native compilers write ordinary diagnostics to stderr; their exit code is authoritative.
        $ErrorActionPreference = 'Continue'
        & $Action 2>&1 | Out-File -LiteralPath $log -Encoding utf8
        $code = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    $finished = (Get-Date).ToUniversalTime().ToString('o')
    [System.IO.File]::AppendAllText($ledger, "$Name`t$started`t$finished`t$code`t$Name.log`n", $utf8)
    Write-Output "$Name exit=$code (build/validation/$runId/$Name.log)"
    if ($code -ne 0) {
        Get-Content -LiteralPath $log -Tail 35 | Write-Output
        throw "Validation failed: $Name"
    }
}

Push-Location $Root
try {
    $head = git rev-parse HEAD
    if ($LASTEXITCODE -ne 0) { throw 'Could not identify the source revision.' }
    $dirty = git status --porcelain
    [System.IO.File]::WriteAllText((Join-Path $logDir 'source.txt'),
            "HEAD=$head`nWorking directory=repository root`n$($dirty -join "`n")`n", $utf8)
    Invoke-Check 'environment' { & (Join-Path $Root 'gradlew.bat') --version }
    Invoke-Check 'build' { & (Join-Path $Root 'gradlew.bat') --no-daemon --console=plain :common:compileJava build }
    Invoke-Check 'behavior' { & $engine -NoProfile -ExecutionPolicy Bypass -File (Join-Path $Root 'tools\run-checks.ps1') -Root $Root }
    Invoke-Check 'documentation' { & $engine -NoProfile -ExecutionPolicy Bypass -File (Join-Path $Root 'tools\check-links.ps1') -Root $Root }
    if ($Smokes) {
        Invoke-Check 'smokes' { & $engine -NoProfile -ExecutionPolicy Bypass -File (Join-Path $Root 'tools\run-smokes.ps1') -Root $Root }
    } else {
        Write-Output 'Full smokes not requested; this is the fast gate, not integration acceptance.'
    }
    Write-Output "Requested validation completed. Ledger: build/validation/$runId/status.tsv"
} catch {
    $failure = $_
    [System.IO.File]::AppendAllText($ledger, "ERROR`t$((Get-Date).ToUniversalTime().ToString('o'))`t`t1`trunner-error.log`n", $utf8)
    $failure | Out-String | Out-File -LiteralPath (Join-Path $logDir 'runner-error.log') -Encoding utf8
    Write-Output $failure.Exception.Message
    exit 1
} finally {
    Pop-Location
}
exit 0
