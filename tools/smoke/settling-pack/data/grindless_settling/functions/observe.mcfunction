scoreboard players add work_tries settling_check 1
execute store result score before settling_check run data get block 4 280 3 Progress 100
execute if score before settling_check matches 100..15999 run function grindless_settling:cover
execute unless score before settling_check matches 100..15999 if score work_tries settling_check matches ..20 run schedule function grindless_settling:observe 5t replace
execute unless score before settling_check matches 100..15999 if score work_tries settling_check matches 21.. run say TROUGH-FAIL-START-TIMEOUT
