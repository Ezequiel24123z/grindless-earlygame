scoreboard objectives add blueprint_check dummy
scoreboard players set phase blueprint_check 0
scoreboard players set tries blueprint_check 0
scoreboard players set tool_spawn blueprint_check -1
forceload add 0 0
schedule function grindless_blueprint:poll 1t replace
