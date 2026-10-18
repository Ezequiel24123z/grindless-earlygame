scoreboard players add tries renewable_check 1
function grindless_renewable:metrics
execute unless score fuel_valid renewable_check matches 1 run say RENEWABLE-FAIL-FUEL-INVENTORY-UNAVAILABLE-OR-INVALID
execute unless score fuel_valid renewable_check matches 1 run function grindless_renewable:failed
execute if score phase renewable_check matches 1..2 unless score balance renewable_check matches 0 run function grindless_renewable:failed
execute if score phase renewable_check matches 1 if score coal renewable_check matches 0 if score startup renewable_check matches 0 if score bio renewable_check matches 3.. if data block 8 280 7 {Duration:200} unless data block 8 280 7 Fuel[{id:"minecraft:coal"}] run function grindless_renewable:baseline
execute if score phase renewable_check matches 2 run function grindless_renewable:steady
execute if score phase renewable_check matches 1..2 if score tries renewable_check matches ..600 run schedule function grindless_renewable:poll 10t replace
execute if score phase renewable_check matches 1..2 if score tries renewable_check matches 601.. run function grindless_renewable:failed
