scoreboard players set bricks renewable_check 1
data modify storage grindless:renewable_check move set from block 9 280 8 Items[{id:"minecraft:clay_ball"}]
data modify storage grindless:renewable_check move.Count set value 4b
data modify storage grindless:renewable_check move.Slot set value 0b
data modify block 20 280 10 Items append from storage grindless:renewable_check move
execute store result score remaining renewable_check run data get block 9 280 8 Items[{id:"minecraft:clay_ball"}].Count
scoreboard players remove remaining renewable_check 4
execute if score remaining renewable_check matches 0 run data remove block 9 280 8 Items[{id:"minecraft:clay_ball"}]
execute if score remaining renewable_check matches 1.. store result block 9 280 8 Items[{id:"minecraft:clay_ball"}].Count byte 1 run scoreboard players get remaining renewable_check
data modify storage grindless:renewable_check move set from block 24 279 4 Items[{id:"minecraft:charcoal"}]
data modify storage grindless:renewable_check move.Count set value 1b
data modify storage grindless:renewable_check move.Slot set value 1b
data modify block 20 280 10 Items append from storage grindless:renewable_check move
data remove block 24 279 4 Items[{id:"minecraft:charcoal"}]
