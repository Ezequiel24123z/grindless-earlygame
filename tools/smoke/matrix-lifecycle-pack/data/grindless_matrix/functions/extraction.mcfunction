execute if data block 8 69 8 {Items:[{id:"grindless:relay_matrix",Count:1b,tag:{Rating:2}}]} unless data block 8 70 8 Items[{}] run say MATRIX-HOPPER-OUT
setblock 8 69 8 minecraft:air
item replace block 8 70 8 container.0 with grindless:relay_matrix
item replace block 8 70 8 container.1 with grindless:machine_casing
item replace block 8 70 8 container.2 with minecraft:glass 4
item replace block 8 70 8 container.3 with grindless:flux_cell
item replace block 8 70 8 container.4 with minecraft:cobblestone 64
# The first cycle ran from zero at real power; only this blocked-output branch starts near completion.
data merge block 8 70 8 {Progress:799.0d}
schedule function grindless_matrix:blocked 20t replace
