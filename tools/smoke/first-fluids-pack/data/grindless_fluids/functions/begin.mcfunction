scoreboard objectives add fluids_check dummy
scoreboard players set phase fluids_check 0
scoreboard players set tries fluids_check 0
scoreboard players set water_seen fluids_check 0
scoreboard players set co_seen fluids_check 0
forceload add 0 0
schedule function grindless_fluids:poll 1t replace
