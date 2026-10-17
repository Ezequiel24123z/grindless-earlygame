scoreboard objectives add settling_check dummy
scoreboard players set ready_tries settling_check 0
scoreboard players set work_tries settling_check 0
fill 1 277 0 14 282 14 minecraft:air
setblock 4 280 3 grindless:settling_trough[facing=north,status=idle]{Energy:{FluxStored:9999L}}
execute if data block 4 280 3 {Energy:{FluxStored:320L}} run say TROUGH-F0-BUFFER
setblock 4 280 4 grindless:settling_channel[facing=north]
setblock 4 280 5 grindless:settling_screen[facing=east]
setblock 4 279 3 minecraft:chest
setblock 4 281 3 minecraft:hopper{Items:[{Slot:0b,id:"minecraft:dirt",Count:2b}]}
setblock 5 280 3 grindless:basic_tank[facing=west,status=idle]{Fluid:{Id:"minecraft:water",Amount:1000,Temp:20.0d,Pressure:0.1d}}
setblock 4 280 1 grindless:basic_tank[facing=north,status=idle]
setblock 4 280 2 grindless:clay_conduit[facing=north,status=idle]
setblock 3 280 3 grindless:hand_crank_dynamo[facing=east,status=idle]{Energy:{FluxStored:800L}}
setblock 10 280 5 grindless:settling_trough[facing=south,status=idle]{ItemLayout:3,Items:[{Slot:0b,id:"minecraft:dirt",Count:2b}],Fluid:{Id:"minecraft:water",Amount:1000,Temp:20.0d,Pressure:0.1d}}
setblock 10 280 4 grindless:settling_channel[facing=south]
setblock 10 280 3 grindless:settling_screen[facing=south]
setblock 10 279 5 minecraft:chest
setblock 11 280 5 grindless:hand_crank_dynamo[facing=west,status=idle]{Energy:{FluxStored:800L}}
setblock 5 280 10 grindless:settling_trough[facing=east,status=idle]{ItemLayout:3,Items:[{Slot:0b,id:"minecraft:dirt",Count:2b}],Fluid:{Id:"minecraft:water",Amount:1000,Temp:20.0d,Pressure:0.1d}}
setblock 4 280 10 grindless:settling_channel[facing=east]
setblock 3 280 10 grindless:settling_screen[facing=east]
setblock 5 279 10 minecraft:chest
setblock 5 280 9 grindless:hand_crank_dynamo[facing=south,status=idle]{Energy:{FluxStored:800L}}
setblock 10 280 10 grindless:settling_trough[facing=west,status=idle]{ItemLayout:3,Items:[{Slot:0b,id:"minecraft:dirt",Count:2b}],Fluid:{Id:"minecraft:water",Amount:1000,Temp:20.0d,Pressure:0.1d}}
setblock 11 280 10 grindless:settling_channel[facing=west]
setblock 12 280 10 grindless:settling_screen[facing=west]
setblock 10 279 10 minecraft:chest
setblock 10 280 11 grindless:hand_crank_dynamo[facing=north,status=idle]{Energy:{FluxStored:800L}}
setblock 13 280 9 grindless:settling_trough{Fluid:{Id:"minecraft:water",Amount:1000}}
execute if data block 13 280 9 {Fluid:{Id:"minecraft:water",Amount:1000},FluidOutputQuota:0} run say TROUGH-LEGACY-STOCK-KEPT
setblock 13 280 12 grindless:settling_trough{Fluid:{Id:"minecraft:water",Amount:1000},FluidOutputQuota:2001}
execute if data block 13 280 12 {Fluid:{Id:"minecraft:water",Amount:1000},FluidOutputQuota:0} run say TROUGH-INVALID-QUOTA-KEPT
schedule function grindless_settling:ready 40t replace
