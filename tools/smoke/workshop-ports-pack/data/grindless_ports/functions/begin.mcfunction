scoreboard objectives add ports_check dummy
fill 1 277 0 14 283 14 minecraft:air
setblock 4 280 3 grindless:settling_trough[facing=north,status=idle]{ItemLayout:3,Items:[{Slot:0b,id:"minecraft:dirt",Count:2b}],Progress:160.0d,Recipe:"bootstrap/settle_clay",WorkRecipe:"bootstrap/settle_clay"}
setblock 4 280 4 grindless:settling_fluid_input[facing=north]
setblock 4 280 5 grindless:settling_fluid_output[facing=east]
setblock 5 280 4 grindless:clay_conduit[facing=west,status=idle]{Fluid:{Id:"minecraft:water",Amount:1000,Temp:20.0d,Pressure:0.1d}}
setblock 5 280 5 grindless:clay_conduit[facing=east,status=idle]
setblock 6 280 5 grindless:basic_tank[facing=west,status=idle]
setblock 4 279 3 minecraft:chest
fill 10 280 3 11 281 4 minecraft:bricks
setblock 10 280 3 grindless:firing_chamber[facing=north,status=idle]{Progress:200.0d,Recipe:"bootstrap/insulators",WorkRecipe:"bootstrap/insulators"}
setblock 11 280 3 grindless:workshop_item_input[facing=north]
setblock 11 281 3 grindless:workshop_item_output[facing=north]
setblock 11 280 1 minecraft:chest{Items:[{Slot:0b,id:"grindless:unfired_ceramic",Count:3b},{Slot:1b,id:"minecraft:charcoal",Count:1b}]}
setblock 11 280 2 grindless:crude_manipulator[facing=south,status=idle]
setblock 11 281 1 minecraft:chest
setblock 11 281 2 grindless:crude_manipulator[facing=north,status=idle]
setblock 2 280 10 grindless:basic_tank[facing=north,status=idle]{Fluid:{Id:"minecraft:water",Amount:1234,Temp:31.5d,Pressure:0.1d}}
setblock 2 280 10 grindless:basic_tank[facing=east,status=idle]
execute if data block 2 280 10 {Fluid:{Id:"minecraft:water",Amount:1234,Temp:31.5d,Pressure:0.1d}} run say TANK-ROTATION-KEPT
setblock 2 280 10 minecraft:air destroy
scoreboard players set tank_drops ports_check 0
execute positioned 2 280 10 as @e[type=minecraft:item,distance=..2,nbt={Item:{id:"grindless:basic_tank"}}] run scoreboard players add tank_drops ports_check 1
execute if score tank_drops ports_check matches 1 run say TANK-ONE-FILLED-DROP
execute positioned 2 280 10 as @e[type=minecraft:item,distance=..2,nbt={Item:{id:"grindless:basic_tank",Count:1b}}] if data entity @s {Item:{tag:{BlockEntityTag:{Fluid:{Id:"minecraft:water",Amount:1234,Temp:31.5d,Pressure:0.1d}}}}} run say TANK-DROP-METADATA-KEPT
schedule function grindless_ports:repair 20t replace
