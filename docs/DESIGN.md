# Grindless — Design Document

> **Grindless** is a Factorio-inspired, futuristic automation mod whose single purpose is to
> delete the modpack early-game grind without deleting the *game*.

---

## 1. The problem we are solving

Open any large modpack. Hours 0–3 look identical every single time:

1. Punch trees, make a crafting table, make stone tools.
2. Strip-mine for iron and copper until you have "enough".
3. Hand-craft several hundred items, one click at a time.
4. *Only then* does the pack's actual content begin.

This is the least interesting part of the game, it is identical across every pack, and it is
the single biggest reason players bounce off a pack before seeing what makes it special.

Existing "fixes" all have failure modes:

| Existing approach | Why it fails |
| --- | --- |
| Creative-mode style item spawners | Removes all progression; pack becomes pointless |
| Veinminer / hammers | Speeds up the grind, but it is still a grind |
| Quest-book handouts | Pack-specific, needs manual authoring per pack |
| Mystical Agriculture-style seeds | Great, but hardcoded to a fixed material list and mid-game gated |

**The gap:** nothing gives you *automated, scalable, early* resource production that
automatically covers *every material in whatever pack you happen to be playing*.

That is the gap Grindless fills.

---

## 2. Design pillars

1. **Anti-grind, not anti-game.** Grindless removes repetition, not progression. Every
   capability is earned through a research tree, and every machine needs power, space and
   logistics. You are trading *tedium* for *engineering*.
2. **Factorio brain, Minecraft body.** Supply-area power poles, resource patches that
   deplete, ratio-driven processing chains, a research tree. Familiar to anyone who has
   played Factorio, native to Minecraft.
3. **Universal by construction.** Grindless must never contain a hardcoded list of
   materials. Everything is discovered at runtime from the item/block tags that the loaded
   modpack actually provides. A pack with 4 mods and a pack with 400 both just work.
4. **Interop is not optional.** Our energy is a first-class citizen of the FE/RF ecosystem
   on day one, not via an "addon" nobody installs.
5. **Every platform, every version.** One shared codebase, thin platform layers.

---

## 3. The three core systems

### 3.1 The Flux Network — power, Factorio-style

Cable spaghetti is its own kind of grind. Grindless uses Factorio's electric-pole model.

* **Flux Pylon** (MK1/MK2/MK3) projects a cubic **supply area**. *Any* Grindless machine
  inside that area is powered — no wire to run, no block placement puzzle.
* Pylons within **wire reach** of each other auto-link, merging into one **Flux Network**
  that shares a single energy pool.
* **Flux Conduit** (a hand item, not a block) force-links two distant pylons: right-click
  pylon A, right-click pylon B. This is the "long-distance transmission line", and it is a
  deliberate, manual, engineered decision — exactly the Factorio feel.
* The network is server-side `SavedData`. Machines resolve their network by position via a
  spatial index, so there is no per-tick graph walk.

```
        ┌── supply area (MK1: 7×5×7) ──┐
        │   ▣ Extractor                │
   ╔════╪═══╗                          │
   ║ PYLON  ║ ◄── auto-link ──► ╔══════╧═╗
   ╚════╪═══╝   (≤ 12 blocks)   ║ PYLON  ║
        │   ▣ Pulverizer        ╚════════╝
        └──────────────────────────────┘
                 one shared FU pool
```

### 3.2 Resource Genesis — materials, Factorio-style

The anti-grind core. You stop strip-mining; you start *surveying and developing*.

* Every chunk deterministically owns a **Resource Vein**: a material, a richness, and a
  reserve. Derived from the world seed + chunk coords, so it is stable, multiplayer-safe and
  needs no extra world storage until it is actually mined.
* **The vein material pool is built at runtime from the tags present in the loaded pack.**
  On a pack with Mekanism + Thermal + Create + AE2 you will find osmium, lead, zinc, certus
  quartz veins. On vanilla-only you will find iron, copper, gold, redstone. No config, no
  per-pack patching. *This is the promise of the mod.*
* **Prospector's Scanner** (hand item) surveys the current chunk and its neighbours.
* **Terrestrial Extractor** sits anywhere in a chunk, drinks FU, and produces that chunk's
  material in *raw* form over time. Rate scales with richness and machine tier.
* Veins **deplete toward a floor** rather than to zero — like Factorio's infinite ore, a
  mined-out vein still trickles. This pushes you to *expand outward*, which is the
  interesting decision, instead of *dig downward*, which is the boring one.
* **Deep Core Drill** (late tier) ignores chunk veins entirely and pulls from a weighted
  planetary pool: slow, infinite, power-hungry.

### 3.3 Matter Replication — everything that is not an ore

Ores are only half the grind. The rest is "I need 64 of this one weird component".

* **Pattern Scanner** consumes N copies of any item and stores a **Pattern**.
* **Deconstructor** dissolves unwanted items into **Matter**, a generic resource.
* **Replicator** spends FU + Matter to rebuild a scanned Pattern.
* Cost is **derived automatically** by walking the item's recipe graph to estimate
  complexity, so a Nether Star is not the same price as a stick. This closes the exploit
  that sinks most "copy any item" mods.
* Datapack- and config-driven blacklist for pack authors.

---

## 4. Energy model: FU (Flux Units)

Grindless has its own unit, **FU**, but it is deliberately *not* a walled garden.

| Unit | Default ratio | Notes |
| --- | --- | --- |
| **FU ↔ FE/RF** | **1 : 1** | Lossless. Chosen so Grindless drops into any pack with zero friction. Configurable. |
| **FU ↔ EU** | **4 : 1** | Matches the long-standing IC2 convention (1 EU = 4 RF). Configurable. |

**Why 1:1 and not an "exchange rate"?** Because a non-unity ratio forces rounding on every
transfer, which silently destroys or duplicates energy over millions of ticks and makes
Grindless a bug magnet in big packs. Interop correctness beats flavour.

**Then what makes FU special?** FU carries semantics that raw FE does not:

1. **Voltage tiers** — `LV → MV → HV → EV → IV`. A machine has a tier; the network has a
   tier. Under-volting runs a machine slowly; over-volting wastes the surplus (configurable
   to instead damage the machine, for packs that want the GregTech bite).
2. **Networked, not adjacency-based** — FU moves through the pylon grid, not block-to-block.
3. **Grid quality** — a network under sustained deficit browns out, reducing every machine's
   speed proportionally rather than randomly stalling one of them.

Foreign FE entering the network is admitted at the network's base tier. Foreign machines see
a perfectly ordinary `IEnergyStorage` (Forge) / `EnergyStorage` (Fabric, Team Reborn Energy).

See [`ENERGY.md`](ENERGY.md) for the full interop matrix.

---

## 5. Processing chains

All recipes are **generated at runtime from tags**, so they cover every material in the pack.

```
   Chunk Vein
       │  Terrestrial Extractor (FU)
       ▼
   Raw Material  ──── Arc Furnace (FU) ────────────────►  Ingot   (×1)
       │
       │  Pulverizer (FU)
       ▼
     Dust  ──────── Arc Furnace (FU) ───────────────────►  Ingot   (×2)
       │
       │  Chemical Washer (FU + water)
       ▼
   Purified Dust ── Arc Furnace (FU) ──────────────────►  Ingot   (×2)  + byproduct
```

Yields are intentionally in the same range as Thermal/Mekanism so Grindless does not
invalidate a pack's existing ore doubling — it *feeds* it.

---

## 6. Progression: the Research Terminal

Factorio's tech tree, adapted. You insert **Data Cores** and FU; research unlocks blueprints.
This is what keeps the mod from being a creative-mode cheat.

| Tier | Unlock window | Contents |
| --- | --- | --- |
| **T0 · Bootstrap** | 0–10 min | Hand Crank Dynamo, Crude Extractor (stone/gravel/sand/coal tier only) |
| **T1 · Voltaic** | 10–40 min | Thermal Generator, Flux Pylon MK1, Terrestrial Extractor, Pulverizer, Arc Furnace |
| **T2 · Industrial** | 1–3 h | Pylon MK2, Chemical Washer, Assembler, Solar/Steam, Pattern Scanner |
| **T3 · Quantum** | late | Pylon MK3, Deep Core Drill, Replicator, Fusion-class generation |

The T0 tier is the crucial one: it is reachable with **cobblestone, wood and two iron**, in
the first ten minutes, and it immediately starts producing. That is the moment the grind dies.

---

## 7. Compatibility strategy

* **Tags over item IDs, always.** Forge (`forge:ores/*`, `forge:ingots/*`,
  `forge:raw_materials/*`, `forge:dusts/*`, …) and Fabric conventions are both consumed, and
  a unified view is exposed to common code.
* **Runtime discovery.** The material registry is rebuilt on every datapack reload, so
  adding a mod mid-pack is picked up without touching Grindless.
* **Graceful degradation.** A material that has an ore but no dust simply skips the
  pulverizing step instead of producing an unobtainable item.
* **No stepping on toes.** Grindless registers *its own* items only for materials that no
  other mod provides.

---

## 8. Scope of the current milestone

See [`ROADMAP.md`](ROADMAP.md) for what is implemented today versus planned.
