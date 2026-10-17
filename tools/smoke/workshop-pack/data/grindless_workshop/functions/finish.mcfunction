execute if data block 8 279 4 Items[{id:"grindless:unfired_ceramic",Count:6b}] run say WORKSHOP-GROG-SINK
execute if data block 5 280 12 Items[{id:"grindless:ceramic_grog",Count:1b}] unless data block 4 280 12 Items[{}] run say WORKSHOP-IDLE-EXPORT
execute if block 4 280 12 grindless:quern[status=idle] if block 12 280 4 grindless:firing_chamber[status=idle] run say WORKSHOP-STABLE-IDLE
say WORKSHOP-COMPLETE
