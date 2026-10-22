# ADR-0188: this legacy helper path now probes permitted tool backpressure, not automatic machine control.
setblock 8 76 8 grindless:assembler[facing=north,status=idle]
data merge block 8 76 8 {SelectedRecipe:"assemble/blueprint_tool"}
item replace block 8 76 8 container.0 with grindless:machine_casing 2
item replace block 8 76 8 container.1 with grindless:motor 4
item replace block 8 76 8 container.2 with grindless:iron_plate 8
item replace block 8 76 8 container.3 with minecraft:air
item replace block 8 76 8 container.4 with minecraft:cobblestone 64
setblock 7 76 8 grindless:thermal_generator[facing=east,status=idle]
item replace block 7 76 8 container.0 with minecraft:charcoal 4
schedule function grindless_matrix:control_blocked 500t replace
