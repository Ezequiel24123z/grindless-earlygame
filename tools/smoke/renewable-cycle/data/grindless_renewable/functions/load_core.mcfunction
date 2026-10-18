scoreboard players set core renewable_check 1
data modify storage grindless:renewable_check move set from block 20 279 10 Items[{id:"grindless:ceramic_insulator"}]
data modify storage grindless:renewable_check move.Count set value 1b
data modify storage grindless:renewable_check move.Slot set value 0b
data modify block 16 280 12 Items append from storage grindless:renewable_check move
data modify block 20 279 10 Items[{id:"grindless:ceramic_insulator"}].Count set value 1b
data modify storage grindless:renewable_check move set from block 20 279 4 Items[{id:"minecraft:redstone"}]
data modify storage grindless:renewable_check move.Count set value 1b
data modify storage grindless:renewable_check move.Slot set value 1b
data modify block 16 280 12 Items append from storage grindless:renewable_check move
data modify block 20 279 4 Items[{id:"minecraft:redstone"}].Count set value 3b
