# Generated assets

Script-produced textures and models are written by `tools/assetgen` through
`tools/generate-assets.ps1`. Change their generator rather than editing its output.

Generation is deterministic: the same generator always produces byte-identical
output, so regenerating never shows up as a spurious diff.

No Minecraft assets are redistributed. The imported `sounds/t0_grinding.ogg` and
`sounds/t0_wet_work.ogg`, together with `sounds/t0_water_flow.ogg` and
`sounds/t0_mineral_extraction.ogg`, are noticed external game audio, not synthetic
generator output.
The approved Quern A import remains byte-identical (ADR-0148); the Mixer working bed
uses its separately recorded technical normalization.
Keep `AUDIO-NOTICE.txt` with them; the repository's `THIRD_PARTY_NOTICES.md` records
their exact provenance and distribution limits.

Generation does not write imported Ogg files, `sounds.json`, `AUDIO-NOTICE.txt`
or localization. The five historical WAV writers produce development evidence only.
The paired legacy audio converters reject runtime synthesis, so an extra WAV
cannot overwrite an imported working bed or restore the retired generic palette.

The Multitool PNG is copied byte-for-byte from `tools/visual-assets/approved/`
after native JDK hash, PNG, dimensions and hard-alpha validation. Its retained
source, generation and transform provenance is separate from normal generation.
No ComfyUI, Python, model or network dependency is needed to preserve it.

What is **not** generated, and is therefore missing rather than merely plain, is
listed in ADR-0048: hero item sprites, complex models, entity animation and music.
A future session should treat those as known gaps with no owner, not as oversights.

Current generated output: 1520 files (imports excluded).
