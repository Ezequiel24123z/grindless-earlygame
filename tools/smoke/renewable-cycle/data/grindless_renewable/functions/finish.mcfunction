scoreboard players set phase renewable_check 7
say RENEWABLE-FIRING-4-BRICKS
say RENEWABLE-FIRING-2-GLASS
say RENEWABLE-REDUCTION-2-COPPER-2-SLAG
say RENEWABLE-TERMINAL-NATIVE-CALIBRATED-CORE
say RENEWABLE-EXIT-ROOT-SUPPLY-READY
scoreboard players set recovered renewable_check 0
execute if data block 20 280 2 {Fluid:{Id:"minecraft:water"}} store result score recovered renewable_check run data get block 20 280 2 Fluid.Amount
execute if score recovered renewable_check matches 750.. run say RENEWABLE-SETTLING-ACTUAL-WATER-EXPORT
say RENEWABLE-METRIC-DOWNSTREAM-RECOVERED-WATER-MB
scoreboard players operation number renewable_check = recovered renewable_check
function grindless_renewable:number
say RENEWABLE-HAND-CRAFT-AND-RECOVERY-REMAIN-MANUAL
say RENEWABLE-COMPLETE
