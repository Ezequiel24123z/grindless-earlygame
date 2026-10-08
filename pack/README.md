# Grindless T0 — first playable-pack candidate

This directory defines a reproducible **client pack** for Minecraft 1.20.1 / Forge 47.4.23.
`tools/build-modpack.ps1` (or `.sh`) builds the local Grindless jar, adds it as an override, and
creates an importable CurseForge-format zip under `dist/`. Nothing under `dist/` is source control.

## Included integrations

| Mod | Purpose in this build | Progression policy |
| --- | --- | --- |
| JEI | Shows vanilla data recipes plus Grindless's generated machine graph and the Research Terminal calibration. | Available immediately; it reveals recipes, not permissions. |
| Jade | In-world machine and block information. | Available immediately. |
| Sophisticated Backpacks | Portable, upgradable inventory. | Available immediately. |
| FTB Essentials | `/home`, `/sethome`, `/back`, and practical single-player/server utilities. | Available immediately. |
| FTB Chunks + Teams + Library | Claims, map and team ownership. | Available immediately. |
| ModernFix, FerriteCore, Embeddium | Memory and client-performance baseline. | Available immediately; Embeddium is client-side. |

## Deliberately not in T0: Applied Energistics 2

AE2 is an intended **T4 Precision** integration, where storage cells, patterns and autocrafting
fit Grindless's first precision/electronics factory. Adding unmodified AE2 to this Bootstrap pack
would expose its native ores, machines and recipes before that frontier and would therefore make a
T0 playthrough non-authoritative. The future AE2 slice must ship its recipe replacement, material
bridges, energy behaviour and a complete survival proof in the same release. Until then this pack
does not claim AE2 compatibility or include the mod as a false gate.

## Install and test locally

1. Use Java 17 and run `powershell -ExecutionPolicy Bypass -File .\tools\build-modpack.ps1` at
   the repository root.
2. Import the resulting `dist/Grindless-T0-0.1.0-t0.1.zip` into the CurseForge launcher.
3. Start a new normal-survival world. The Field Guide and quest book identify the T0 route; JEI
   shows every authored crafting recipe, generated process and physical calibration cycle.

This is a first playable-pack **candidate**, not a T0 acceptance certificate. The remaining gate
is a recorded end-to-end no-command survival run from the documented two-iron entry through the
first recoverable four-Relay-Matrix batch.

## Dependency provenance

The manifest makes the launcher download each upstream file; it does not vendor their jars in this
repository. Exact CurseForge project/file identifiers are pinned in `curseforge/manifest.json`.
Their licences remain theirs: JEI (MIT), Jade (LGPL-3.0), Sophisticated Backpacks and Core (ARR),
FTB Essentials/Chunks/Teams/Library (ARR), ModernFix (LGPL-3.0), FerriteCore (MIT), and Embeddium
(LGPL-3.0). See the upstream project pages before redistributing a modified dependency.
