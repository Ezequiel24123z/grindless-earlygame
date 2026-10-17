scoreboard players add blocked_tries workshop_check 1
scoreboard players set blocked_ready workshop_check 0
execute if block 4 280 12 grindless:quern[status=blocked] if data block 4 280 12 {Progress:60.0d,Items:[{Slot:0b,id:"minecraft:brick",Count:1b},{Slot:1b,id:"minecraft:sand",Count:64b},{Slot:2b,id:"minecraft:sand",Count:64b}]} run scoreboard players set blocked_ready workshop_check 1
execute if score blocked_ready workshop_check matches 1 run function grindless_workshop:unblock
execute if score blocked_ready workshop_check matches 0 if score blocked_tries workshop_check matches ..10 run schedule function grindless_workshop:blocked 10t replace
execute if score blocked_ready workshop_check matches 0 if score blocked_tries workshop_check matches 11.. run say WORKSHOP-FAIL-BLOCKED-TIMEOUT
