# Function feedback is silent; retain explicit say emitters for the actual scores and NBT predicates.
say HARNESS-FAIL-CHUNK-OR-ENTITY-TIMEOUT
say HARNESS-DIAG-SOURCE-first-harness-pack-DIM-minecraft:overworld-CHUNK-0,0-POS-8,100,8
say HARNESS-DIAG-BUDGET-160-POLLS-5t-INTERVAL-RUNNER-DEFAULT-120s
execute if score phase harness_check matches 0 run say HARNESS-DIAG-PHASE-CHUNK-WAIT
execute if score phase harness_check matches 1 run say HARNESS-DIAG-PHASE-ENTITY-WAIT
execute unless score phase harness_check matches 0..1 run say HARNESS-DIAG-PHASE-UNEXPECTED
execute if score tries harness_check matches 160 run say HARNESS-DIAG-COUNTER-160
execute unless score tries harness_check matches 160 run say HARNESS-DIAG-COUNTER-NOT-160
execute if loaded 8 100 8 run say HARNESS-DIAG-CHUNK-LOADED
execute unless loaded 8 100 8 run say HARNESS-DIAG-CHUNK-NOT-LOADED
execute store success score chunk_requested harness_check run forceload query 0 0
execute if score chunk_requested harness_check matches 1 run say HARNESS-DIAG-CHUNK-REQUESTED
execute unless score chunk_requested harness_check matches 1 run say HARNESS-DIAG-CHUNK-NOT-REQUESTED
execute if score helmet_spawn harness_check matches -1 run say HARNESS-DIAG-SUMMON-HELMET-NOT-ATTEMPTED
execute if score helmet_spawn harness_check matches 0 run say HARNESS-DIAG-SUMMON-HELMET-REJECTED
execute if score helmet_spawn harness_check matches 1 run say HARNESS-DIAG-SUMMON-HELMET-ACCEPTED
execute unless score helmet_spawn harness_check matches -1..1 run say HARNESS-DIAG-SUMMON-HELMET-UNKNOWN
execute if entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:voltaic_helmet",Count:1b}}] run say HARNESS-DIAG-ENTITY-HELMET-OBSERVED
execute unless entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:voltaic_helmet",Count:1b}}] run say HARNESS-DIAG-ENTITY-HELMET-NOT-OBSERVED
execute if score chest_spawn harness_check matches -1 run say HARNESS-DIAG-SUMMON-CHEST-NOT-ATTEMPTED
execute if score chest_spawn harness_check matches 0 run say HARNESS-DIAG-SUMMON-CHEST-REJECTED
execute if score chest_spawn harness_check matches 1 run say HARNESS-DIAG-SUMMON-CHEST-ACCEPTED
execute unless score chest_spawn harness_check matches -1..1 run say HARNESS-DIAG-SUMMON-CHEST-UNKNOWN
execute if entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:voltaic_chestplate",Count:1b}}] run say HARNESS-DIAG-ENTITY-CHEST-OBSERVED
execute unless entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:voltaic_chestplate",Count:1b}}] run say HARNESS-DIAG-ENTITY-CHEST-NOT-OBSERVED
execute if score legs_spawn harness_check matches -1 run say HARNESS-DIAG-SUMMON-LEGS-NOT-ATTEMPTED
execute if score legs_spawn harness_check matches 0 run say HARNESS-DIAG-SUMMON-LEGS-REJECTED
execute if score legs_spawn harness_check matches 1 run say HARNESS-DIAG-SUMMON-LEGS-ACCEPTED
execute unless score legs_spawn harness_check matches -1..1 run say HARNESS-DIAG-SUMMON-LEGS-UNKNOWN
execute if entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:voltaic_leggings",Count:1b}}] run say HARNESS-DIAG-ENTITY-LEGS-OBSERVED
execute unless entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:voltaic_leggings",Count:1b}}] run say HARNESS-DIAG-ENTITY-LEGS-NOT-OBSERVED
execute if score boots_spawn harness_check matches -1 run say HARNESS-DIAG-SUMMON-BOOTS-NOT-ATTEMPTED
execute if score boots_spawn harness_check matches 0 run say HARNESS-DIAG-SUMMON-BOOTS-REJECTED
execute if score boots_spawn harness_check matches 1 run say HARNESS-DIAG-SUMMON-BOOTS-ACCEPTED
execute unless score boots_spawn harness_check matches -1..1 run say HARNESS-DIAG-SUMMON-BOOTS-UNKNOWN
execute if entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:voltaic_boots",Count:1b}}] run say HARNESS-DIAG-ENTITY-BOOTS-OBSERVED
execute unless entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:voltaic_boots",Count:1b}}] run say HARNESS-DIAG-ENTITY-BOOTS-NOT-OBSERVED
execute if score cell_spawn harness_check matches -1 run say HARNESS-DIAG-SUMMON-CELL-NOT-ATTEMPTED
execute if score cell_spawn harness_check matches 0 run say HARNESS-DIAG-SUMMON-CELL-REJECTED
execute if score cell_spawn harness_check matches 1 run say HARNESS-DIAG-SUMMON-CELL-ACCEPTED
execute unless score cell_spawn harness_check matches -1..1 run say HARNESS-DIAG-SUMMON-CELL-UNKNOWN
execute if entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:flux_cell",Count:1b}}] run say HARNESS-DIAG-ENTITY-CELL-OBSERVED
execute unless entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:flux_cell",Count:1b}}] run say HARNESS-DIAG-ENTITY-CELL-NOT-OBSERVED
scoreboard players set phase harness_check -1
function grindless_harness:cleanup
