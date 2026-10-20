execute if data block 4 279 3 {Items:[{id:"minecraft:clay_ball",Count:2b},{id:"minecraft:gravel",Count:1b}]} run say TROUGH-NORTH-OUTPUT
execute if data block 10 279 5 {Items:[{id:"minecraft:clay_ball",Count:2b},{id:"minecraft:gravel",Count:1b}]} run say TROUGH-SOUTH-OUTPUT
execute if data block 5 279 10 {Items:[{id:"minecraft:clay_ball",Count:2b},{id:"minecraft:gravel",Count:1b}]} run say TROUGH-EAST-OUTPUT
execute if data block 10 279 10 {Items:[{id:"minecraft:clay_ball",Count:2b},{id:"minecraft:gravel",Count:1b}]} run say TROUGH-WEST-OUTPUT
execute if data block 4 280 3 {Fluid:{Id:"minecraft:water",Amount:750},FluidOutputQuota:750} unless data block 4 280 3 Items[{Slot:0b}] run say TROUGH-WATER-RETAINED
data modify storage grindless:settling_check output set from block 4 280 3
data modify block 4 280 3 Items set value []
setblock 4 280 3 minecraft:air
setblock 4 280 3 grindless:settling_trough[facing=north,status=idle]
data modify block 4 280 3 Fluid set from storage grindless:settling_check output.Fluid
data modify block 4 280 3 FluidOutputQuota set from storage grindless:settling_check output.FluidOutputQuota
execute if data block 4 280 3 {Fluid:{Id:"minecraft:water",Amount:750},FluidOutputQuota:750} run say TROUGH-OUTPUT-OWNERSHIP-RESTORED
setblock 4 280 2 grindless:clay_conduit[facing=north,status=idle]
schedule function grindless_settling:finish 120t replace
