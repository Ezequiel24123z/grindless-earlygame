# Function command feedback is suppressed; five decimal digits make console evidence durable.
execute if score number renewable_check matches ..-1 run say RENEWABLE-NUMBER-NEGATIVE
execute if score number renewable_check matches ..-1 run scoreboard players operation number renewable_check *= minus_one renewable_check
scoreboard players set divisor renewable_check 10000
execute if score number renewable_check matches 100000.. run say RENEWABLE-NUMBER-OVERFLOW
function grindless_renewable:digit
