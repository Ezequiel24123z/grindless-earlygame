scoreboard players set phase renewable_check 2
scoreboard players operation start_geo renewable_check = geo renewable_check
scoreboard players operation start_bio renewable_check = bio renewable_check
scoreboard players operation start_time renewable_check = now renewable_check
scoreboard players operation start_potential renewable_check = potential renewable_check
execute store result storage grindless:renewable_check baseline.geological_cycles int 1 run scoreboard players get geo renewable_check
execute store result storage grindless:renewable_check baseline.biological_cycles int 1 run scoreboard players get bio renewable_check
execute store result storage grindless:renewable_check baseline.game_tick int 1 run scoreboard players get now renewable_check
execute store result storage grindless:renewable_check baseline.biomass_stock int 1 run scoreboard players get biomass renewable_check
execute store result storage grindless:renewable_check baseline.burn_remaining int 1 run scoreboard players get burn renewable_check
execute store result storage grindless:renewable_check baseline.stored_fu int 1 run scoreboard players get stored renewable_check
execute store result storage grindless:renewable_check baseline.potential_fu int 1 run scoreboard players get potential renewable_check
execute store result storage grindless:renewable_check baseline.balance_fu int 1 run scoreboard players get balance renewable_check
say RENEWABLE-EXTERNAL-STARTUP-EXHAUSTED
data get storage grindless:renewable_check baseline
function grindless_renewable:report
