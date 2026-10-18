# T0 Sound Style Guide

**Status:** the owner approved the initial listening of **all three delivered
Quern preview WAVs**. This closes the initial calibration gate; no further
single-candidate choice or regeneration is required. Runtime, factory mixing,
the complete T0 sound pass and release acceptance remain unverified.

Authority: [hash-bound approval record](t0-sound-approval.json),
[executed design manifest](t0-sound-design.json) and
[ADR-0145](../DECISIONS.md#adr-0145--calibrate-a-small-shared-sound-palette-with-development-only-generation).
The executed manifest/contract/runner and historical receipts are immutable
evidence, not files to rewrite with retrospective approval flags.

## Approved reference palette

| Preview | Character | Role |
| --- | --- | --- |
| A / `quern-a` | Dry, close granular contact and stone-disc abrasion | Astra's default first Quern integration reference |
| B / `quern-b` | Restrained grinding with lower woody resonance | Equally owner-approved alternative/reference |
| C / `quern-c` | Quiet, damped abrasion with less bright crunch | Equally owner-approved alternative/reference |

**Astra selected A as the first implementation reference**, because it most
directly matches granular processing and has a clean boundary screen. This is
not an exclusive choice made by the owner. B/C are neither rejected nor automatic
extra shipped files. Woody resonance is an artistic contrast, not proof of a
timber frame. Neither the recipe's stick nor its F0 rating establishes a manual
or electric drive.

## Sound language

- Ground the sound in stone, granular material, wood-like resonance and plausible
  physical contact. T0 should feel like a restrained working shop, not endgame.
- Prefer short, steady, natural working textures with modest transients and low
  fatigue. The approved calibration is three seconds; duration alone is not a
  universal specification or a guarantee of seamless repetition.
- Avoid generic electronic/sci-fi hums, musical tones, voices, cinematic drama,
  exaggerated impacts, unnecessary beeps and constant alarms.
- Keep a machine silent when there is no useful physical sound. A logical state
  is not an asset request: no routine idle, missing-input, blocked, invalid-recipe,
  start/stop or per-machine error palette.

## Reuse and factory restraint

Use the minimum useful set of physical-family beds and shared interactions.
Stone grinding does not automatically fit a pump, wet process or firebox. Add a
family only for a demonstrated mechanism gap, not to fill a catalogue.

Start with one Quern family reference. Variants need a bounded, demonstrated
repetition/mix benefit under the same family identity; they are not a reason to
spawn per-state files or publish all three copies by default. Preserve existing
ordinary-error silence and the distinct, justified pylon shortage alert.

A working bed should remain comfortable with one and eight nearby machines.
Distance falloff, gain, independent voice ownership, stopping and concurrency
must be evaluated in the actual client. Do not conceal a mix problem with more
assets. A loopable file is **not** the current randomized one-shot caller and is
not, by itself, a managed runtime loop.

## Technical and approval boundaries

The approved files are mono, 44.1 kHz PCM16 previews produced by the committed
fixed -3 dB downmix. Preserve their exact hashes and source/provenance lineage.
Any later transform has a new derived hash; do not silently relabel it as the
exact file heard by the owner. Keep generation outside normal builds/runtime.

C's raw MP3 retains `loop_boundary_risk`: sample jump approximately **0.022812**
against the 0.02 screen. Its approved preview measures approximately **0.015411**
and has no boundary threshold flag. That lower measurement does not prove every
join, runtime behavior or factory mix. C remains owner-approved for the initial
preview listening; the historical warning is not erased and does not demand a
regeneration.

The derived Ogg files have technical QC, not separate recorded listening or
runtime acceptance. Asset import, actual game-incorporation rights and publication
remain Astra's later review; this guide authorizes no standalone public library.
Calibration 01 used its three authorized requests; that exhausted grant permits no
additional request. The owner's broader directive permits continued bounded work
after this artistic calibration. Any further generation needs a newly reviewed
purpose and budget, not reuse of the old grant or generation merely because credits remain.
