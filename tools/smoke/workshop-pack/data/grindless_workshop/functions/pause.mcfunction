say WORKSHOP-IN-PROGRESS
setblock 13 281 5 minecraft:air
data merge block 11 280 4 {Energy:{FluxStored:0L}}
schedule function grindless_workshop:restore 20t replace
