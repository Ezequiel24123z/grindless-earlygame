scoreboard players set phase renewable_check -1
execute store result storage grindless:renewable_check failure.coal_remaining int 1 run scoreboard players get coal renewable_check
execute store result storage grindless:renewable_check failure.fuel_read_success int 1 run scoreboard players get fuel_read renewable_check
execute store result storage grindless:renewable_check failure.fuel_entries int 1 run scoreboard players get fuel_slots renewable_check
execute store result storage grindless:renewable_check failure.fuel_count_read_success int 1 run scoreboard players get fuel_count_read renewable_check
execute store result storage grindless:renewable_check failure.fuel_count int 1 run scoreboard players get fuel_count renewable_check
execute store result storage grindless:renewable_check failure.fuel_inventory_valid int 1 run scoreboard players get fuel_valid renewable_check
execute store result storage grindless:renewable_check failure.balance_fu int 1 run scoreboard players get balance renewable_check
execute store result storage grindless:renewable_check failure.geological_cycles int 1 run scoreboard players get geo renewable_check
execute store result storage grindless:renewable_check failure.biological_cycles int 1 run scoreboard players get bio renewable_check
execute store result storage grindless:renewable_check failure.aggregate_stock int 1 run scoreboard players get aggregate renewable_check
execute store result storage grindless:renewable_check failure.biomass_stock int 1 run scoreboard players get biomass renewable_check
execute store result storage grindless:renewable_check failure.burn_remaining int 1 run scoreboard players get burn renewable_check
execute store result storage grindless:renewable_check failure.generated_fu int 1 run scoreboard players get generated renewable_check
execute store result storage grindless:renewable_check failure.paid_fu int 1 run scoreboard players get paid renewable_check
execute store result storage grindless:renewable_check failure.stored_fu int 1 run scoreboard players get stored renewable_check
say RENEWABLE-FAIL-BALANCE-OR-PRODUCTION-TIMEOUT
data get storage grindless:renewable_check failure
data get block 8 280 8
data get block 8 280 6
data get block 8 280 7
function grindless_renewable:report
