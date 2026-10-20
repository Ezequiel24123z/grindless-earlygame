say TROUGH-WRONG-FACING-HELD
execute unless data block 4 280 1 Fluid.Id if data block 4 280 3 {FluidOutputQuota:0,Fluid:{Amount:1000}} run say TROUGH-INPUT-NOT-EXPORTED
setblock 4 280 2 minecraft:air
setblock 4 280 5 grindless:settling_screen[facing=north]
schedule function grindless_settling:observe 20t replace
