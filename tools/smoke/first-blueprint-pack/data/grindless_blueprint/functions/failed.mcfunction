# Function command feedback is silent; these say commands are the retained console evidence.
say BLUEPRINT-FAIL-CHUNK-OR-ENTITY-TIMEOUT
say BLUEPRINT-DIAG-SOURCE-first-blueprint-pack-DIM-minecraft:overworld-CHUNK-0,0-POS-8,100,8
say BLUEPRINT-DIAG-BUDGET-160-POLLS-5t-INTERVAL-RUNNER-DEFAULT-120s
execute if score phase blueprint_check matches 0 run say BLUEPRINT-DIAG-PHASE-CHUNK-WAIT
execute if score phase blueprint_check matches 1 run say BLUEPRINT-DIAG-PHASE-ENTITY-WAIT
execute unless score phase blueprint_check matches 0..1 run say BLUEPRINT-DIAG-PHASE-UNEXPECTED
execute if score tries blueprint_check matches 160 run say BLUEPRINT-DIAG-COUNTER-160
execute unless score tries blueprint_check matches 160 run say BLUEPRINT-DIAG-COUNTER-NOT-160
execute if loaded 8 100 8 run say BLUEPRINT-DIAG-CHUNK-LOADED
execute unless loaded 8 100 8 run say BLUEPRINT-DIAG-CHUNK-NOT-LOADED
execute store success score chunk_requested blueprint_check run forceload query 0 0
execute if score chunk_requested blueprint_check matches 1 run say BLUEPRINT-DIAG-CHUNK-REQUESTED
execute unless score chunk_requested blueprint_check matches 1 run say BLUEPRINT-DIAG-CHUNK-NOT-REQUESTED
execute if score tool_spawn blueprint_check matches -1 run say BLUEPRINT-DIAG-SUMMON-TOOL-NOT-ATTEMPTED
execute if score tool_spawn blueprint_check matches 0 run say BLUEPRINT-DIAG-SUMMON-TOOL-REJECTED
execute if score tool_spawn blueprint_check matches 1 run say BLUEPRINT-DIAG-SUMMON-TOOL-ACCEPTED
execute unless score tool_spawn blueprint_check matches -1..1 run say BLUEPRINT-DIAG-SUMMON-TOOL-UNKNOWN
execute if entity @e[type=minecraft:item,tag=grindless_blueprint_smoke,nbt={Item:{id:"grindless:blueprint_tool",Count:1b}}] run say BLUEPRINT-DIAG-ENTITY-TOOL-OBSERVED
execute unless entity @e[type=minecraft:item,tag=grindless_blueprint_smoke,nbt={Item:{id:"grindless:blueprint_tool",Count:1b}}] run say BLUEPRINT-DIAG-ENTITY-TOOL-NOT-OBSERVED
scoreboard players set phase blueprint_check -1
function grindless_blueprint:cleanup
