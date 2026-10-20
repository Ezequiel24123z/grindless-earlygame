# Function command feedback is silent; these say commands are the retained console evidence.
say EXOSUIT-FAIL-CHUNK-OR-ENTITY-TIMEOUT
say EXOSUIT-DIAG-SOURCE-first-exosuit-pack-DIM-minecraft:overworld-CHUNK-0,0-POS-8,100,8
say EXOSUIT-DIAG-BUDGET-160-POLLS-5t-INTERVAL-RUNNER-DEFAULT-120s
execute if score phase exosuit_check matches 0 run say EXOSUIT-DIAG-PHASE-CHUNK-WAIT
execute if score phase exosuit_check matches 1 run say EXOSUIT-DIAG-PHASE-ENTITY-WAIT
execute unless score phase exosuit_check matches 0..1 run say EXOSUIT-DIAG-PHASE-UNEXPECTED
execute if score tries exosuit_check matches 160 run say EXOSUIT-DIAG-COUNTER-160
execute unless score tries exosuit_check matches 160 run say EXOSUIT-DIAG-COUNTER-NOT-160
execute if loaded 8 100 8 run say EXOSUIT-DIAG-CHUNK-LOADED
execute unless loaded 8 100 8 run say EXOSUIT-DIAG-CHUNK-NOT-LOADED
execute store success score chunk_requested exosuit_check run forceload query 0 0
execute if score chunk_requested exosuit_check matches 1 run say EXOSUIT-DIAG-CHUNK-REQUESTED
execute unless score chunk_requested exosuit_check matches 1 run say EXOSUIT-DIAG-CHUNK-NOT-REQUESTED
execute if score helmet_spawn exosuit_check matches -1 run say EXOSUIT-DIAG-SUMMON-HELMET-NOT-ATTEMPTED
execute if score helmet_spawn exosuit_check matches 0 run say EXOSUIT-DIAG-SUMMON-HELMET-REJECTED
execute if score helmet_spawn exosuit_check matches 1 run say EXOSUIT-DIAG-SUMMON-HELMET-ACCEPTED
execute unless score helmet_spawn exosuit_check matches -1..1 run say EXOSUIT-DIAG-SUMMON-HELMET-UNKNOWN
execute if entity @e[type=minecraft:item,tag=grindless_exosuit_smoke,nbt={Item:{id:"grindless:flux_exosuit_helmet",Count:1b}}] run say EXOSUIT-DIAG-ENTITY-HELMET-OBSERVED
execute unless entity @e[type=minecraft:item,tag=grindless_exosuit_smoke,nbt={Item:{id:"grindless:flux_exosuit_helmet",Count:1b}}] run say EXOSUIT-DIAG-ENTITY-HELMET-NOT-OBSERVED
execute if score chest_spawn exosuit_check matches -1 run say EXOSUIT-DIAG-SUMMON-CHEST-NOT-ATTEMPTED
execute if score chest_spawn exosuit_check matches 0 run say EXOSUIT-DIAG-SUMMON-CHEST-REJECTED
execute if score chest_spawn exosuit_check matches 1 run say EXOSUIT-DIAG-SUMMON-CHEST-ACCEPTED
execute unless score chest_spawn exosuit_check matches -1..1 run say EXOSUIT-DIAG-SUMMON-CHEST-UNKNOWN
execute if entity @e[type=minecraft:item,tag=grindless_exosuit_smoke,nbt={Item:{id:"grindless:flux_exosuit_chestplate",Count:1b}}] run say EXOSUIT-DIAG-ENTITY-CHEST-OBSERVED
execute unless entity @e[type=minecraft:item,tag=grindless_exosuit_smoke,nbt={Item:{id:"grindless:flux_exosuit_chestplate",Count:1b}}] run say EXOSUIT-DIAG-ENTITY-CHEST-NOT-OBSERVED
execute if score tap_spawn exosuit_check matches -1 run say EXOSUIT-DIAG-SUMMON-TAP-NOT-ATTEMPTED
execute if score tap_spawn exosuit_check matches 0 run say EXOSUIT-DIAG-SUMMON-TAP-REJECTED
execute if score tap_spawn exosuit_check matches 1 run say EXOSUIT-DIAG-SUMMON-TAP-ACCEPTED
execute unless score tap_spawn exosuit_check matches -1..1 run say EXOSUIT-DIAG-SUMMON-TAP-UNKNOWN
execute if entity @e[type=minecraft:item,tag=grindless_exosuit_smoke,nbt={Item:{id:"grindless:network_tap",Count:1b}}] run say EXOSUIT-DIAG-ENTITY-TAP-OBSERVED
execute unless entity @e[type=minecraft:item,tag=grindless_exosuit_smoke,nbt={Item:{id:"grindless:network_tap",Count:1b}}] run say EXOSUIT-DIAG-ENTITY-TAP-NOT-OBSERVED
execute if score legs_spawn exosuit_check matches -1 run say EXOSUIT-DIAG-SUMMON-LEGS-NOT-ATTEMPTED
execute if score legs_spawn exosuit_check matches 0 run say EXOSUIT-DIAG-SUMMON-LEGS-REJECTED
execute if score legs_spawn exosuit_check matches 1 run say EXOSUIT-DIAG-SUMMON-LEGS-ACCEPTED
execute unless score legs_spawn exosuit_check matches -1..1 run say EXOSUIT-DIAG-SUMMON-LEGS-UNKNOWN
execute if entity @e[type=minecraft:item,tag=grindless_exosuit_smoke,nbt={Item:{id:"grindless:exoskeleton_legs",Count:1b}}] run say EXOSUIT-DIAG-ENTITY-LEGS-OBSERVED
execute unless entity @e[type=minecraft:item,tag=grindless_exosuit_smoke,nbt={Item:{id:"grindless:exoskeleton_legs",Count:1b}}] run say EXOSUIT-DIAG-ENTITY-LEGS-NOT-OBSERVED
scoreboard players set phase exosuit_check -1
function grindless_exosuit:cleanup
