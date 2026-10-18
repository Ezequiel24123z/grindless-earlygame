# One shared 160-poll limit (796 scheduled ticks); the runner still owns its 120s wall-clock budget.
scoreboard players add tries exosuit_check 1
execute if score phase exosuit_check matches 0 if loaded 8 100 8 run function grindless_exosuit:spawn
scoreboard players set observed exosuit_check 0
execute if score phase exosuit_check matches 1 if loaded 8 100 8 if entity @e[type=minecraft:item,tag=grindless_exosuit_smoke,nbt={Item:{id:"grindless:flux_exosuit_helmet",Count:1b}}] if entity @e[type=minecraft:item,tag=grindless_exosuit_smoke,nbt={Item:{id:"grindless:flux_exosuit_chestplate",Count:1b}}] if entity @e[type=minecraft:item,tag=grindless_exosuit_smoke,nbt={Item:{id:"grindless:network_tap",Count:1b}}] if entity @e[type=minecraft:item,tag=grindless_exosuit_smoke,nbt={Item:{id:"grindless:exoskeleton_legs",Count:1b}}] run scoreboard players set observed exosuit_check 1
execute if score observed exosuit_check matches 1 run function grindless_exosuit:passed
execute if score phase exosuit_check matches 0..1 if score tries exosuit_check matches ..159 run schedule function grindless_exosuit:poll 5t replace
execute if score phase exosuit_check matches 0..1 if score tries exosuit_check matches 160.. run function grindless_exosuit:failed
