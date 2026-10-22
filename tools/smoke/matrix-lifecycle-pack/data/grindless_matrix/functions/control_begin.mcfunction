setblock 8 76 8 grindless:assembler[facing=north,status=idle]
item replace block 8 76 8 container.0 with grindless:machine_casing 2
item replace block 8 76 8 container.1 with grindless:copper_coil 4
item replace block 8 76 8 container.2 with grindless:iron_plate 8
item replace block 8 76 8 container.3 with grindless:relay_matrix{Rating:3} 2
item replace block 8 76 8 container.4 with minecraft:cobblestone 64
setblock 7 76 8 grindless:thermal_generator[facing=east,status=idle]
item replace block 7 76 8 container.0 with minecraft:charcoal 4
schedule function grindless_matrix:control_blocked 500t replace
