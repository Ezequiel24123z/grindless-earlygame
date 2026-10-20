execute if data block 4 280 1 {Fluid:{Id:"minecraft:water",Amount:750}} unless data block 4 280 3 Fluid.Id run say TROUGH-WATER-RECOVERED
execute if block 4 280 3 grindless:settling_trough[status=idle] run say TROUGH-IDLE-AFTER-DRAIN
say TROUGH-COMPLETE
