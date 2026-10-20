scoreboard players set mixing renewable_check 1
say RENEWABLE-SAND-3-FROM-SOURCE-GRAVEL
data modify storage grindless:renewable_check move set from block 9 280 8 Items[{id:"minecraft:clay_ball"}]
data modify storage grindless:renewable_check move.Count set value 2b
data modify storage grindless:renewable_check move.Slot set value 0b
data modify block 16 280 8 Items append from storage grindless:renewable_check move
execute store result score remaining renewable_check run data get block 9 280 8 Items[{id:"minecraft:clay_ball"}].Count
scoreboard players remove remaining renewable_check 2
execute store result block 9 280 8 Items[{id:"minecraft:clay_ball"}].Count byte 1 run scoreboard players get remaining renewable_check
data modify storage grindless:renewable_check move set from block 16 279 4 Items[{id:"minecraft:sand"}]
data modify storage grindless:renewable_check move.Count set value 1b
data modify storage grindless:renewable_check move.Slot set value 1b
data modify block 16 280 8 Items append from storage grindless:renewable_check move
data modify block 16 279 4 Items[{id:"minecraft:sand"}].Count set value 2b
