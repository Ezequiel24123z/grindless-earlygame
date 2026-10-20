execute in grindless:luna if block 4 80 4 grindless:crude_extractor[status=blocked] if data block 4 80 4 {Progress:400.0d,Output:[{id:"grindless:helium_3",Count:64b}],Energy:{FluxStored:0L}} run say A01-BLOCKED-PAID-WORK
execute in grindless:drift if data block 4 80 4 {Progress:123.5d,Output:[{id:"grindless:helium_3",Count:63b}],Energy:{FluxStored:16L}} if data block 5 80 4 {Items:[{Slot:0b,id:"grindless:helium_3",Count:64b}]} run say A01-PARTIAL-EXPORT-WITHOUT-VEIN
execute in grindless:luna run data modify storage grindless:extractor_a01 saved set from block 4 80 4
# Retain the only owned output in storage before replacing this diagnostic block entity.
execute in grindless:luna run data modify block 4 80 4 Output set value []
execute in grindless:luna run setblock 4 80 4 minecraft:air
execute in grindless:luna run setblock 4 80 4 grindless:crude_extractor[facing=east,status=idle]
execute in grindless:luna run data modify block 4 80 4 Progress set from storage grindless:extractor_a01 saved.Progress
execute in grindless:luna run data modify block 4 80 4 Output set from storage grindless:extractor_a01 saved.Output
execute in grindless:luna run data modify block 4 80 4 Energy set from storage grindless:extractor_a01 saved.Energy
execute in grindless:luna if data block 4 80 4 {Progress:400.0d,Output:[{id:"grindless:helium_3",Count:64b}],Energy:{FluxStored:0L}} run say A01-LOADED-OUTPUT-RETAINED
schedule function grindless_extractor_a01:unblock 16t replace
