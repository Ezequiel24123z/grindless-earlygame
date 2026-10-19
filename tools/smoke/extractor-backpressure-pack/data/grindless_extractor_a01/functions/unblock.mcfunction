execute in grindless:luna if block 4 80 4 grindless:crude_extractor[status=blocked] if data block 4 80 4 {Progress:400.0d,Output:[{id:"grindless:helium_3",Count:64b}],Energy:{FluxStored:0L}} run say A01-LOADED-STILL-BLOCKED
# Empty the same chest's inventory: no block replacement, hopper, power or neighbour notification.
execute in grindless:luna run data modify block 5 80 4 Items set value []
# Free only a filler slot; retain all helium already received by the no-vein destination.
execute in grindless:drift run item replace block 5 80 4 container.1 with minecraft:air
schedule function grindless_extractor_a01:recovered 16t replace
