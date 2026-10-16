execute store result score blocked matrix_check run data get block 8 70 8 Progress 1
execute if score blocked matrix_check matches 800.. if data block 8 70 8 {Items:[{Slot:0b,id:"grindless:relay_matrix",Count:1b},{Slot:1b,id:"grindless:machine_casing",Count:1b},{Slot:2b,id:"minecraft:glass",Count:4b},{Slot:3b,id:"grindless:flux_cell",Count:1b},{Slot:4b,id:"minecraft:cobblestone",Count:64b}]} run say MATRIX-BLOCKED-OK
item replace block 8 70 8 container.4 with minecraft:air
schedule function grindless_matrix:recovered 20t replace
