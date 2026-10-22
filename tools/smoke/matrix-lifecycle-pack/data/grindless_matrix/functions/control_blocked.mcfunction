# The manual Matrix binding is checked by grindless_matrix_input_checks; no Matrix is added to this tool bill.
execute store result score control matrix_check run data get block 8 76 8 Progress 1
execute if score control matrix_check matches 400.. if data block 8 76 8 {SelectedRecipe:"assemble/blueprint_tool",Recipe:"assemble/blueprint_tool",Items:[{Slot:0b,id:"grindless:machine_casing",Count:2b},{Slot:1b,id:"grindless:motor",Count:4b},{Slot:2b,id:"grindless:iron_plate",Count:8b},{Slot:4b,id:"minecraft:cobblestone",Count:64b}]} unless data block 8 76 8 Items[{Slot:3b}] run say MATRIX-ASSEMBLER-TOOL-BLOCKED
item replace block 8 76 8 container.4 with minecraft:air
schedule function grindless_matrix:control_finish 20t replace
