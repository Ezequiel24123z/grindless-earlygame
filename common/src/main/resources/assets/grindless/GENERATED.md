# Generated assets

Every file under this directory is produced by `tools/assetgen` and written by
`tools/generate-assets.ps1`. **Do not edit them by hand** — the next run overwrites
them. Change the generator instead.

Generation is deterministic: the same generator always produces byte-identical
output, so regenerating never shows up as a spurious diff.

Nothing here is copied from Minecraft or from any third-party mod, which is what
lets the README state its provenance without qualification.

What is **not** generated, and is therefore missing rather than merely plain, is
listed in ADR-0048: hero item sprites, complex models, entity animation and music.
A future session should treat those as known gaps with no owner, not as oversights.

Current output: 633 files.
