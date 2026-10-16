execute store result score before matrix_check run data get block 8 70 8 Progress 1
execute if score before matrix_check matches 1..799 run say MATRIX-IN-PROGRESS
data modify storage grindless:matrix_check saved set from block 8 70 8
data modify block 8 70 8 Items set value []
setblock 8 70 8 minecraft:air
setblock 8 70 8 grindless:assembler[facing=north,status=idle]
data modify block 8 70 8 Items set from storage grindless:matrix_check saved.Items
data modify block 8 70 8 Progress set from storage grindless:matrix_check saved.Progress
data modify block 8 70 8 Energy set from storage grindless:matrix_check saved.Energy
execute store result score after matrix_check run data get block 8 70 8 Progress 1
execute if score before matrix_check = after matrix_check if data block 8 70 8 {Recipe:"assemble/relay_matrix_f2",Items:[{Slot:0b,id:"grindless:relay_matrix",Count:1b},{Slot:1b,id:"grindless:machine_casing",Count:1b},{Slot:2b,id:"minecraft:glass",Count:4b},{Slot:3b,id:"grindless:flux_cell",Count:1b}]} run say MATRIX-PROGRESS-RESTORED
schedule function grindless_matrix:finish 900t replace
