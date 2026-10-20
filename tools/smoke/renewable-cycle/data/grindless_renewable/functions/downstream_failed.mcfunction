scoreboard players set phase renewable_check -2
say RENEWABLE-FAIL-DOWNSTREAM-TIMEOUT
say RENEWABLE-METRIC-DOWNSTREAM-TRIES
scoreboard players operation number renewable_check = downstream_tries renewable_check
function grindless_renewable:number
say RENEWABLE-METRIC-HARVESTED-AGGREGATE
scoreboard players operation number renewable_check = mineral_stock renewable_check
function grindless_renewable:number
say RENEWABLE-METRIC-HARVESTED-CLAY
scoreboard players operation number renewable_check = clay_stock renewable_check
function grindless_renewable:number
say RENEWABLE-METRIC-RETAINED-SOURCE-FUEL
scoreboard players operation number renewable_check = fuel_stock renewable_check
function grindless_renewable:number
