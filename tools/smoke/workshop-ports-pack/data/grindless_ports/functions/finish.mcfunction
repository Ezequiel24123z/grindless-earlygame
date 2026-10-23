execute if data block 4 280 3 {Fluid:{Id:"minecraft:water",Amount:1000},Items:[{Slot:0b,id:"minecraft:dirt",Count:2b}],Progress:160.0d} run say PORT-BREAK-CONTROLLER-KEPT
execute positioned 4 280 5 if entity @e[type=minecraft:item,distance=..2,nbt={Item:{id:"grindless:settling_fluid_output",Count:1b}}] unless entity @e[type=minecraft:item,distance=..2,nbt={Item:{tag:{BlockEntityTag:{Fluid:{Id:"minecraft:water"}}}}}] run say PORT-BREAK-NO-CONTROLLER-COPY
say WORKSHOP-PORTS-COMPLETE
