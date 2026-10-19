execute in grindless:luna store result score recovered_slots extractor_a01 run data get block 5 80 4 Items
execute in grindless:luna if score recovered_slots extractor_a01 matches 2 if data block 4 80 4 {Progress:0.0d,Output:[],Energy:{FluxStored:0L}} if data block 5 80 4 {Items:[{Slot:0b,id:"grindless:helium_3",Count:64b},{Slot:1b,id:"grindless:helium_3",Count:1b}]} run say A01-RECOVERED-EXACTLY-65
execute in grindless:drift if data block 4 80 4 {Progress:123.5d,Output:[],Energy:{FluxStored:16L}} if data block 5 80 4 {Items:[{Slot:0b,id:"grindless:helium_3",Count:64b},{Slot:1b,id:"grindless:helium_3",Count:63b}]} run say A01-DRAINED-WITHOUT-VEIN-EXACTLY-127
schedule function grindless_extractor_a01:guard_start 16t replace
