<#
    Grindless - one-shot bootstrap for a Windows development environment.

    Run this ONCE from a normal PowerShell window (it is idempotent and safe to re-run):

        powershell -ExecutionPolicy Bypass -File .\SETUP.ps1

    By default it:
      1. Reports whether this checkout can hold the deep source paths a multiloader
         Minecraft mod needs (Windows MAX_PATH is 260 unless long paths are enabled).
      2. Creates the full source / resource directory tree.
      3. Locates a JDK 17 and Gradle 8.8 (no system-wide install required).
      4. Generates the committed Gradle wrapper so the repo is self-contained.

    Optional switches:
      -Commit        Stage, commit and push this worktree.
      -Message       The commit message to use with -Commit.
      -MakePrivate   Switch the GitHub repository to private (needs the gh CLI).
      -SkipWrapper   Skip Gradle wrapper generation.

    Tip: -Commit stages, commits and pushes this worktree. Do that before opening a
    new session on this branch — a new session checks the branch out into a fresh
    worktree, so anything left uncommitted here would not appear there.

    It is also the fallback when the in-app terminal stops working, which is the
    situation AGENTS.md points here for:

        powershell -ExecutionPolicy Bypass -File .\SETUP.ps1 -SkipWrapper `
            -Commit -Message "Subject line

        Body explaining why."

    It never deletes anything except its own listed scratch files, and never rewrites
    git history.
#>

[CmdletBinding()]
param(
    # Skip wrapper generation (useful if gradle/wrapper is already committed).
    [switch]$SkipWrapper,

    # Stage, commit and push the work in this worktree. Useful before opening a new
    # session on this branch: it is checked out into a fresh worktree, so anything
    # left uncommitted here would not appear there.
    [switch]$Commit,

    # The commit message for -Commit. A subject line, a blank line, then a body
    # explaining why. Required, because a generic message on a specific change is
    # worse than no message: it actively misleads whoever reads the history later.
    [string]$Message,

    # Also switch the GitHub repository to private (requires the gh CLI, authenticated).
    [switch]$MakePrivate
)

$ErrorActionPreference = 'Stop'

# PowerShell 7.4+ turns native-command stderr into a terminating error. Gradle and gh both
# write progress to stderr routinely, so opt out or the script aborts on healthy output.
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -Scope Global -ErrorAction SilentlyContinue) {
    $PSNativeCommandUseErrorActionPreference = $false
}

$root = $PSScriptRoot

function Write-Step { param([string]$Message) Write-Host "`n=== $Message" -ForegroundColor Cyan }
function Write-Ok   { param([string]$Message) Write-Host "  [ok]   $Message" -ForegroundColor Green }
function Write-Warn { param([string]$Message) Write-Host "  [warn] $Message" -ForegroundColor Yellow }
function Write-Bad  { param([string]$Message) Write-Host "  [fail] $Message" -ForegroundColor Red }

Write-Host "Grindless bootstrap" -ForegroundColor White
Write-Host "Repository: $root"

# ---------------------------------------------------------------------------
# 1. MAX_PATH sanity check
# ---------------------------------------------------------------------------
Write-Step "Checking Windows path length limits"

# The deepest path this project will ever create, used as the worst case.
$deepestRelative = 'common\src\main\java\io\github\ezequiel24123z\grindless\network\FluxNetworkSavedData.java'
$worstCase = (Join-Path $root $deepestRelative).Length
Write-Host "  Worst-case path length: $worstCase characters"

$longPathsEnabled = $false
try {
    $lp = Get-ItemProperty -Path 'HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem' -Name 'LongPathsEnabled' -ErrorAction Stop
    $longPathsEnabled = ($lp.LongPathsEnabled -eq 1)
} catch {
    $longPathsEnabled = $false
}

if ($longPathsEnabled) {
    Write-Ok "Win32 long paths are enabled; deep source trees are fine."
} elseif ($worstCase -lt 250) {
    Write-Ok "Under the 260 character limit with headroom."
} else {
    Write-Warn "This checkout sits $($root.Length) characters deep and long paths are DISABLED."
    Write-Warn "Java/Gradle builds can fail with mysterious 'file not found' errors."
    Write-Warn "Fix it with ONE of the following:"
    Write-Warn "  (a) Run this in an ADMIN PowerShell, then reboot:"
    Write-Warn "      New-ItemProperty -Path 'HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem' ``"
    Write-Warn "        -Name LongPathsEnabled -Value 1 -PropertyType DWORD -Force"
    Write-Warn "  (b) Or develop from a short path instead, e.g.:"
    Write-Warn "      git clone https://github.com/Ezequiel24123z/grindless-earlygame C:\mc\grindless"
}

# Git is fixable without elevation: core.longpaths makes Git for Windows use the Unicode
# path APIs. It is a per-repository setting, so a fresh clone needs it again. See ADR-0024.
if (-not $longPathsEnabled) {
    try {
        $currentLongPaths = (& git -C $root config --get core.longpaths) 2>$null
        if ($currentLongPaths -ne 'true') {
            & git -C $root config core.longpaths true
            Write-Ok "Set git core.longpaths=true so staging deep files works."
        } else {
            Write-Ok "git core.longpaths already enabled."
        }
    } catch {
        Write-Warn "Could not set git core.longpaths; 'git add' may fail on deep paths."
    }
}

# ---------------------------------------------------------------------------
# 2. Directory tree
# ---------------------------------------------------------------------------
Write-Step "Creating the source tree"

$modPackage = 'io\github\ezequiel24123z\grindless'

$directories = @(
    'docs',
    'tools',
    '.github\workflows',
    'gradle\wrapper'
)

# Shared, loader-agnostic code. This is where almost all of the mod lives.
$commonPackages = @(
    '',                 # Grindless.java, GrindlessConfig.java
    'api',              # public API surface other mods may depend on
    'api\energy',       # FluxStorage / FluxTier contracts
    'block',
    'block\entity',
    'block\multiblock',  # fission, fusion, accelerator, deep core drill
    'client',
    'client\render',
    'client\screen',
    'energy',           # FU implementation + RF/EU conversion
    'entity',           # Aberrations, drones, projectiles
    'item',
    'item\equipment',   # Flux Exosuit modules
    'item\tool',        # drill, multitool, blueprint, scanner, conduit
    'item\weapon',
    'logistics',        # belts, manipulators, splitters, lane data model
    'logistics\drone',  # logistics + construction drones, crates
    'logistics\signal', # signal cable, logic controller, arithmetic unit
    'material',         # runtime tag-driven material registry
    'menu',
    'network',          # Flux Network: pylons, links, saved data
    'orbital',          # launch pad, rockets, mass driver, station, satellites
    'orbital\colony',   # abstractly simulated remote colonies
    'orbital\planet',   # planet registry + space mod detection
    'orbital\telepresence',  # Proxy Frames and the terminal
    'recipe',
    'registry',
    'research',         # Research Terminal progression gating
    'resonance',        # pollution analogue + Aberration spawning
    'vein',             # deterministic per-chunk resource veins
    'util'
)
foreach ($pkg in $commonPackages) {
    $directories += (Join-Path (Join-Path 'common\src\main\java' $modPackage) $pkg).TrimEnd('\')
}

$directories += @(
    'common\src\main\resources',
    'common\src\main\resources\assets\grindless\lang',
    'common\src\main\resources\assets\grindless\textures\block',
    'common\src\main\resources\assets\grindless\textures\item',
    'common\src\main\resources\assets\grindless\textures\entity',
    'common\src\main\resources\assets\grindless\textures\gui',
    'common\src\main\resources\assets\grindless\models\block',
    'common\src\main\resources\assets\grindless\models\item',
    'common\src\main\resources\assets\grindless\blockstates',
    'common\src\main\resources\data\grindless\recipes',
    'common\src\main\resources\data\grindless\tags\blocks',
    'common\src\main\resources\data\grindless\tags\items',
    'common\src\main\resources\data\grindless\loot_tables\blocks',
    'common\src\main\resources\data\grindless\research',
    'common\src\main\resources\data\grindless\multiblock',
    'common\src\main\resources\data\grindless\planets',
    'common\src\main\resources\data\grindless\dimension',
    'common\src\main\resources\data\grindless\dimension_type'
)

# Forge platform, the only build target (ADR-0039). The same jar also loads on
# NeoForge 1.20.1-47.1.x, which is a soft-fork of Forge 47 and keeps the
# net.minecraftforge package names.
#
# Note the layout of the bridge packages: Architectury's @ExpectPlatform resolves
# `<pkg>.Foo` to `<pkg>.forge.FooImpl`, so a platform implementation lives in a
# `forge` package *beneath* the package it implements -- energy\forge, not
# forge\energy. Getting this backwards compiles fine and fails at runtime.
$directories += @(
    (Join-Path (Join-Path 'forge\src\main\java' $modPackage) 'forge'),
    (Join-Path (Join-Path 'forge\src\main\java' $modPackage) 'energy\forge'),
    (Join-Path (Join-Path 'forge\src\main\java' $modPackage) 'client\forge'),
    'forge\src\main\resources\META-INF'
)

# Scratch files the session could not delete without a working shell.
foreach ($stale in @('README.new.md')) {
    $stalePath = Join-Path $root $stale
    if (Test-Path -LiteralPath $stalePath) {
        Remove-Item -LiteralPath $stalePath -Force
        Write-Ok "Removed stale scratch file $stale"
    }
}

$created = 0
foreach ($dir in $directories) {
    $full = Join-Path $root $dir
    if (-not (Test-Path -LiteralPath $full)) {
        New-Item -ItemType Directory -Force -Path $full | Out-Null
        $created++
    }
}
Write-Ok "$created directories created, $($directories.Count) total."

# ---------------------------------------------------------------------------
# 3. Toolchain discovery
# ---------------------------------------------------------------------------
Write-Step "Locating JDK 17 and Gradle"

$toolsRoot = Join-Path $env:LOCALAPPDATA 'CopilotTools'

$javaHome = $null
if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    $javaHome = $env:JAVA_HOME
} else {
    $candidate = Get-ChildItem -Path (Join-Path $toolsRoot 'jdk') -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') } |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($candidate) { $javaHome = $candidate.FullName }
}

if ($javaHome) {
    $env:JAVA_HOME = $javaHome
    $version = & (Join-Path $javaHome 'bin\java.exe') -version 2>&1 | Select-Object -First 1
    Write-Ok "JAVA_HOME = $javaHome"
    Write-Host "         $version"
} else {
    Write-Bad "No JDK 17 found. Download a JDK 17 (Temurin) and set JAVA_HOME, then re-run."
    Write-Bad "Looked in: `$env:JAVA_HOME and $toolsRoot\jdk\*"
}

$gradleCmd = $null
$gradleOnPath = Get-Command gradle -ErrorAction SilentlyContinue
$localGradle = Get-ChildItem -Path (Join-Path $toolsRoot 'gradle') -Directory -ErrorAction SilentlyContinue |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\gradle.bat') } |
    Sort-Object Name -Descending | Select-Object -First 1

if ($localGradle) {
    $gradleCmd = Join-Path $localGradle.FullName 'bin\gradle.bat'
} elseif ($gradleOnPath) {
    $gradleCmd = $gradleOnPath.Source
}

if ($gradleCmd) {
    Write-Ok "Gradle = $gradleCmd"
} else {
    Write-Warn "No Gradle found; the wrapper cannot be generated automatically."
    Write-Warn "Download Gradle 8.8 (binary distribution) from https://gradle.org/releases/"
}

# ---------------------------------------------------------------------------
# 4. Gradle wrapper
# ---------------------------------------------------------------------------
if (-not $SkipWrapper) {
    Write-Step "Generating the Gradle wrapper"
    if (Test-Path -LiteralPath (Join-Path $root 'gradlew.bat')) {
        Write-Ok "Wrapper already present; skipping."
    } elseif ($gradleCmd -and $javaHome) {
        Push-Location $root
        try {
            & $gradleCmd wrapper --gradle-version 8.8 --distribution-type bin
            if ($LASTEXITCODE -eq 0) {
                Write-Ok "Wrapper generated. Commit gradle/ , gradlew and gradlew.bat."
            } else {
                Write-Bad "Gradle exited with code $LASTEXITCODE."
            }
        } finally {
            Pop-Location
        }
    } else {
        Write-Warn "Skipped: a working JDK 17 and Gradle are both required."
    }
}

# ---------------------------------------------------------------------------
# 5. Commit and push (opt-in)
# ---------------------------------------------------------------------------
if ($Commit) {
    Write-Step "Committing and pushing this worktree"
    Push-Location $root
    try {
        $branch = (& git rev-parse --abbrev-ref HEAD).Trim()
        Write-Host "  Branch: $branch"

        & git add -A
        $staged = & git diff --cached --name-only
        if (-not $staged) {
            Write-Ok "Nothing to commit; working tree already clean."
        } else {
            Write-Host "  Staged files:"
            $staged | ForEach-Object { Write-Host "    $_" }

            if (-not $Message) {
                Write-Bad 'No commit message. Re-run with -Message "Subject line<newline><newline>Why."'
                Write-Host '  A generic message on a specific change is worse than no message:'
                Write-Host '  it actively misleads whoever reads the history later.'
                return
            }

            # The co-author trailer is appended below, so callers never have to remember it.
            $message = @"
$Message

Co-authored-by: ******* App <223556219+Copilot@users.noreply.github.com>
"@
            & git commit -m $message
            if ($LASTEXITCODE -eq 0) {
                Write-Ok "Committed."
                # git has no stored credentials in this worktree, so a plain push fails. When
                # GH_TOKEN is in the environment, push with a transient auth header: -c applies
                # it to this one command, so no token is ever written to .git/config, to a
                # remote URL, or to any tracked file.
                if ($env:GH_TOKEN) {
                    $pair = "x-access-token:$($env:GH_TOKEN)"
                    $b64 = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
                    & git -c http.extraheader="AUTHORIZATION: basic $b64" push -u origin $branch
                } else {
                    Write-Host "  No GH_TOKEN in the environment; trying an unauthenticated push."
                    & git push -u origin $branch
                }
                if ($LASTEXITCODE -eq 0) {
                    Write-Ok "Pushed to origin/$branch."
                    Write-Ok "A new Copilot session on this branch will now see all the work."
                } else {
                    Write-Bad "Push failed with code $LASTEXITCODE. Push manually: git push -u origin $branch"
                }
            } else {
                Write-Bad "Commit failed with code $LASTEXITCODE."
            }
        }
    } finally {
        Pop-Location
    }
}

# ---------------------------------------------------------------------------
# 6. Repository visibility (opt-in)
# ---------------------------------------------------------------------------
if ($MakePrivate) {
    Write-Step "Switching the GitHub repository to private"
    $gh = Get-Command gh -ErrorAction SilentlyContinue
    if ($gh) {
        & gh repo edit Ezequiel24123z/grindless-earlygame --visibility private --accept-visibility-change-consequences
        if ($LASTEXITCODE -eq 0) {
            Write-Ok "Repository is now private."
        } else {
            Write-Bad "gh exited with code $LASTEXITCODE. Change it manually at:"
            Write-Bad "  https://github.com/Ezequiel24123z/grindless-earlygame/settings"
        }
    } else {
        Write-Warn "The gh CLI is not installed. Change visibility manually:"
        Write-Warn "  https://github.com/Ezequiel24123z/grindless-earlygame/settings"
        Write-Warn "  Danger Zone -> Change repository visibility -> Make private"
    }
}

# ---------------------------------------------------------------------------
Write-Step "Done"
Write-Host @"
Next steps:

  1. Tell the Copilot session that SETUP.ps1 finished, so it can write the source files.
  2. Once the sources exist, the first build downloads and decompiles Minecraft and
     will take several minutes:

       `$env:JAVA_HOME = "$javaHome"
       .\gradlew.bat :forge:build

  3. The finished jar lands in forge\build\libs\.
     It also loads on NeoForge 1.20.1-47.1.x unchanged.
"@ -ForegroundColor White
