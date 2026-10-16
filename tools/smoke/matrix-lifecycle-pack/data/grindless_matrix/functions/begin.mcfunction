scoreboard objectives add matrix_check dummy
setblock 8 69 8 minecraft:air
setblock 8 70 8 grindless:assembler[facing=north,status=idle]
item replace block 8 70 8 container.0 with grindless:relay_matrix{Rating:1}
item replace block 8 70 8 container.1 with grindless:machine_casing
item replace block 8 70 8 container.2 with minecraft:glass 4
item replace block 8 70 8 container.3 with grindless:flux_cell
execute if data block 8 70 8 {Recipe:"assemble/relay_matrix_f2"} run say MATRIX-F1-ACCEPTED
item replace block 8 70 8 container.0 with grindless:relay_matrix{Rating:2}
execute if data block 8 70 8 {Recipe:""} run say MATRIX-F2-INPUT-REJECTED
item replace block 8 70 8 container.0 with grindless:relay_matrix{Rating:1.5d}
execute if data block 8 70 8 {Recipe:""} run say MATRIX-FRACTION-REJECTED
item replace block 8 70 8 container.0 with grindless:relay_matrix{Rating:4294967297L}
execute if data block 8 70 8 {Recipe:""} run say MATRIX-OVERFLOW-REJECTED
item replace block 8 70 8 container.0 with grindless:relay_matrix{Rating:"1"}
execute if data block 8 70 8 {Recipe:""} run say MATRIX-STRING-REJECTED
item replace block 8 70 8 container.0 with grindless:relay_matrix{Rating:4}
execute if data block 8 70 8 {Recipe:""} run say MATRIX-ARCHITECTURE-REJECTED
item replace block 8 70 8 container.0 with grindless:relay_matrix
execute if data block 8 70 8 {Recipe:"assemble/relay_matrix_f2",Progress:0.0d} run say MATRIX-UNTAGGED-ACCEPTED
setblock 12 70 8 grindless:assembler{ItemLayout:1,Items:[{Slot:3b,id:"grindless:relay_matrix",Count:1b,tag:{Rating:2}}]}
execute if data block 12 70 8 {ItemLayout:3,Items:[{Slot:4b,id:"grindless:relay_matrix",Count:1b,tag:{Rating:2}}]} run say MATRIX-LEGACY-SLOT-MIGRATED
setblock 14 70 8 grindless:assembler{ItemLayout:2,Items:[{id:"grindless:relay_matrix",Count:1b,tag:{Rating:2}}]}
execute if data block 14 70 8 {Items:[{Slot:0b,id:"grindless:relay_matrix",Count:1b,tag:{Rating:2}}]} run say MATRIX-LEGACY-COMPACT-KEPT
setblock 7 70 8 grindless:thermal_generator[facing=east,status=idle]
item replace block 7 70 8 container.0 with minecraft:charcoal 4
schedule function grindless_matrix:checkpoint 100t replace
