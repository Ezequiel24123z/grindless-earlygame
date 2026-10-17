scoreboard players add ready_tries settling_check 1
scoreboard players set ready settling_check 0
execute if block 4 280 3 grindless:settling_trough[status=out_of_band] if data block 4 280 3 {Progress:0.0d,Recipe:"bootstrap/settle_clay",Energy:{FluxStored:320L},Items:[{Slot:0b,id:"minecraft:dirt",Count:2b}],Fluid:{Id:"minecraft:water",Amount:1000}} run scoreboard players set ready settling_check 1
execute if score ready settling_check matches 1 run function grindless_settling:start
execute if score ready settling_check matches 0 if score ready_tries settling_check matches ..40 run schedule function grindless_settling:ready 10t replace
execute if score ready settling_check matches 0 if score ready_tries settling_check matches 41.. run say TROUGH-FAIL-READY-TIMEOUT
