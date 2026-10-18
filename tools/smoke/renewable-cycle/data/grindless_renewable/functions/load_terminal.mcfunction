scoreboard players set core renewable_check 2
scoreboard players set terminal renewable_check 1
say RENEWABLE-BENCH-1-UNCALIBRATED-CORE
data modify storage grindless:renewable_check move set from block 16 279 12 Items[{id:"grindless:data_core"}]
data modify storage grindless:renewable_check move.Slot set value 0b
data modify block 16 281 16 Items append from storage grindless:renewable_check move
data remove block 16 279 12 Items[{id:"grindless:data_core"}]
