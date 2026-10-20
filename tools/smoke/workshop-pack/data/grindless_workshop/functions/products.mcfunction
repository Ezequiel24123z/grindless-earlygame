execute if data block 4 279 4 Items[{id:"minecraft:sand",Count:1b}] run say WORKSHOP-MILLING-OUTPUT
execute if data block 8 279 4 Items[{id:"grindless:unfired_ceramic",Count:3b}] run say WORKSHOP-MIXING-OUTPUT
execute unless data block 8 280 4 Fluid.Id unless data block 8 280 5 Fluid.Id run say WORKSHOP-WATER-CONSUMED
execute if data block 12 278 4 {Items:[{id:"grindless:ceramic_insulator",Count:2b},{id:"grindless:ceramic_grog",Count:1b}]} run say WORKSHOP-FIRING-OUTPUT
execute if data block 4 279 9 Items[{id:"grindless:ceramic_grog",Count:1b}] run say WORKSHOP-SWITCH-OUTPUT
execute unless data block 4 279 9 Items[{id:"minecraft:sand"}] run say WORKSHOP-NO-SWITCH-SAND
execute unless data block 4 280 12 Items[{Slot:0b}] if data block 4 280 12 Items[{id:"grindless:ceramic_grog",Count:1b}] run say WORKSHOP-BLOCKED-RECOVERED
item replace block 5 280 12 container.2 with minecraft:air
data modify block 8 280 4 Items set value [{Slot:0b,id:"minecraft:clay_ball",Count:2b},{Slot:1b,id:"grindless:ceramic_grog",Count:1b}]
data merge block 8 280 5 {Fluid:{Id:"minecraft:water",Amount:250,Temp:20.0d,Pressure:0.1d}}
data merge block 7 280 4 {Energy:{FluxStored:1600L}}
schedule function grindless_workshop:finish 140t replace
