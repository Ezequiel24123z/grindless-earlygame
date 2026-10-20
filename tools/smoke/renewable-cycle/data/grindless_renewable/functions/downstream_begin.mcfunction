scoreboard players set phase renewable_check 6
scoreboard players set downstream_tries renewable_check 0
scoreboard players set milling renewable_check 0
scoreboard players set mixing renewable_check 0
scoreboard players set carbon renewable_check 0
scoreboard players set wet_seen renewable_check 0
scoreboard players set wet_products renewable_check 0
scoreboard players set copper renewable_check 0
scoreboard players set glass renewable_check 0
scoreboard players set ceramics renewable_check 0
scoreboard players set bricks renewable_check 0
scoreboard players set core renewable_check 0
scoreboard players set terminal renewable_check 0
scoreboard players set recovered renewable_check 0
# Suspend the source environment only after taking the steady-state evidence and real surplus.
setblock 8 279 8 minecraft:air
# This finite, non-consumed distribution fixture starts empty and uses only the existing biomass.
setblock 12 280 20 grindless:flux_pylon_mk1[status=idle]
setblock 16 280 4 grindless:quern[facing=north,status=idle]
setblock 16 279 4 minecraft:chest
setblock 20 280 4 grindless:settling_trough[facing=north,status=idle]
setblock 20 280 5 grindless:settling_channel[facing=north]
setblock 20 280 6 grindless:settling_screen[facing=north]
setblock 20 279 4 minecraft:chest
setblock 21 279 4 minecraft:water
setblock 21 280 4 grindless:hand_pump[facing=west,status=idle]
setblock 20 280 3 grindless:clay_conduit[facing=north,status=idle]
setblock 20 280 2 grindless:basic_tank[facing=north,status=idle]
setblock 16 280 8 grindless:clay_mixer[facing=north,status=idle]
setblock 16 279 8 minecraft:chest
setblock 17 279 8 minecraft:water
setblock 17 280 8 grindless:hand_pump[facing=west,status=idle]
setblock 24 280 4 grindless:firing_chamber[facing=north,status=idle]
setblock 25 280 4 minecraft:bricks
setblock 24 280 5 minecraft:bricks
setblock 25 280 5 minecraft:bricks
fill 24 281 4 25 281 5 minecraft:bricks
setblock 24 279 4 minecraft:chest
setblock 20 280 10 grindless:firing_chamber[facing=north,status=idle]
setblock 21 280 10 minecraft:bricks
setblock 20 280 11 minecraft:bricks
setblock 21 280 11 minecraft:bricks
fill 20 281 10 21 281 11 minecraft:bricks
setblock 20 279 10 minecraft:chest
setblock 24 280 10 grindless:firing_chamber[facing=north,status=idle]
setblock 25 280 10 minecraft:bricks
setblock 24 280 11 minecraft:bricks
setblock 25 280 11 minecraft:bricks
fill 24 281 10 25 281 11 minecraft:bricks
setblock 24 279 10 minecraft:chest
setblock 24 280 16 grindless:firing_chamber[facing=north,status=idle]
setblock 25 280 16 minecraft:bricks
setblock 24 280 17 minecraft:bricks
setblock 25 280 17 minecraft:bricks
fill 24 281 16 25 281 17 minecraft:bricks
setblock 24 279 16 minecraft:chest
setblock 16 280 12 grindless:assembly_bench[facing=north,status=idle]
setblock 16 279 12 minecraft:chest
setblock 16 280 16 grindless:research_terminal[facing=north,status=idle]
setblock 16 281 16 minecraft:hopper[facing=down]
setblock 16 279 16 minecraft:hopper[facing=down]
setblock 16 278 16 minecraft:chest
# These paired fixture hand-loads copy only produced stock, then debit it in the same function.
# They do not claim automated downstream routing or a crafting-table interaction.
data modify storage grindless:renewable_check move set from block 9 280 8 Items[{id:"grindless:mineral_aggregate"}]
execute store result score remaining renewable_check run data get storage grindless:renewable_check move.Count
scoreboard players remove remaining renewable_check 2
data modify storage grindless:renewable_check move.Count set value 2b
data modify storage grindless:renewable_check move.Slot set value 0b
data modify block 16 280 4 Items append from storage grindless:renewable_check move
execute store result block 9 280 8 Items[{id:"grindless:mineral_aggregate"}].Count byte 1 run scoreboard players get remaining renewable_check
data modify block 20 280 4 Items append from storage grindless:renewable_check move
scoreboard players remove remaining renewable_check 2
execute store result block 9 280 8 Items[{id:"grindless:mineral_aggregate"}].Count byte 1 run scoreboard players get remaining renewable_check
data modify storage grindless:renewable_check move set from block 8 280 5 Items[{id:"grindless:biomass"}]
execute store result score remaining renewable_check run data get storage grindless:renewable_check move.Count
scoreboard players remove remaining renewable_check 12
data modify storage grindless:renewable_check move.Count set value 12b
data modify storage grindless:renewable_check move.Slot set value 0b
data modify block 24 280 4 Items append from storage grindless:renewable_check move
execute if score remaining renewable_check matches 0 run data remove block 8 280 5 Items[{id:"grindless:biomass"}]
execute if score remaining renewable_check matches 1.. store result block 8 280 5 Items[{id:"grindless:biomass"}].Count byte 1 run scoreboard players get remaining renewable_check
say RENEWABLE-DOWNSTREAM-USES-ONLY-HARVESTED-INPUTS-AND-POWER
schedule function grindless_renewable:downstream_poll 10t replace
