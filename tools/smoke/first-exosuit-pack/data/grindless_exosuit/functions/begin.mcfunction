scoreboard objectives add exosuit_check dummy
scoreboard players set phase exosuit_check 0
scoreboard players set tries exosuit_check 0
scoreboard players set helmet_spawn exosuit_check -1
scoreboard players set chest_spawn exosuit_check -1
scoreboard players set tap_spawn exosuit_check -1
scoreboard players set legs_spawn exosuit_check -1
forceload add 0 0
schedule function grindless_exosuit:poll 1t replace
