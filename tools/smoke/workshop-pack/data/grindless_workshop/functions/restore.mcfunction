execute store result score paused workshop_check run data get block 12 280 4 Progress 100
execute if score before workshop_check = paused workshop_check if block 12 280 4 grindless:firing_chamber[status=out_of_band] run say WORKSHOP-STRUCTURE-PAUSED
execute if data block 12 280 4 {Items:[{Slot:0b,id:"grindless:unfired_ceramic",Count:3b},{Slot:1b,id:"minecraft:charcoal",Count:1b}]} run say WORKSHOP-FUEL-KEPT
data modify storage grindless:workshop_check saved set from block 12 280 4
data modify block 12 280 4 Items set value []
setblock 12 280 4 minecraft:air
setblock 13 281 5 minecraft:bricks
setblock 12 280 4 grindless:firing_chamber[facing=north,status=idle]
data modify block 12 280 4 Items set from storage grindless:workshop_check saved.Items
data modify block 12 280 4 WorkRecipe set from storage grindless:workshop_check saved.WorkRecipe
data modify block 12 280 4 Energy set from storage grindless:workshop_check saved.Energy
data modify block 12 280 4 Progress set from storage grindless:workshop_check saved.Progress
execute store result score restored workshop_check run data get block 12 280 4 Progress 100
execute if score before workshop_check = restored workshop_check if data block 12 280 4 {WorkRecipe:"bootstrap/insulators",ItemLayout:3} run say WORKSHOP-WORK-RESTORED
data merge block 11 280 4 {Energy:{FluxStored:1600L}}
schedule function grindless_workshop:products 300t replace
