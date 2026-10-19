#requires -Version 5.1
<#
.SYNOPSIS
Run the fixed Grindless Sync/build/deploy workflow over the existing personal-PC SSH.
.EXAMPLE
.\tools\pc-workflow.ps1 -Action Sync -Revision <full-commit-sha>
.EXAMPLE
.\tools\pc-workflow.ps1 -Action BuildAndDeploy -Revision <full-commit-sha>
.EXAMPLE
.\tools\pc-workflow.ps1 -Action Build -Revision <full-commit-sha>
.DESCRIPTION
Host paths and SSH identity come only from the private pc.json, never Git. No action
changes authentication, Git configuration, branches in the clone, or global Java/Prism
settings. Sync is separate and never resets, cleans, forces or repairs a dirty worktree.
Deploy requires an explicit remote PackZip. No action launches Prism or Minecraft.
#>
[CmdletBinding()]
param(
    [ValidateSet('Inspect', 'Sync', 'Build', 'BuildAndDeploy', 'Deploy')][string]$Action = 'Inspect',
    [string]$Revision,
    [string]$PackZip,
    [string]$ConfigFile = (Join-Path $HOME '.config/grindless/pc.json')
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2.0

function Assert-PcRevision([string]$Value) {
    if ($Value -cnotmatch '^[0-9a-f]{40}$') { throw 'Revision must be an exact lowercase full commit SHA.' }
}

function ConvertTo-PcRemotePath([string]$Value) {
    if (-not $Value -or $Value.Length -gt 2048 -or $Value -cnotmatch '^[A-Za-z]:[\\/].+' -or
        $Value.Substring(2) -match '[\x00-\x1f<>:"|?*]' -or $Value -match '(^|[\\/])\.{1,2}([\\/]|$)') {
        throw 'Private configuration requires absolute local Windows PC paths without traversal or devices.'
    }
    $path = $Value.Replace('/', '\').TrimEnd('\')
    foreach ($part in $path.Substring(3).Split('\')) {
        if (-not $part -or $part.EndsWith('.') -or $part.EndsWith(' ')) { throw 'Unsafe private PC path segment.' }
    }
    return $path
}

function Test-PcWithin([string]$Path, [string]$Parent) {
    return $Path.Equals($Parent, [StringComparison]::OrdinalIgnoreCase) -or
        $Path.StartsWith($Parent.TrimEnd('\') + '\', [StringComparison]::OrdinalIgnoreCase)
}

function Read-PcWorkflowConfig([string]$Path) {
    $path = [IO.Path]::GetFullPath($Path)
    $repository = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
    if ($path.Equals($repository, [StringComparison]::OrdinalIgnoreCase) -or
        $path.StartsWith($repository.TrimEnd([char[]]'\/') + [IO.Path]::DirectorySeparatorChar,
            [StringComparison]::OrdinalIgnoreCase)) { throw 'Keep the private PC configuration outside the repository.' }
    $item = Get-Item -LiteralPath $path -Force -ErrorAction Stop
    if ($item.PSIsContainer -or ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0 -or
        $item.Length -gt 64KB) { throw 'Private PC configuration must be a small regular JSON file.' }
    $config = [IO.File]::ReadAllText($path, [Text.Encoding]::UTF8) | ConvertFrom-Json -ErrorAction Stop
    $keys = @('schema', 'schema_version', 'sshTarget', 'clonePath', 'worktreePath', 'gradleUserHome',
        'prismRoot', 'templateInstanceId', 'instanceId', 'javaHome', 'sourceBranch',
        'receivedSourceRevision', 'personalGitSshAlias', 'javawSha256')
    if ($null -eq $config -or $config -isnot [System.Management.Automation.PSCustomObject] -or
        @($config.PSObject.Properties).Count -ne $keys.Count) { throw 'Invalid private PC configuration schema.' }
    foreach ($key in $keys) {
        if ($key -cnotin @($config.PSObject.Properties.Name)) { throw 'Missing private PC configuration field.' }
        if ($key -ne 'schema_version' -and ($config.$key -isnot [string] -or -not $config.$key)) {
            throw 'Private PC configuration fields must be nonempty strings.'
        }
    }
    if ($config.schema -cne 'grindless.pc-workflow' -or $config.schema_version -ne 1 -or
        $config.sshTarget -cnotmatch '^[A-Za-z0-9][A-Za-z0-9._@:-]{0,254}$' -or
        $config.personalGitSshAlias -cnotmatch '^[A-Za-z0-9][A-Za-z0-9._@-]{0,254}$') {
        throw 'Invalid PC workflow schema or SSH identity; options/commands are not accepted.'
    }
    Assert-PcRevision $config.receivedSourceRevision
    if ($config.javawSha256 -cnotmatch '^[0-9a-f]{64}$') { throw 'Supply the verified Java 17 javaw.exe SHA256 in javawSha256.' }
    if ($config.sourceBranch -cnotmatch '^[A-Za-z0-9][A-Za-z0-9._/-]*$' -or
        $config.sourceBranch -match '(\.\.|//|/\.)' -or $config.sourceBranch.EndsWith('/') -or
        $config.sourceBranch.EndsWith('.lock')) { throw 'Invalid source branch label.' }
    if ($config.templateInstanceId -match '[\\/:*?"<>|\x00-\x1f]' -or
        $config.templateInstanceId.EndsWith('.') -or $config.templateInstanceId.EndsWith(' ') -or
        $config.templateInstanceId -in @('.', '..')) {
        throw 'Invalid private Prism template instance ID.'
    }
    if ($config.instanceId -cnotmatch '^Grindless-[A-Za-z0-9._+-]+-Dev$' -or
        $config.instanceId -ieq $config.templateInstanceId) { throw 'Use a separate named engineering development instance.' }
    foreach ($key in @('clonePath', 'worktreePath', 'gradleUserHome', 'prismRoot', 'javaHome')) {
        $config.$key = ConvertTo-PcRemotePath $config.$key
    }
    $writes = @($config.clonePath, $config.worktreePath, $config.gradleUserHome, $config.prismRoot)
    for ($a = 0; $a -lt $writes.Count; $a++) {
        for ($b = $a + 1; $b -lt $writes.Count; $b++) {
            if ((Test-PcWithin $writes[$a] $writes[$b]) -or (Test-PcWithin $writes[$b] $writes[$a])) {
                throw 'Clone, detached worktree, Gradle home and Prism root must be separate paths.'
            }
        }
        if ((Test-PcWithin $writes[$a] $config.javaHome) -or (Test-PcWithin $config.javaHome $writes[$a])) {
            throw 'The Java installation must be separate from workflow write targets.'
        }
    }
    return $config
}

# Fixed remote implementation, not a user-supplied command/script parameter. It travels
# as data over stdin; neither paths nor configuration values become PowerShell source.
$script:PcWorkflowRemote = {
    param($Request)
    $ErrorActionPreference = 'Stop'
    Set-StrictMode -Version 2.0
    $c = $Request.config
    $revision = [string]$Request.revision
    if ($Request.protocol -cne 'grindless.pc-workflow/1' -or
        $Request.action -cnotin @('Sync', 'Inspect', 'Build', 'BuildAndDeploy', 'Deploy') -or
        $revision -cnotmatch '^[0-9a-f]{40}$') { throw 'Invalid fixed PC workflow request.' }
    function SafePath([string]$Path, [switch]$Missing) {
        if ($Path -cnotmatch '^[A-Za-z]:[\\/].+' -or $Path -match '(^|[\\/])\.{1,2}([\\/]|$)' -or
            $Path.Substring(2) -match '[\x00-\x1f<>:"|?*]') { throw 'Unsafe PC path.' }
        $full = [IO.Path]::GetFullPath($Path).TrimEnd('\')
        $current = [IO.Path]::GetPathRoot($full)
        foreach ($segment in $full.Substring($current.Length).Split('\')) {
            if (-not $segment -or $segment.EndsWith('.') -or $segment.EndsWith(' ')) { throw 'Unsafe PC path segment.' }
            $current = Join-Path $current $segment
            if (Test-Path -LiteralPath $current) {
                if (((Get-Item -LiteralPath $current -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
                    throw 'PC workflow refuses reparse points/symlinks.'
                }
            }
        }
        if (-not $Missing -and -not (Test-Path -LiteralPath $full)) { throw 'Required PC path is missing.' }
        return $full
    }
    function InvokeRemoteGit([string]$At, [string[]]$Arguments, [int[]]$Allowed = @(0)) {
        $previous = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            $output = @(& git --no-pager -C $At @Arguments 2>&1)
            $code = $LASTEXITCODE
        } finally { $ErrorActionPreference = $previous }
        if ($code -notin $Allowed) { throw 'PC Git verification/synchronization failed; no reset, force or automatic repair was attempted.' }
        return [pscustomobject]@{ Code = $code; Text = ($output -join "`n").Trim() }
    }
    function CommonDir([string]$At) {
        $value = (InvokeRemoteGit $At @('rev-parse', '--git-common-dir')).Text
        if (-not [IO.Path]::IsPathRooted($value)) { $value = Join-Path $At $value }
        return SafePath $value
    }
    function ExecutionState([string]$At, [string]$Common) {
        if ((SafePath (InvokeRemoteGit $At @('rev-parse', '--show-toplevel')).Text) -ine $At -or
            (CommonDir $At) -ine $Common) { throw 'Execution path is not the clone-owned registered worktree.' }
        if ((InvokeRemoteGit $At @('symbolic-ref', '-q', 'HEAD') @(0, 1)).Code -ne 1) {
            throw 'Execution worktree must already be detached; no branch will be switched.'
        }
        if ((InvokeRemoteGit $At @('status', '--porcelain', '--untracked-files=normal')).Text) {
            throw 'Execution worktree is dirty; it is preserved without reset or clean.'
        }
        $head = (InvokeRemoteGit $At @('rev-parse', '--verify', 'HEAD^{commit}')).Text
        if ($head -cnotmatch '^[0-9a-f]{40}$') { throw 'Invalid execution worktree HEAD.' }
        return $head
    }
    function AssertExecutionIdle([string]$At) {
        try {
            $processes = @(Get-CimInstance -ClassName Win32_Process -Filter `
                "Name = 'java.exe' OR Name = 'javaw.exe' OR Name = 'java' OR Name = 'javaw'" -ErrorAction Stop)
        } catch { throw 'Cannot inspect execution-worktree build ownership; Sync is refused.' }
        foreach ($process in $processes) {
            if ([string]::IsNullOrWhiteSpace($process.CommandLine) -or [string]::IsNullOrWhiteSpace($process.ExecutablePath)) {
                throw 'Uninspectable execution-worktree build ownership; Sync is refused.'
            }
            $command = $process.CommandLine.Replace('\', '/').ToLowerInvariant()
            if ($command.Contains($At.Replace('\', '/').ToLowerInvariant()) -or
                ($command -match '(org\.gradle\.|gradledaemon|gradleworkermain)' -and
                    $command.Contains($c.gradleUserHome.Replace('\', '/').ToLowerInvariant()))) {
                throw 'An active/shared execution-worktree build must finish before Sync.'
            }
            # Prism and the player's game are not stopped or reconfigured by Sync.
        }
    }
    $clone = SafePath $c.clonePath
    $worktree = SafePath $c.worktreePath -Missing
    if (-not [IO.Directory]::Exists($clone) -or
        (SafePath (InvokeRemoteGit $clone @('rev-parse', '--show-toplevel')).Text) -ine $clone) {
        throw 'Configured PC clone must be the existing repository root.'
    }
    $common = CommonDir $clone
    $cloneHead = (InvokeRemoteGit $clone @('rev-parse', '--verify', 'HEAD^{commit}')).Text
    $cloneBranch = (InvokeRemoteGit $clone @('symbolic-ref', '-q', 'HEAD') @(0, 1)).Text
    $origin = (InvokeRemoteGit $clone @('remote', 'get-url', 'origin')).Text
    $alias = [Regex]::Escape(($c.personalGitSshAlias -split '@')[-1])
    if ($origin -notmatch ("^(?:[^@/\s]+@)?" + $alias + ":") -and
        $origin -notmatch ("^ssh://(?:[^@/\s]+@)?" + $alias + "(?::[0-9]+)?/")) {
        throw 'Clone origin does not use the configured existing personal Git SSH alias; it was not changed.'
    }
    $head = ''
    if (Test-Path -LiteralPath $worktree) { $head = ExecutionState $worktree $common }
    if ($Request.action -ceq 'Sync') {
        $syncLock = $null
        $syncLockPath = ''
        try {
            if ($head) {
                $buildDirectory = SafePath (Join-Path $worktree 'build') -Missing
                [IO.Directory]::CreateDirectory($buildDirectory) | Out-Null
                $syncLockPath = SafePath (Join-Path $buildDirectory '.grindless-prism-build.lock') -Missing
                try { $syncLock = [IO.File]::Open($syncLockPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None) }
                catch { throw 'An active or stale execution-worktree build lock exists; Sync is refused.' }
                AssertExecutionIdle $worktree
            }
            # Fetch ONLY the requested object through unchanged origin, not a branch/refspec.
            $null = InvokeRemoteGit $clone @('fetch', '--no-tags', '--no-recurse-submodules', 'origin', $revision)
            if ((InvokeRemoteGit $clone @('rev-parse', '--verify', ($revision + '^{commit}'))).Text -cne $revision) {
                throw 'Fetched object does not resolve to the exact requested commit.'
            }
            $null = InvokeRemoteGit $clone @('merge-base', '--is-ancestor', $c.receivedSourceRevision, $revision)
            if ($head) {
                $null = InvokeRemoteGit $clone @('merge-base', '--is-ancestor', $head, $revision)
                if ($head -cne $revision) { $null = InvokeRemoteGit $worktree @('checkout', '--detach', $revision) }
            } else { $null = InvokeRemoteGit $clone @('worktree', 'add', '--detach', $worktree, $revision) }
            if ((ExecutionState $worktree $common) -cne $revision) { throw 'Post-Sync exact revision verification failed.' }
            if ((InvokeRemoteGit $clone @('rev-parse', '--verify', 'HEAD^{commit}')).Text -cne $cloneHead -or
                (InvokeRemoteGit $clone @('symbolic-ref', '-q', 'HEAD') @(0, 1)).Text -cne $cloneBranch) {
                throw 'Clone HEAD unexpectedly changed; Sync did not report success.'
            }
            return [pscustomobject]@{ Action = 'Sync'; Outcome = 'VerifiedDetached'; Revision = $revision }
        } finally {
            if ($null -ne $syncLock) { $syncLock.Dispose(); [IO.File]::Delete($syncLockPath) }
        }
    }
    if (-not $head -or $head -cne $revision) { throw 'Run Sync first; execution worktree is not the exact requested revision.' }
    $null = InvokeRemoteGit $clone @('merge-base', '--is-ancestor', $c.receivedSourceRevision, $revision)
    $native = SafePath (Join-Path $worktree 'tools/prism-dev.ps1')
    $arguments = @{ Action = $Request.action; Root = $worktree; ExpectedRevision = $revision }
    if ($Request.action -cne 'Build') {
        $prism = SafePath $c.prismRoot
        $instances = SafePath (Join-Path $prism 'instances')
        $arguments.InstancesRoot = $instances
        $arguments.InstanceId = $c.instanceId
    }
    if ($Request.action -cne 'Inspect') {
        if ($Request.action -cne 'Build') {
            $arguments.TemplateInstance = SafePath (Join-Path $instances $c.templateInstanceId)
        }
        $javaHome = SafePath $c.javaHome
        $java = Join-Path $javaHome 'bin/javaw.exe'
        $arguments.JavaPath = SafePath $java
        if ((Get-FileHash -LiteralPath $java -Algorithm SHA256).Hash.ToLowerInvariant() -cne $c.javawSha256) {
            throw 'PC Java executable changed from the verified private javawSha256; no deployment was attempted.'
        }
        $arguments.JavaSha256 = $c.javawSha256
    }
    if ($Request.action -ceq 'Deploy') { $arguments.PackZip = SafePath $Request.packZip }
    $oldJava = $env:JAVA_HOME
    $oldGradle = $env:GRADLE_USER_HOME
    try {
        $env:JAVA_HOME = $c.javaHome
        $env:GRADLE_USER_HOME = SafePath $c.gradleUserHome -Missing
        & $native @arguments
    } finally { $env:JAVA_HOME = $oldJava; $env:GRADLE_USER_HOME = $oldGradle }
}

function New-PcWorkflowPacket($Config, [string]$Action, [string]$Revision, [string]$PackZip) {
    Assert-PcRevision $Revision
    if ($Action -cnotin @('Inspect', 'Sync', 'Build', 'BuildAndDeploy', 'Deploy')) { throw 'Unknown workflow action.' }
    if ($Action -ceq 'Deploy') { $PackZip = ConvertTo-PcRemotePath $PackZip }
    elseif ($PackZip) { throw 'PackZip is only accepted for explicit Deploy.' }
    $code = $script:PcWorkflowRemote.ToString()
    $bytes = [Text.Encoding]::UTF8.GetBytes($code)
    $hash = [Security.Cryptography.SHA256]::Create()
    try { $digest = ([BitConverter]::ToString($hash.ComputeHash($bytes))).Replace('-', '').ToLowerInvariant() }
    finally { $hash.Dispose() }
    # ASCII-only, bounded bootstrap; private paths and action data are stdin JSON.
    $bootstrap = @'
$ErrorActionPreference='Stop'
$ProgressPreference='SilentlyContinue'
[Console]::InputEncoding=New-Object Text.UTF8Encoding($false)
try {
 $p=[Console]::In.ReadToEnd()|ConvertFrom-Json
 if($p.protocol -cne 'grindless.pc-workflow/1'){throw 'Invalid workflow packet'}
 $b=[Convert]::FromBase64String($p.code)
 $h=[Security.Cryptography.SHA256]::Create()
 try{$d=([BitConverter]::ToString($h.ComputeHash($b))).Replace('-','').ToLowerInvariant()}finally{$h.Dispose()}
 if($d -cne 'CODE_DIGEST'){throw 'Workflow implementation hash mismatch'}
 $s=[ScriptBlock]::Create([Text.Encoding]::UTF8.GetString($b))
 & $s $p.request | ConvertTo-Json -Depth 10 -Compress
 exit 0
} catch {Write-Error $_.Exception.Message -ErrorAction Continue;exit 1}
'@
    $bootstrap = $bootstrap.Replace('CODE_DIGEST', $digest)
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($bootstrap))
    $command = 'powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -OutputFormat Text -EncodedCommand ' + $encoded
    if ($command.Length -gt 7000) { throw 'Fixed SSH bootstrap exceeds the Windows command-line budget.' }
    $payload = [ordered]@{ protocol = 'grindless.pc-workflow/1'; code = [Convert]::ToBase64String($bytes)
        request = @{ protocol = 'grindless.pc-workflow/1'; action = $Action; revision = $Revision
            packZip = $PackZip; config = $Config } }
    return [pscustomobject]@{ Command = $command; Json = (ConvertTo-Json $payload -Depth 10 -Compress) }
}

function Invoke-PcWorkflowTransport([string]$Target, $Packet) {
    # PS 5.1's default native pipe encoding is ASCII. Scope and restore UTF-8 so
    # non-ASCII private paths survive without changing any user/global setting.
    $previousEncoding = $OutputEncoding
    try {
        $OutputEncoding = New-Object Text.UTF8Encoding($false)
        $Packet.Json | & ssh -T -o BatchMode=yes -o StrictHostKeyChecking=yes -o ConnectTimeout=15 `
            -o ConnectionAttempts=1 $Target $Packet.Command | ForEach-Object { Write-Host $_ }
        return [int]$LASTEXITCODE
    } finally { $OutputEncoding = $previousEncoding }
}

function Invoke-PcWorkflow {
    [CmdletBinding()]
    param(
        [ValidateSet('Inspect', 'Sync', 'Build', 'BuildAndDeploy', 'Deploy')][string]$Action = 'Inspect',
        [string]$Revision, [string]$PackZip,
        [string]$ConfigFile = (Join-Path $HOME '.config/grindless/pc.json')
    )
    Assert-PcRevision $Revision
    $config = Read-PcWorkflowConfig $ConfigFile
    $packet = New-PcWorkflowPacket $config $Action $Revision $PackZip
    return Invoke-PcWorkflowTransport $config.sshTarget $packet
}

if ($MyInvocation.InvocationName -ne '.') {
    try { exit (Invoke-PcWorkflow @PSBoundParameters) }
    catch { Write-Error $_.Exception.Message -ErrorAction Continue; exit 1 }
}
