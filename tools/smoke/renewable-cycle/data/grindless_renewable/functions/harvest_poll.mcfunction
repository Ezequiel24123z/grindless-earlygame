scoreboard players add harvest_tries renewable_check 1
scoreboard players set surplus renewable_check 0
execute if data block 8 280 5 Items[{id:"grindless:biomass"}] store result score surplus renewable_check run data get block 8 280 5 Items[{id:"grindless:biomass"}].Count
execute if score phase renewable_check matches 3 if score surplus renewable_check matches 12.. run setblock 8 281 6 minecraft:stone
execute if score phase renewable_check matches 3 if score surplus renewable_check matches 12.. run scoreboard players set phase renewable_check 4
execute if score phase renewable_check matches 4 unless data block 8 281 8 Items[{}] run function grindless_renewable:divert
scoreboard players set mineral_stock renewable_check 0
scoreboard players set clay_stock renewable_check 0
scoreboard players set fuel_stock renewable_check 0
execute if score phase renewable_check matches 5 if data block 9 280 8 Items[{id:"grindless:mineral_aggregate"}] store result score mineral_stock renewable_check run data get block 9 280 8 Items[{id:"grindless:mineral_aggregate"}].Count
execute if score phase renewable_check matches 5 if data block 9 280 8 Items[{id:"minecraft:clay_ball"}] store result score clay_stock renewable_check run data get block 9 280 8 Items[{id:"minecraft:clay_ball"}].Count
execute if score phase renewable_check matches 5 if data block 8 280 7 Fuel[{id:"grindless:biomass"}] store result score fuel_stock renewable_check run data get block 8 280 7 Fuel[{id:"grindless:biomass"}].Count
execute if score phase renewable_check matches 5 if score mineral_stock renewable_check matches 6.. if score clay_stock renewable_check matches 6.. if score fuel_stock renewable_check matches 22.. run function grindless_renewable:downstream_begin
execute if score phase renewable_check matches 3..5 if score harvest_tries renewable_check matches ..400 run schedule function grindless_renewable:harvest_poll 10t replace
execute if score phase renewable_check matches 3..5 if score harvest_tries renewable_check matches 401.. run function grindless_renewable:downstream_failed
