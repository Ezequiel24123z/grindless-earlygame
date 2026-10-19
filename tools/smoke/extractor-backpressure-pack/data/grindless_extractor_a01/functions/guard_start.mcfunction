execute in grindless:luna store result score stable_slots extractor_a01 run data get block 5 80 4 Items
execute in grindless:luna if score stable_slots extractor_a01 matches 2 if data block 4 80 4 {Progress:0.0d,Output:[],Energy:{FluxStored:0L}} if data block 5 80 4 {Items:[{Slot:0b,id:"grindless:helium_3",Count:64b},{Slot:1b,id:"grindless:helium_3",Count:1b}]} run say A01-RECOVERY-REMAINS-STABLE
# A scheduled function runs before block-entity ticks. The next scheduled observation is one tick later.
# Completing against the last free internal space cancels/re-adds working through inventory listeners.
execute in grindless:luna run data merge block 20 80 4 {Progress:399.9999d,Output:[{id:"grindless:helium_3",Count:63b}],Energy:{FluxStored:16L}}
schedule function grindless_extractor_a01:guard_observe 1t replace
