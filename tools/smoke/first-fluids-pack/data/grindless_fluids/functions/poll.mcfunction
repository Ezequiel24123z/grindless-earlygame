# Latch the two observed events separately; a later water arrival cannot require CO to burn twice.
scoreboard players add tries fluids_check 1
execute if score phase fluids_check matches 0 if loaded 8 71 8 run function grindless_fluids:setup
execute if score phase fluids_check matches 1 if score water_seen fluids_check matches 0 store result score water_amount fluids_check run data get block 8 71 10 Fluid.Amount
execute if score phase fluids_check matches 1 if score water_seen fluids_check matches 0 if score water_amount fluids_check matches 1.. if data block 8 71 10 Fluid{Id:"minecraft:water"} run say WATER-IN
execute if score phase fluids_check matches 1 if score water_seen fluids_check matches 0 if score water_amount fluids_check matches 1.. if data block 8 71 10 Fluid{Id:"minecraft:water"} run scoreboard players set water_seen fluids_check 1
execute if score phase fluids_check matches 1 if score co_seen fluids_check matches 0 store result score burn fluids_check run data get block 10 70 9 Burn
execute if score phase fluids_check matches 1 if score co_seen fluids_check matches 0 if score burn fluids_check matches 1.. run say CO-BURN
execute if score phase fluids_check matches 1 if score co_seen fluids_check matches 0 if score burn fluids_check matches 1.. run scoreboard players set co_seen fluids_check 1
execute if score phase fluids_check matches 1 if score water_seen fluids_check matches 1 if score co_seen fluids_check matches 1 run function grindless_fluids:passed
execute if score phase fluids_check matches 0..1 if score tries fluids_check matches ..159 run schedule function grindless_fluids:poll 5t replace
execute if score phase fluids_check matches 0..1 if score tries fluids_check matches 160.. run function grindless_fluids:failed
