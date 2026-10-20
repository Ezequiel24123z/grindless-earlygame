# After the measured window, collect actual surplus; no inventories or energy are replenished.
setblock 8 280 5 minecraft:chest
scoreboard players set harvest_tries renewable_check 0
schedule function grindless_renewable:harvest_poll 10t replace
