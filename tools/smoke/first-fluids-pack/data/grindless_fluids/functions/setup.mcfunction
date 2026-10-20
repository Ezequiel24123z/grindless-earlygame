scoreboard players set phase fluids_check 1
# Stage one enclosed circuit without yielding to terrain, weather or fluid ticks.
fill 6 68 6 12 73 12 minecraft:stone
fill 7 70 7 11 72 11 minecraft:air
fill 7 69 7 9 70 9 minecraft:stone
setblock 8 71 8 grindless:hand_pump[facing=south,status=idle]
execute if block 8 71 8 grindless:hand_pump[facing=south] run say PUMP-OK
setblock 8 71 9 grindless:clay_conduit[facing=south,status=idle]
execute if block 8 71 9 grindless:clay_conduit[facing=south] run say PIPE-OK
setblock 8 71 10 grindless:basic_tank[facing=south,status=idle]
execute if block 8 71 10 grindless:basic_tank[facing=south] run say TANK-OK
setblock 8 70 8 minecraft:water[level=0]
setblock 10 70 8 grindless:basic_tank[facing=south,status=idle]{Fluid:{Id:"grindless:carbon_monoxide",Amount:1000,Temp:20.0d,Pressure:0.1d}}
setblock 10 70 9 grindless:thermal_generator[facing=south,status=idle]
say WAIT-FLUID
