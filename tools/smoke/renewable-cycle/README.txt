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

After that window, an empty chest collects 12 real biomass. Roofing the culture
suspends it through its actual environmental requirement. An observed-empty first
hopper is removed and a chest collects at least six aggregate and six clay from
the collector; removing its geological bed then suspends it. At least 22 produced
biomass remain in the original Dynamo before downstream work starts.

Additional command-built, non-consumed equipment consists of an initially empty
Mk1 pylon, Quern, Settling Trough with channel/screen, Clay Mixer, four seven-brick
Firing Chamber assemblies, Assembly Bench, Research Terminal, empty chests/hoppers,
two Hand Pumps, two more placed water blocks, a conduit and a tank. All new energy
and fluid buffers start empty. There is no additional fuel or energy seed.

Paired fixture hand-loads copy an observed produced stack to a machine INPUT and
debit the original in the same function, with no game tick between the operations.
They do not write any claimed machine output, set Progress, or substitute a product.
These explicit test-controlled loads do not claim autonomous downstream item routing.
The real machines, live recipes/tags, natural pumps, native power network and native
output export must make all products:
  two dry aggregate batches -> eight cobblestone + four gravel;
  three of those gravel -> three sand;
  two wet aggregate batches -> two raw iron + two raw copper + four redstone;
  12 produced biomass -> six charcoal;
  two clay + one sand + naturally pumped water -> three unfired ceramic;
  that ceramic + one charcoal -> two insulators + one grog;
  four clay + one charcoal -> four bricks;
  two sand + two charcoal -> two glass;
  two raw copper + two charcoal -> two copper ingots + two slag;
  one insulator + one redstone -> one Data Core in the real Bench;
  that actual Data Core -> calibrated core in the real Research Terminal.
The wet route must be observed with at least 1,000 mB gross water while its first
batch still has both aggregate inputs. The return tank is measured separately;
ongoing pumping/reuse means its amount is not assumed to be exactly 1,500 mB.
All transitions poll actual inventories/work state, with bounded failure markers,
rather than declaring a timed delay or a RUNNING visual to be a production pass.

The final simultaneous stock gate requires glass x2, copper x2, redstone x3
(one spare beyond the hand recipe), brick x4 and a hopper-recovered calibrated core.
Keep three acceptance boundaries separate: machine-produced materials and real core
calibration are native smoke evidence; the authored hand recipe/quantities are
existing-check evidence; physical crafting-table interaction and recovery of five
Relay Matrices remain manual acceptance. No crafting hook is added, the Bench never
crafts a Matrix, and ADR-0137's physical hand-craft requirement is not removed.
Do not report a complete native Matrix exit, no-command survival, night-independent
supply, a complete construction reserve, retained-world compatibility, crash recovery,
client/GUI interaction or Tier acceptance.
