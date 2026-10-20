scoreboard players set ready extractor_a01 1
fill 3 79 3 6 81 5 minecraft:air
fill 19 79 3 22 81 5 minecraft:air
setblock 5 80 4 minecraft:chest
execute positioned 5 80 4 run function grindless_extractor_a01:fill_chest
setblock 4 80 4 grindless:crude_extractor[facing=east,status=idle]{Progress:400.0d,Output:[{id:"grindless:helium_3",Count:64b}],Energy:{FluxStored:0L}}
setblock 21 80 4 minecraft:chest
execute positioned 21 80 4 run function grindless_extractor_a01:fill_chest
setblock 20 80 4 grindless:crude_extractor[facing=east,status=idle]
execute in grindless:drift run fill 3 79 3 6 81 5 minecraft:air
execute in grindless:drift run setblock 5 80 4 minecraft:chest
execute in grindless:drift positioned 5 80 4 run function grindless_extractor_a01:fill_chest
execute in grindless:drift run item replace block 5 80 4 container.0 with grindless:helium_3 63
execute in grindless:drift run setblock 4 80 4 grindless:crude_extractor[facing=east,status=idle]{Progress:123.5d,Output:[{id:"grindless:helium_3",Count:64b}],Energy:{FluxStored:16L}}
schedule function grindless_extractor_a01:checkpoint 16t replace
