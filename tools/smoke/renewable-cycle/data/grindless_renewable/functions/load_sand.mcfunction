scoreboard players set milling renewable_check 1
say RENEWABLE-DRY-AGGREGATE-8-COBBLE-4-GRAVEL
data modify storage grindless:renewable_check move set from block 16 279 4 Items[{id:"minecraft:gravel"}]
data modify storage grindless:renewable_check move.Count set value 3b
data modify storage grindless:renewable_check move.Slot set value 0b
data modify block 16 280 4 Items append from storage grindless:renewable_check move
data modify block 16 279 4 Items[{id:"minecraft:gravel"}].Count set value 1b
