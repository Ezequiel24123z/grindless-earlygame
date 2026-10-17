scoreboard objectives add workshop_check dummy
scoreboard players set blocked_tries workshop_check 0
fill 2 277 2 14 282 14 minecraft:air
setblock 4 280 4 grindless:quern[facing=north,status=idle]{Energy:{FluxStored:9999L}}
execute if data block 4 280 4 {Energy:{FluxStored:320L}} run say WORKSHOP-F0-BUFFER
data merge block 4 280 4 {Energy:{FluxStored:0L}}
setblock 4 279 4 minecraft:chest
setblock 4 281 4 minecraft:hopper{Items:[{Slot:0b,id:"minecraft:gravel",Count:1b}]}
setblock 3 280 4 grindless:hand_crank_dynamo[facing=east,status=idle]{Energy:{FluxStored:1600L}}
setblock 8 280 4 grindless:clay_mixer[facing=north,status=idle]
item replace block 8 280 4 container.0 with minecraft:clay_ball 2
item replace block 8 280 4 container.1 with minecraft:sand
setblock 8 279 4 minecraft:chest
setblock 7 280 4 grindless:hand_crank_dynamo[facing=east,status=idle]{Energy:{FluxStored:1600L}}
setblock 12 280 4 grindless:firing_chamber[facing=north,status=idle]
setblock 13 280 4 minecraft:bricks
setblock 12 280 5 minecraft:bricks
setblock 13 280 5 minecraft:bricks
fill 12 281 4 13 281 5 minecraft:bricks
setblock 12 278 4 minecraft:chest
setblock 12 279 4 minecraft:hopper
setblock 12 280 3 minecraft:hopper[facing=south]{Items:[{Slot:0b,id:"grindless:unfired_ceramic",Count:3b},{Slot:1b,id:"minecraft:charcoal",Count:1b}]}
setblock 11 280 4 grindless:hand_crank_dynamo[facing=east,status=idle]{Energy:{FluxStored:1600L}}
setblock 4 280 9 grindless:quern[facing=north,status=idle]{Items:[{Slot:0b,id:"minecraft:gravel",Count:1b}],ItemLayout:3}
setblock 4 279 9 minecraft:chest
setblock 3 280 9 grindless:hand_crank_dynamo[facing=east,status=idle]{Energy:{FluxStored:1600L}}
setblock 4 280 12 grindless:quern[facing=north,status=idle]{Items:[{Slot:0b,id:"minecraft:brick",Count:1b},{Slot:1b,id:"minecraft:sand",Count:64b},{Slot:2b,id:"minecraft:sand",Count:64b}],ItemLayout:3}
setblock 3 280 12 grindless:hand_crank_dynamo[facing=east,status=idle]{Energy:{FluxStored:1600L}}
setblock 5 280 12 minecraft:dispenser{Items:[{Slot:0b,id:"minecraft:stone",Count:64b},{Slot:1b,id:"minecraft:stone",Count:64b},{Slot:2b,id:"minecraft:stone",Count:64b},{Slot:3b,id:"minecraft:stone",Count:64b},{Slot:4b,id:"minecraft:stone",Count:64b},{Slot:5b,id:"minecraft:stone",Count:64b},{Slot:6b,id:"minecraft:stone",Count:64b},{Slot:7b,id:"minecraft:stone",Count:64b},{Slot:8b,id:"minecraft:stone",Count:64b}]}
schedule function grindless_workshop:water 20t replace
schedule function grindless_workshop:switch 50t replace
schedule function grindless_workshop:break 80t replace
schedule function grindless_workshop:blocked 80t replace
