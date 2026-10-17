scoreboard players operation digit renewable_check = number renewable_check
scoreboard players operation digit renewable_check /= divisor renewable_check
execute if score digit renewable_check matches 0 run say RENEWABLE-DIGIT-0
execute if score digit renewable_check matches 1 run say RENEWABLE-DIGIT-1
execute if score digit renewable_check matches 2 run say RENEWABLE-DIGIT-2
execute if score digit renewable_check matches 3 run say RENEWABLE-DIGIT-3
execute if score digit renewable_check matches 4 run say RENEWABLE-DIGIT-4
execute if score digit renewable_check matches 5 run say RENEWABLE-DIGIT-5
execute if score digit renewable_check matches 6 run say RENEWABLE-DIGIT-6
execute if score digit renewable_check matches 7 run say RENEWABLE-DIGIT-7
execute if score digit renewable_check matches 8 run say RENEWABLE-DIGIT-8
execute if score digit renewable_check matches 9 run say RENEWABLE-DIGIT-9
scoreboard players operation number renewable_check %= divisor renewable_check
scoreboard players operation divisor renewable_check /= ten renewable_check
execute if score divisor renewable_check matches 1.. run function grindless_renewable:digit
