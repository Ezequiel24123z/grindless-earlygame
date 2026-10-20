scoreboard objectives add harness_check dummy
scoreboard players set phase harness_check 0
scoreboard players set tries harness_check 0
scoreboard players set helmet_spawn harness_check -1
scoreboard players set chest_spawn harness_check -1
scoreboard players set legs_spawn harness_check -1
scoreboard players set boots_spawn harness_check -1
scoreboard players set cell_spawn harness_check -1
forceload add 0 0
schedule function grindless_harness:poll 1t replace
