scoreboard players set phase renewable_check 3
execute store result storage grindless:renewable_check steady.elapsed_ticks int 1 run scoreboard players get elapsed renewable_check
execute store result storage grindless:renewable_check steady.geological_cycles int 1 run scoreboard players get geo renewable_check
execute store result storage grindless:renewable_check steady.biological_cycles int 1 run scoreboard players get bio renewable_check
execute store result storage grindless:renewable_check steady.new_geological_cycles int 1 run scoreboard players get geo_delta renewable_check
execute store result storage grindless:renewable_check steady.new_biological_cycles int 1 run scoreboard players get bio_delta renewable_check
execute store result storage grindless:renewable_check steady.aggregate_stock int 1 run scoreboard players get aggregate renewable_check
execute store result storage grindless:renewable_check steady.biomass_stock int 1 run scoreboard players get biomass renewable_check
execute store result storage grindless:renewable_check steady.burnt_biomass int 1 run scoreboard players get burnt renewable_check
execute store result storage grindless:renewable_check steady.burn_remaining int 1 run scoreboard players get burn renewable_check
execute store result storage grindless:renewable_check steady.source_water_consumed_mb int 1 run scoreboard players get water renewable_check
execute store result storage grindless:renewable_check steady.generated_fu int 1 run scoreboard players get generated renewable_check
execute store result storage grindless:renewable_check steady.paid_fu int 1 run scoreboard players get paid renewable_check
execute store result storage grindless:renewable_check steady.stored_fu int 1 run scoreboard players get stored renewable_check
execute store result storage grindless:renewable_check steady.balance_fu int 1 run scoreboard players get balance renewable_check
execute store result storage grindless:renewable_check steady.potential_gain_fu int 1 run scoreboard players get gain renewable_check
say RENEWABLE-STEADY-MIN-4-CYCLES-800-TICKS
say RENEWABLE-STEADY-EXACT-STOCK-AND-FU-CONSERVATION
say RENEWABLE-AUTOMATIC-WATER-AND-PRODUCED-BIOMASS-POWER
data get storage grindless:renewable_check steady
say RENEWABLE-SOURCE-COMPLETE
