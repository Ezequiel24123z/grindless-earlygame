# Validates every relative link and anchor in the repository's markdown, and checks that the
# ADR index in docs/DECISIONS.md matches its actual sections.
#
# Run before committing documentation:
#     powershell -ExecutionPolicy Bypass -File .\tools\check-links.ps1 -Root .
#
# Exits non-zero when something does not resolve, so it can gate a commit or a CI job.
#
# Two things this gets right that a naive version does not, both of which produced real
# false results before they were fixed:
#
#   1. Files are read as UTF-8 explicitly. Headings in this project contain em dashes, and
#      reading them in the console's default codepage mangles the slug, which shows up as a
#      flood of phantom broken anchors.
#   2. The file list comes from `git ls-files`, not from walking the tree. Recursing the
#      working directory descends into build/ and .gradle/loom-cache, whose generated paths
#      exceed Windows MAX_PATH and abort the walk with an error that looks nothing like a
#      path-length problem (ADR-0024).

param([Parameter(Mandatory=$true)][string]$Root)

$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Get-Slug([string]$heading) {
    $t = $heading
    $t = [regex]::Replace($t, '`([^`]*)`', '$1')          # strip code ticks, keep content
    $t = [regex]::Replace($t, '\*\*([^*]*)\*\*', '$1')     # bold
    $t = [regex]::Replace($t, '\*([^*]*)\*', '$1')         # italic
    $t = [regex]::Replace($t, '\[([^\]]*)\]\([^)]*\)', '$1') # inline links -> text
    $t = $t.ToLowerInvariant()
    $t = [regex]::Replace($t, '[^a-z0-9 _-]', '')          # drop punctuation incl. em dashes
    $t = $t -replace ' ', '-'
    return $t
}

# Ask git for the file list rather than walking the tree. Recursing the working directory
# descends into build/ and .gradle/loom-cache, whose generated paths blow past MAX_PATH and
# abort the traversal with an error that looks nothing like a path-length problem. Git also
# gives exactly the right set: committed documentation, never build output.
Push-Location $Root
try {
    $tracked = @(git ls-files '*.md')
} finally {
    Pop-Location
}
if ($LASTEXITCODE -ne 0) { throw "git ls-files failed in $Root" }

$mdFiles = @($tracked | ForEach-Object { Get-Item -LiteralPath (Join-Path $Root $_) })

# Build anchor table per file
$anchors = @{}
$headingText = @{}
foreach ($f in $mdFiles) {
    $lines = [System.IO.File]::ReadAllLines($f.FullName, $utf8)
    $set = New-Object 'System.Collections.Generic.HashSet[string]'
    $titles = New-Object 'System.Collections.Generic.List[string]'
    $counts = @{}
    $inFence = $false
    foreach ($line in $lines) {
        if ($line -match '^\s*```') { $inFence = -not $inFence; continue }
        if ($inFence) { continue }
        if ($line -match '^(#{1,6})\s+(.*?)\s*$') {
            $raw = $Matches[2]
            $titles.Add($raw)
            $slug = Get-Slug $raw
            if ($counts.ContainsKey($slug)) {
                $counts[$slug]++
                $slug = "$slug-$($counts[$slug])"
            } else { $counts[$slug] = 0 }
            [void]$set.Add($slug)
        }
    }
    $anchors[$f.FullName] = $set
    $headingText[$f.FullName] = $titles
}

$problems = New-Object 'System.Collections.Generic.List[string]'
$linkCount = 0

foreach ($f in $mdFiles) {
    $text = [System.IO.File]::ReadAllText($f.FullName, $utf8)
    # blank out fenced code blocks so example text isn't parsed as links
    $text = [regex]::Replace($text, '(?s)```.*?```', { "`n" * ($args[0].Value -split "`n").Count })
    $rel = $f.FullName.Substring($Root.Length).TrimStart('\')

    foreach ($m in [regex]::Matches($text, '\[(?<t>[^\]\[]*)\]\((?<u>[^)\s]+)\)')) {
        $target = $m.Groups['u'].Value
        if ($target -match '^(https?:|mailto:|#!)') { continue }
        $linkCount++

        $path, $frag = $target -split '#', 2
        if ([string]::IsNullOrEmpty($path)) {
            $destFile = $f.FullName
        } else {
            $destFile = [System.IO.Path]::GetFullPath((Join-Path $f.DirectoryName $path))
            if (-not (Test-Path -LiteralPath $destFile)) {
                $problems.Add("MISSING FILE  $rel  ->  $target")
                continue
            }
        }

        if ($frag) {
            if (-not $anchors.ContainsKey($destFile)) {
                $problems.Add("NOT INDEXED   $rel  ->  $target")
                continue
            }
            if (-not $anchors[$destFile].Contains($frag)) {
                $problems.Add("BAD ANCHOR    $rel  ->  $target")
            }
        }
    }
}

# DECISIONS.md index consistency
$dec = Join-Path $Root 'docs\DECISIONS.md'
if (Test-Path -LiteralPath $dec) {
    $decLines = [System.IO.File]::ReadAllLines($dec, $utf8)
    $sections = @{}
    $index = @{}
    foreach ($line in $decLines) {
        if ($line -match '^##\s+(ADR-(\d{4}))\s') { $sections[$Matches[2]] = $Matches[1] }
        if ($line -match '^\|\s*\[(\d{4})\]\(#') { $index[$Matches[1]] = $true }
    }
    foreach ($k in $sections.Keys) {
        if (-not $index.ContainsKey($k)) { $problems.Add("INDEX MISSING ADR-$k has a section but no index row") }
    }
    foreach ($k in $index.Keys) {
        if (-not $sections.ContainsKey($k)) { $problems.Add("INDEX ORPHAN  ADR-$k is indexed but has no section") }
    }
    Write-Output "DECISIONS.md: $($sections.Count) sections, $($index.Count) index rows"
}

Write-Output "Checked $linkCount relative links across $($mdFiles.Count) markdown files."
if ($problems.Count -eq 0) {
    Write-Output "ALL LINKS AND ANCHORS RESOLVE"
} else {
    Write-Output "--- $($problems.Count) PROBLEM(S) ---"
    $problems | ForEach-Object { Write-Output $_ }
    exit 1
}
