scoreboard players set glass renewable_check 1
data modify storage grindless:renewable_check move set from block 16 279 4 Items[{id:"minecraft:sand"}]
data modify storage grindless:renewable_check move.Slot set value 0b
data modify block 24 280 10 Items append from storage grindless:renewable_check move
data remove block 16 279 4 Items[{id:"minecraft:sand"}]
data modify storage grindless:renewable_check move set from block 24 279 4 Items[{id:"minecraft:charcoal"}]
data modify storage grindless:renewable_check move.Count set value 2b
data modify storage grindless:renewable_check move.Slot set value 1b
data modify block 24 280 10 Items append from storage grindless:renewable_check move
execute store result score remaining renewable_check run data get block 24 279 4 Items[{id:"minecraft:charcoal"}].Count
scoreboard players remove remaining renewable_check 2
execute store result block 24 279 4 Items[{id:"minecraft:charcoal"}].Count byte 1 run scoreboard players get remaining renewable_check
