say TROUGH-IN-PROGRESS
setblock 4 281 4 minecraft:stone
data merge block 3 280 3 {Energy:{FluxStored:0L}}
execute store result score energy_before settling_check run data get block 4 280 3 Energy.FluxStored 1
schedule function grindless_settling:restore 20t replace
