# T0 asset workflow

The [manifest](t0-asset-manifest.json) inventories six T0 machine families and ten
standalone icons. Defaults are inherited by each entry; machine-specific fields override them.
It is an audit/production contract, not a replacement source of runtime IDs, state enums or recipes.

Both supported image-generation keys were absent when checked. No image API request or AI
candidate is claimed. The original JDK generator now produces the workshop's three silhouettes,
state faces and three distinct ceramic item shapes. The manifest check covers 46 distinct PNGs.
These are functional original assets, not final hero-art acceptance; unrelated existing art stays
unchanged. Absence of credentials does not block code, geometry or validation.

## Bounded candidate round

Resolve the manifest's component, silhouette, state and palette into its original prompt template.
Generate at most two alternatives for one logical component before review; do not generate every
animation frame independently. Record the declared generation metadata and output hashes in
ignored `build/assets/t0/candidates/`, keeping keys out of every file.

Treat generated images as suggestions. Compose the stable body, front panel/mechanism, indicator
and any future emissive/state layer separately. The current runtime uses static single-frame
textures; adding animation or emissive behavior needs its own accepted implementation, not just
a strip of PNG frames.

Normalize to the native 16-pixel cells with hard pixel edges. Build the declared 4x4 preview atlas
using nearest-neighbour scaling; unused cells remain transparent. The committed JDK generator is
the deterministic source for functional assets and models. Extend its original geometry and
layers rather than hand-editing generated output; an external AI candidate requires its own QC
before replacing that source.

## QC boundary

`VerifyT0Preparation` checks the manifest against `BlockCatalogue`, actual PNG dimensions/alpha,
state model references, identical shared body references, and an actual idle/running pixel
difference. Existing `VerifyAssets` checks the wider model/texture/sound resource graph.

These checks do not certify artistic quality, future frame-mask continuity, every global orphan
asset or visual accessibility at all client scales. A human client pass must inspect silhouette,
contrast, state readability, accidental flicker and originality. No final-art approval is claimed.

The Firing Chamber is a real T0 structure under ADR-0129, with an original controller and
seven referenced vanilla brick blocks. Its manifest records the 2x2x2 composition, four
orientations, controller interfaces and active door region. No Minecraft texture is copied into
the mod. The next assembled settling unit must also declare its geometry, continuity and preview;
neither unit creates the future parametric T3 kernel.

Any upstream asset import also needs component-specific clearance and simultaneous
[notices](../../THIRD_PARTY_NOTICES.md); a generator or a familiar licence badge is not clearance.
