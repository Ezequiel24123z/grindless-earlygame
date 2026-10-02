# Regenerates every script-produced asset into common/src/main/resources/assets/grindless.
#
#     powershell -ExecutionPolicy Bypass -File .\tools\generate-assets.ps1 -Root .
#
# Needs nothing but the JDK the project already requires: javax.imageio writes the PNGs and
# javax.sound.sampled writes the WAVs, with no third-party library and no Python or Node (neither
# of which is present on the reference machine). See ADR-0048.
#
# Output is deterministic, so running this twice produces byte-identical files and a regeneration
# never appears as a spurious diff. If `git status` is dirty after a run, something really changed.
#
# This covers the form x material matrix and the machine casings. It does NOT produce hero item
# sprites, complex models, entity animation or music; ADR-0048 lists those as known gaps.

param([Parameter(Mandatory = $true)][string]$Root)

$ErrorActionPreference = 'Stop'

$source = Join-Path $Root 'tools\assetgen'
if (-not (Test-Path -LiteralPath $source)) {
    Write-Output "No generator at $source."
    exit 1
}

$out = Join-Path ([System.IO.Path]::GetTempPath()) ("grindless-assetgen-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $out | Out-Null

try {
    $sources = @(Get-ChildItem -Path $source -Filter '*.java' -Recurse -File | ForEach-Object { $_.FullName })
    # SupplyCatalogue is shared with the mod so the list of materials is written once. It and
    # MaterialForm have no Minecraft imports, which is what lets the generator compile them alone.
    $material = Join-Path $Root 'common\src\main\java\io\github\ezequiel24123z\grindless\material'
    $sources += (Join-Path $material 'SupplyCatalogue.java')
    $sources += (Join-Path $material 'MaterialForm.java')
    & javac -nowarn -d $out $sources
    if ($LASTEXITCODE -ne 0) {
        Write-Output 'GENERATOR DID NOT COMPILE'
        exit 1
    }

    & java -cp $out io.github.ezequiel24123z.grindless.assetgen.GenerateAssets (Resolve-Path $Root)
    if ($LASTEXITCODE -ne 0) {
        Write-Output 'GENERATION FAILED'
        exit 1
    }
    exit 0
} finally {
    Remove-Item -Recurse -Force $out -ErrorAction SilentlyContinue
}
