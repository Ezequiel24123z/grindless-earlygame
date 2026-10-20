# Generated assets

Script-produced textures and models are written by `tools/assetgen` through
`tools/generate-assets.ps1`. Change their generator rather than editing its output.

Generation is deterministic: the same generator always produces byte-identical
output, so regenerating never shows up as a spurious diff.

No Minecraft assets are redistributed. The approved `sounds/t0_grinding.ogg` is
an unchanged external game-audio import, not synthetic generator output.
Keep `AUDIO-NOTICE.txt` with it; the repository's `THIRD_PARTY_NOTICES.md` records
its exact provenance and distribution limits (ADR-0148).

Generation does not write imported Ogg files, `sounds.json`, `AUDIO-NOTICE.txt`
or localization. The paired legacy audio converters allow only the five synthetic
sound names, so an extra WAV cannot overwrite the approved Quern import.

The Multitool PNG is copied byte-for-byte from `tools/visual-assets/approved/`
after native JDK hash, PNG, dimensions and hard-alpha validation. Its retained
source, generation and transform provenance is separate from normal generation.
No ComfyUI, Python, model or network dependency is needed to preserve it.

What is **not** generated, and is therefore missing rather than merely plain, is
listed in ADR-0048: hero item sprites, complex models, entity animation and music.
A future session should treat those as known gaps with no owner, not as oversights.

Current generated output: 1520 files (imports excluded).
