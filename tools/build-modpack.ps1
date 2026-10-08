# Builds the reproducible client-pack archive from the tracked CurseForge manifest and local jar.
# Usage: powershell -ExecutionPolicy Bypass -File .\tools\build-modpack.ps1 [-Root .]
param([string]$Root = (Split-Path -Parent $PSScriptRoot))

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path -LiteralPath $Root).Path
$propertiesPath = Join-Path $Root 'gradle.properties'
$manifestPath = Join-Path $Root 'pack\curseforge\manifest.json'

function Read-Property([string]$Name) {
    $line = Select-String -LiteralPath $propertiesPath -Pattern "^$([regex]::Escape($Name))=(.+)$" |
        Select-Object -First 1
    if (-not $line) { throw "Missing $Name in gradle.properties." }
    return $line.Matches[0].Groups[1].Value.Trim()
}

$modVersion = Read-Property 'mod_version'
$packVersion = Read-Property 'pack_version'
$archiveName = Read-Property 'archives_base_name'
$manifest = Get-Content -Raw -LiteralPath $manifestPath | ConvertFrom-Json
if ($manifest.version -ne $packVersion) {
    throw "Pack manifest version '$($manifest.version)' differs from gradle.properties '$packVersion'."
}

Push-Location $Root
try {
    & .\gradlew.bat :forge:remapJar
    if ($LASTEXITCODE -ne 0) { throw 'Forge jar build failed.' }

    $jar = Join-Path $Root "forge\build\libs\$archiveName-$modVersion-forge.jar"
    if (-not (Test-Path -LiteralPath $jar)) { throw "Expected remapped jar was not produced: $jar" }

    $stage = Join-Path ([System.IO.Path]::GetTempPath()) ("grindless-pack-" + [guid]::NewGuid())
    $outputDirectory = Join-Path $Root 'dist'
    $output = Join-Path $outputDirectory "Grindless-T0-$packVersion.zip"
    try {
        New-Item -ItemType Directory -Force -Path (Join-Path $stage 'overrides\mods') | Out-Null
        Copy-Item -LiteralPath $manifestPath -Destination (Join-Path $stage 'manifest.json')
        Copy-Item -LiteralPath $jar -Destination (Join-Path $stage 'overrides\mods')
        New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
        if (Test-Path -LiteralPath $output) { Remove-Item -LiteralPath $output -Force }
        Compress-Archive -Path (Join-Path $stage '*') -DestinationPath $output -CompressionLevel Optimal
        if (-not (Test-Path -LiteralPath $output)) { throw 'Archive creation produced no output.' }
        Write-Output "Built $output"
    } finally {
        Remove-Item -LiteralPath $stage -Recurse -Force -ErrorAction SilentlyContinue
    }
} finally {
    Pop-Location
}
