execute if data block 8 70 8 {Items:[{Slot:4b,id:"grindless:relay_matrix",Count:1b,tag:{Rating:2}}]} unless data block 8 70 8 Items[{Slot:0b}] unless data block 8 70 8 Items[{Slot:1b}] unless data block 8 70 8 Items[{Slot:2b}] unless data block 8 70 8 Items[{Slot:3b}] run say MATRIX-CYCLE-OK
data modify storage grindless:matrix_check output set from block 8 70 8 Items
data modify block 8 70 8 Items set value []
setblock 8 70 8 minecraft:air
setblock 8 70 8 grindless:assembler[facing=north,status=idle]
data modify block 8 70 8 Items set from storage grindless:matrix_check output
execute if data block 8 70 8 {Items:[{Slot:4b,id:"grindless:relay_matrix",Count:1b,tag:{Rating:2}}]} unless data block 8 70 8 Items[{Slot:0b}] run say MATRIX-OUTPUT-RESTORED
setblock 8 69 8 minecraft:hopper
schedule function grindless_matrix:extraction 20t replace
