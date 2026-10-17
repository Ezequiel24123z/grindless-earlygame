scoreboard players add firing_tries workshop_check 1
execute store result score before workshop_check run data get block 12 280 4 Progress 100
execute if score before workshop_check matches 100..19999 run function grindless_workshop:pause
execute unless score before workshop_check matches 100..19999 if score firing_tries workshop_check matches ..40 run schedule function grindless_workshop:break 10t replace
execute unless score before workshop_check matches 100..19999 if score firing_tries workshop_check matches 41.. run say WORKSHOP-FAIL-START-TIMEOUT
