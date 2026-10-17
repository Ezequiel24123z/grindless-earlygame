execute store result score before workshop_check run data get block 12 280 4 Progress 100
execute if score before workshop_check matches 100..19999 run say WORKSHOP-IN-PROGRESS
setblock 13 281 5 minecraft:air
data merge block 11 280 4 {Energy:{FluxStored:0L}}
schedule function grindless_workshop:restore 20t replace
