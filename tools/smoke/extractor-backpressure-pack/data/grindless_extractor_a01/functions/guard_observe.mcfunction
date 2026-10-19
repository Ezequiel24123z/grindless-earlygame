execute in grindless:luna if data block 20 80 4 {Energy:{FluxStored:8L}} run say A01-SINGLE-TICK-EXACTLY-8-FU
execute in grindless:luna store result score guard_progress extractor_a01 run data get block 20 80 4 Progress 1000
execute in grindless:luna if score guard_progress extractor_a01 matches 0..399999 if data block 20 80 4 {Output:[{id:"grindless:helium_3",Count:64b}]} run say A01-SINGLE-COMPLETION
execute in grindless:luna run data merge block 20 80 4 {Energy:{FluxStored:0L}}
execute in grindless:luna run data modify storage grindless:extractor_a01 recovered set from block 4 80 4
execute in grindless:luna run data modify storage grindless:extractor_a01 guarded set from block 20 80 4
execute in grindless:drift run data modify storage grindless:extractor_a01 no_vein set from block 4 80 4
say A01-FIXTURE-FINISHED
