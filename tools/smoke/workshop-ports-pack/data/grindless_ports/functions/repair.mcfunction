execute if data block 5 280 4 {Fluid:{Id:"minecraft:water",Amount:1000}} run say PORT-INVALID-INPUT-HELD
execute if data block 4 280 3 {Items:[{Slot:0b,id:"minecraft:dirt",Count:2b}],Progress:160.0d,FluidOutputQuota:0} unless data block 4 280 3 Fluid.Id run say PORT-INVALID-CONTROLLER-HELD
setblock 4 280 5 grindless:settling_fluid_output[facing=north]
schedule function grindless_ports:observe 200t replace
