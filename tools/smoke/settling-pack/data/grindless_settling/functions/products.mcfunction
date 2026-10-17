execute if data block 4 279 3 {Items:[{id:"minecraft:clay_ball",Count:2b},{id:"minecraft:gravel",Count:1b}]} run say TROUGH-NORTH-OUTPUT
execute if data block 10 279 5 {Items:[{id:"minecraft:clay_ball",Count:2b},{id:"minecraft:gravel",Count:1b}]} run say TROUGH-SOUTH-OUTPUT
execute if data block 5 279 10 {Items:[{id:"minecraft:clay_ball",Count:2b},{id:"minecraft:gravel",Count:1b}]} run say TROUGH-EAST-OUTPUT
execute if data block 10 279 10 {Items:[{id:"minecraft:clay_ball",Count:2b},{id:"minecraft:gravel",Count:1b}]} run say TROUGH-WEST-OUTPUT
execute if data block 4 280 3 {Fluid:{Id:"minecraft:water",Amount:750}} unless data block 4 280 3 Items[{Slot:0b}] run say TROUGH-WATER-RETAINED
setblock 4 280 2 minecraft:air
setblock 4 280 1 grindless:basic_tank[facing=north,status=idle]
setblock 4 280 2 grindless:clay_conduit[facing=north,status=idle]
schedule function grindless_settling:finish 120t replace
