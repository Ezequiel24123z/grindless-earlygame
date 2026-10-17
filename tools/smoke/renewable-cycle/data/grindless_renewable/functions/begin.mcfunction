# Finite equipment and one named startup coal; no claimed product is supplied.
scoreboard objectives add renewable_check dummy
scoreboard players set phase renewable_check 1
scoreboard players set tries renewable_check 0
scoreboard players set four renewable_check 4
scoreboard players set eight renewable_check 8
scoreboard players set water_cost renewable_check 250
scoreboard players set geo_cost renewable_check 800
scoreboard players set bio_cost renewable_check 640
scoreboard players set fuel_value renewable_check 1600
scoreboard players set ten renewable_check 10
scoreboard players set minus_one renewable_check -1
gamerule doDaylightCycle false
gamerule doWeatherCycle false
time set noon
weather clear
fill 2 277 2 30 283 30 minecraft:air
fill 2 278 2 30 279 30 minecraft:stone
setblock 7 279 6 minecraft:water
setblock 7 279 8 minecraft:water
setblock 8 280 8 grindless:geological_collector[facing=north,status=idle]
setblock 8 280 6 grindless:biocultivator[facing=north,status=idle]
setblock 8 280 7 grindless:biomass_dynamo[facing=north,status=idle]{Fuel:[{Slot:0b,id:"minecraft:coal",Count:1b}]}
setblock 7 280 8 grindless:hand_pump[facing=east,status=idle]
setblock 7 280 6 grindless:hand_pump[facing=east,status=idle]
# Aggregate crosses the hopper chain; clay is rejected by the culture and retained.
setblock 8 281 8 minecraft:hopper[facing=east]
setblock 9 281 8 minecraft:hopper[facing=north]
setblock 9 281 7 minecraft:hopper[facing=north]
setblock 9 281 6 minecraft:hopper[facing=down]
setblock 9 280 6 minecraft:hopper[facing=west]
execute if data block 8 280 8 {Energy:{FluxStored:0L},Progress:0.0d} if data block 8 280 6 {Energy:{FluxStored:0L},Progress:0.0d} if data block 8 280 7 {Energy:{FluxStored:0L},Burn:0,Fuel:[{Slot:0b,id:"minecraft:coal",Count:1b}]} unless data block 8 280 8 Items[{}] unless data block 8 280 6 Items[{}] unless data block 8 280 8 Fluid.Id unless data block 8 280 6 Fluid.Id run say RENEWABLE-FINITE-STARTUP-ONE-COAL-ZERO-STORED-FU
say RENEWABLE-ENVIRONMENT-DAYLIGHT-ROCK-BED-PLACED-WATER
schedule function grindless_renewable:poll 10t replace
