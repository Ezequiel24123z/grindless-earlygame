# One shared 160-poll limit (796 scheduled ticks); no extension of the runner's 120 seconds.
scoreboard players add tries harness_check 1
execute if score phase harness_check matches 0 if loaded 8 100 8 run function grindless_harness:spawn
scoreboard players set observed harness_check 0
execute if score phase harness_check matches 1 if loaded 8 100 8 if entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:voltaic_helmet",Count:1b}}] if entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:voltaic_chestplate",Count:1b}}] if entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:voltaic_leggings",Count:1b}}] if entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:voltaic_boots",Count:1b}}] if entity @e[type=minecraft:item,tag=grindless_harness_smoke,nbt={Item:{id:"grindless:flux_cell",Count:1b}}] run scoreboard players set observed harness_check 1
execute if score observed harness_check matches 1 run function grindless_harness:passed
execute if score phase harness_check matches 0..1 if score tries harness_check matches ..159 run schedule function grindless_harness:poll 5t replace
execute if score phase harness_check matches 0..1 if score tries harness_check matches 160.. run function grindless_harness:failed
