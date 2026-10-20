say RENEWABLE-METRIC-GEOLOGICAL-CYCLES
scoreboard players operation number renewable_check = geo renewable_check
function grindless_renewable:number
say RENEWABLE-METRIC-BIOLOGICAL-CYCLES
scoreboard players operation number renewable_check = bio renewable_check
function grindless_renewable:number
say RENEWABLE-METRIC-AGGREGATE-STOCK
scoreboard players operation number renewable_check = aggregate renewable_check
function grindless_renewable:number
say RENEWABLE-METRIC-BIOMASS-STOCK
scoreboard players operation number renewable_check = biomass renewable_check
function grindless_renewable:number
say RENEWABLE-METRIC-BURNT-BIOMASS
scoreboard players operation number renewable_check = burnt renewable_check
function grindless_renewable:number
say RENEWABLE-METRIC-BURN-REMAINING
scoreboard players operation number renewable_check = burn renewable_check
function grindless_renewable:number
say RENEWABLE-METRIC-PAID-FU
scoreboard players operation number renewable_check = paid renewable_check
function grindless_renewable:number
say RENEWABLE-METRIC-STORED-FU
scoreboard players operation number renewable_check = stored renewable_check
function grindless_renewable:number
say RENEWABLE-METRIC-BALANCE-FU
scoreboard players operation number renewable_check = balance renewable_check
function grindless_renewable:number
say RENEWABLE-METRIC-CONSUMED-WATER-MB
scoreboard players operation number renewable_check = water renewable_check
function grindless_renewable:number
execute if score phase renewable_check matches 3 run say RENEWABLE-METRIC-STEADY-ELAPSED-TICKS
execute if score phase renewable_check matches 3 run scoreboard players operation number renewable_check = elapsed renewable_check
execute if score phase renewable_check matches 3 run function grindless_renewable:number
execute if score phase renewable_check matches 3 run say RENEWABLE-METRIC-STEADY-POTENTIAL-GAIN-FU
execute if score phase renewable_check matches 3 run scoreboard players operation number renewable_check = gain renewable_check
execute if score phase renewable_check matches 3 run function grindless_renewable:number
