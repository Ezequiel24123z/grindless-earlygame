execute store result score control matrix_check run data get block 8 76 8 Progress 1
execute if score control matrix_check matches 400.. if data block 8 76 8 {Recipe:"assemble/wire_mill",Items:[{Slot:0b,id:"grindless:machine_casing",Count:2b},{Slot:1b,id:"grindless:copper_coil",Count:4b},{Slot:2b,id:"grindless:iron_plate",Count:8b},{Slot:3b,id:"grindless:relay_matrix",Count:2b,tag:{Rating:3}},{Slot:4b,id:"minecraft:cobblestone",Count:64b}]} run say MATRIX-CONTROL-BLOCKED
item replace block 8 76 8 container.4 with minecraft:air
schedule function grindless_matrix:control_finish 20t replace
