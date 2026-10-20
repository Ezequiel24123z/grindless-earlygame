say FLUIDS-FAIL-CHUNK-OR-TRANSFER-TIMEOUT
say FLUIDS-DIAG-SOURCE-first-fluids-pack-DIM-minecraft:overworld-CHUNK-0,0
say FLUIDS-DIAG-BUDGET-160-POLLS-5t-INTERVAL-RUNNER-DEFAULT-120s
execute if score phase fluids_check matches 0 run say FLUIDS-DIAG-PHASE-CHUNK-WAIT
execute if score phase fluids_check matches 1 run say FLUIDS-DIAG-PHASE-TRANSFER-WAIT
execute if loaded 8 71 8 run say FLUIDS-DIAG-CHUNK-LOADED
execute unless loaded 8 71 8 run say FLUIDS-DIAG-CHUNK-NOT-LOADED
execute if block 8 70 8 minecraft:water[level=0] run say FLUIDS-DIAG-SOURCE-WATER
execute unless block 8 70 8 minecraft:water[level=0] run say FLUIDS-DIAG-SOURCE-NOT-WATER
execute if block 8 71 8 grindless:hand_pump[facing=south] run say FLUIDS-DIAG-PUMP-PLACED
execute unless block 8 71 8 grindless:hand_pump[facing=south] run say FLUIDS-DIAG-PUMP-NOT-PLACED
execute if data block 8 71 8 Fluid{Id:"minecraft:water"} run say FLUIDS-DIAG-PUMP-HAS-WATER
execute unless data block 8 71 8 Fluid{Id:"minecraft:water"} run say FLUIDS-DIAG-PUMP-NO-WATER
execute if data block 8 71 9 Fluid{Id:"minecraft:water"} run say FLUIDS-DIAG-PIPE-HAS-WATER
execute unless data block 8 71 9 Fluid{Id:"minecraft:water"} run say FLUIDS-DIAG-PIPE-NO-WATER
execute if data block 8 71 10 Fluid{Id:"minecraft:water"} run say FLUIDS-DIAG-TANK-HAS-WATER
execute unless data block 8 71 10 Fluid{Id:"minecraft:water"} run say FLUIDS-DIAG-TANK-NO-WATER
execute if data block 10 70 8 Fluid{Id:"grindless:carbon_monoxide"} run say FLUIDS-DIAG-CO-TANK-HAS-CO
execute unless data block 10 70 8 Fluid{Id:"grindless:carbon_monoxide"} run say FLUIDS-DIAG-CO-TANK-NO-CO
execute if data block 10 70 9 Burn run say FLUIDS-DIAG-GENERATOR-HAS-BURN-FIELD
execute unless data block 10 70 9 Burn run say FLUIDS-DIAG-GENERATOR-MISSING-BURN-FIELD
execute store result score failed_burn fluids_check run data get block 10 70 9 Burn
execute if score failed_burn fluids_check matches 1.. run say FLUIDS-DIAG-CURRENT-BURN-POSITIVE
execute if score failed_burn fluids_check matches 0 run say FLUIDS-DIAG-CURRENT-BURN-ZERO
execute if score water_seen fluids_check matches 0 run say FLUIDS-DIAG-WATER-NOT-OBSERVED
execute if score water_seen fluids_check matches 1 run say FLUIDS-DIAG-WATER-OBSERVED
execute if score co_seen fluids_check matches 0 run say FLUIDS-DIAG-BURN-NOT-OBSERVED
execute if score co_seen fluids_check matches 1 run say FLUIDS-DIAG-BURN-OBSERVED
scoreboard players set phase fluids_check -1
function grindless_fluids:cleanup
