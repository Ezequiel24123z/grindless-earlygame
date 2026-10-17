Bounded native renewable-cycle fixture (ADR-0131, ADR-0133, ADR-0137)

Run through the existing official production-jar runner:
  SMOKE_PORT=25627
  SMOKE_EXPECT_TIMEOUT=900
  GRINDLESS_ACCEPT_EULA=true
  powershell -ExecutionPolicy Bypass -File .\tools\run-smokes.ps1 -Root . -Only renewable-cycle

The renewable-cycle.timeout sidecar declares a 900-second scenario expectation budget.
The coordinator owns the paired runner support: explicit environment override, then this
sidecar, then the unchanged 120-second fallback. No runner/CI edit belongs to this fixture.

Finite setup is command-built equipment, hoppers, stone, two placed water blocks,
daylight/open sky, and one explicitly supplied coal in the Biomass Dynamo. Its
native 1,600-tick burn at 8 FU/t is a finite 12,800-FU startup allowance; every
energy buffer starts empty. No biomass, aggregate, clay or processed product is seeded.
No item/fluid/energy stock is replenished during the source observation window.
Geology, a hopper chain, culture, direct biomass fuel insertion, two unpowered
Hand Pumps and native neighboring energy delivery do the actual production.
There is no pylon/network during the measured source window.

The live authorities are SourceLogic, RenewableProcesses, FluxTier and ResearchLogic.
Fixture constants assert their current quantities; they are not gameplay authorities.
Each geological cycle yields one clay and one aggregate. All produced clay remains
countable in the collector/hoppers. Biological cycles equal geological cycles minus
all remaining aggregate. Four biomass per biological cycle are either in real
inventory or have ignited the Dynamo's real 200-tick, 8-FU/t fuel cycle.

At every 10-tick observation, require the exact integer equality:
  12800 + (4 * bio_cycles - biomass_stock) * 1600 - burn_remaining * 8
    = geo_cycles * 800 + bio_cycles * 640
      + geo_progress * 4 + bio_progress * 4 + all_three_energy_buffers
During startup burn_remaining refers to the one coal; after its complete burn it
refers to biomass. The steady baseline requires the actual native Duration=200
biomass ignition, no coal in fuel inventory and zero startup burn remaining.
Require at least four additional complete cycles of each source over at least
800 elapsed game ticks, at least one actually burned produced biomass, 32 retained
biomass and 15,000 FU of additional stored/fuel potential. This cannot pass by
displaying RUNNING, by spending only startup energy or by injecting final outputs.
Clay/aggregate totals remain below a stack in this bounded run; a missing or
ambiguous inventory selector cannot silently satisfy the exact balance.
The report/number/digit functions emit named five-digit decimal metrics through
ordinary say commands, because Minecraft suppresses data-get feedback inside functions.

This first checkpoint covers the source window only. Downstream ceramic, settling,
carbon, glass, copper/redstone and physical core calibration are the next fixture
stage. No crafting hook exists. The authored hand craft and its quantities belong
to existing checks; physical crafting-table interaction and recovery of five Relay
Matrices remain manual acceptance. Do not report a complete native Matrix exit,
no-command survival, night-independent supply, a complete construction reserve,
retained-world compatibility, crash recovery, client/GUI interaction or Tier acceptance.
