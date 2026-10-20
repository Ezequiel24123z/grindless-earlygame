scoreboard players operation elapsed renewable_check = now renewable_check
scoreboard players operation elapsed renewable_check -= start_time renewable_check
scoreboard players operation geo_delta renewable_check = geo renewable_check
scoreboard players operation geo_delta renewable_check -= start_geo renewable_check
scoreboard players operation bio_delta renewable_check = bio renewable_check
scoreboard players operation bio_delta renewable_check -= start_bio renewable_check
scoreboard players operation gain renewable_check = potential renewable_check
scoreboard players operation gain renewable_check -= start_potential renewable_check
execute if score elapsed renewable_check matches 800.. if score geo_delta renewable_check matches 4.. if score bio_delta renewable_check matches 4.. if score biomass renewable_check matches 32.. if score burnt renewable_check matches 1.. if score startup renewable_check matches 0 if score balance renewable_check matches 0 if score gain renewable_check matches 15000.. if block 8 279 8 minecraft:stone if block 7 279 8 minecraft:water if block 7 279 6 minecraft:water if block 7 280 8 grindless:hand_pump if block 7 280 6 grindless:hand_pump run function grindless_renewable:source_complete
