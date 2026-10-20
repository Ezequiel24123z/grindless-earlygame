execute unless data block 5 280 4 Fluid.Id run say PORT-FLUID-INPUT-PAID
execute if data block 6 280 5 {Fluid:{Id:"minecraft:water",Amount:750}} unless data block 4 280 3 Fluid.Id if data block 4 280 3 {FluidOutputQuota:0} run say PORT-FLUID-OUTPUT-750
execute if data block 4 279 3 {Items:[{id:"minecraft:clay_ball",Count:2b},{id:"minecraft:gravel",Count:1b}]} run say PORT-TROUGH-PRODUCTS
execute unless data block 11 280 1 Items[{id:"grindless:unfired_ceramic"}] unless data block 11 280 1 Items[{id:"minecraft:charcoal"}] run say PORT-ITEM-INPUT-PAID
execute if data block 11 281 1 {Items:[{id:"grindless:ceramic_insulator",Count:2b}]} run say PORT-ITEM-OUTPUT-INSULATORS
execute if data block 11 281 1 {Items:[{id:"grindless:ceramic_grog",Count:1b}]} run say PORT-ITEM-OUTPUT-GROG
data merge block 4 280 3 {ItemLayout:3,Items:[{Slot:0b,id:"minecraft:dirt",Count:2b}],Fluid:{Id:"minecraft:water",Amount:1000,Temp:20.0d,Pressure:0.1d},FluidOutputQuota:0,Progress:160.0d,Recipe:"bootstrap/settle_clay",WorkRecipe:"bootstrap/settle_clay"}
setblock 4 280 5 minecraft:air destroy
schedule function grindless_ports:finish 20t replace
