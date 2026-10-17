execute store result score paused settling_check run data get block 4 280 3 Progress 100
execute if score before settling_check = paused settling_check if block 4 280 3 grindless:settling_trough[status=out_of_band] run say TROUGH-CLEARANCE-PAUSED
execute if data block 4 280 3 {Items:[{Slot:0b,id:"minecraft:dirt",Count:2b}],Fluid:{Id:"minecraft:water",Amount:1000}} run say TROUGH-INPUTS-KEPT
execute store result score energy_after settling_check run data get block 4 280 3 Energy.FluxStored 1
execute if score energy_before settling_check = energy_after settling_check run say TROUGH-ENERGY-KEPT
data modify storage grindless:settling_check saved set from block 4 280 3
data modify block 4 280 3 Items set value []
setblock 4 280 3 minecraft:air
setblock 4 281 4 minecraft:air
setblock 4 280 3 grindless:settling_trough[facing=north,status=idle]
data modify block 4 280 3 Items set from storage grindless:settling_check saved.Items
data modify block 4 280 3 WorkRecipe set from storage grindless:settling_check saved.WorkRecipe
data modify block 4 280 3 Fluid set from storage grindless:settling_check saved.Fluid
data modify block 4 280 3 Energy set from storage grindless:settling_check saved.Energy
data modify block 4 280 3 Progress set from storage grindless:settling_check saved.Progress
execute store result score restored settling_check run data get block 4 280 3 Progress 100
execute if score before settling_check = restored settling_check if data block 4 280 3 {ItemLayout:3,WorkRecipe:"bootstrap/settle_clay",Fluid:{Id:"minecraft:water",Amount:1000}} run say TROUGH-RECONSTRUCTED
data merge block 3 280 3 {Energy:{FluxStored:800L}}
schedule function grindless_settling:products 240t replace
