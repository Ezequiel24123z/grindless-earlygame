# One shared 160-poll limit (796 scheduled ticks); the runner still owns its 120s wall-clock budget.
scoreboard players add tries blueprint_check 1
execute if score phase blueprint_check matches 0 if loaded 8 100 8 run function grindless_blueprint:spawn
scoreboard players set observed blueprint_check 0
execute if score phase blueprint_check matches 1 if loaded 8 100 8 if entity @e[type=minecraft:item,tag=grindless_blueprint_smoke,nbt={Item:{id:"grindless:blueprint_tool",Count:1b}}] run scoreboard players set observed blueprint_check 1
execute if score observed blueprint_check matches 1 run function grindless_blueprint:passed
execute if score phase blueprint_check matches 0..1 if score tries blueprint_check matches ..159 run schedule function grindless_blueprint:poll 5t replace
execute if score phase blueprint_check matches 0..1 if score tries blueprint_check matches 160.. run function grindless_blueprint:failed
