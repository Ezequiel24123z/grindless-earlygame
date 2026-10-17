# T0 asset workflow

The [manifest](t0-asset-manifest.json) inventories three existing machine families and seven
standalone icons. Defaults are inherited by each entry; machine-specific fields override them.
It is preparatory data, not a replacement source of runtime IDs, state enums or recipes.

Both supported image-generation keys were absent when checked. No API request, candidate image,
new runtime texture, animation, model or audio was generated. Existing committed JDK-generated
placeholders remain the functional floor. Absence of credentials does not block code or validation.

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
the deterministic source for present placeholders and models. Extend it only after a candidate
is accepted rather than hand-editing generated output.

## QC boundary

`VerifyT0Preparation` checks the manifest against `BlockCatalogue`, actual PNG dimensions/alpha,
state model references, identical shared body references, and an actual idle/running pixel
difference. Existing `VerifyAssets` checks the wider model/texture/sound resource graph.

These checks do not certify artistic quality, future frame-mask continuity, every global orphan
asset or visual accessibility at all client scales. A human client pass must inspect silhouette,
contrast, state readability, accidental flicker and originality. No final-art approval is claimed.

There is no T0 multiblock to tile. Do not invent casings/ports just to fill this field. When a
real T3 process requires a structure, its own manifest must add orientations, seams, adjacent
block continuity, port identity and an assembled preview.

Any upstream asset import also needs component-specific clearance and simultaneous
[notices](../../THIRD_PARTY_NOTICES.md); a generator or a familiar licence badge is not clearance.
