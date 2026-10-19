scoreboard players set valid cache_reload 1
scoreboard players set done cache_reload 0
execute unless loaded 8 280 8 run scoreboard players set valid cache_reload 0
execute unless block 8 280 8 grindless:pulverizer run scoreboard players set valid cache_reload 0
execute unless block 7 280 8 grindless:thermal_generator run scoreboard players set valid cache_reload 0
execute unless data block 8 280 8 {ItemLayout:3,FluidOutputQuota:0} run scoreboard players set valid cache_reload 0
execute unless data block 7 280 8 {Burn:0,Duration:0} run scoreboard players set valid cache_reload 0
execute if data block 7 280 8 Fuel[{}] run scoreboard players set valid cache_reload 0
execute store success score read_ok cache_reload store result score progress cache_reload run data get block 8 280 8 Progress 32
execute unless score read_ok cache_reload matches 1 run scoreboard players set valid cache_reload 0
execute store success score read_ok cache_reload store result score energy cache_reload run data get block 8 280 8 Energy.FluxStored
execute unless score read_ok cache_reload matches 1 run scoreboard players set valid cache_reload 0
execute store success score read_ok cache_reload store result score donor cache_reload run data get block 7 280 8 Energy.FluxStored
execute unless score read_ok cache_reload matches 1 run scoreboard players set valid cache_reload 0
execute store success score read_ok cache_reload store result score stacks cache_reload run data get block 8 280 8 Items
execute unless score read_ok cache_reload matches 1 run scoreboard players set valid cache_reload 0
execute unless score stacks cache_reload matches 1 run scoreboard players set valid cache_reload 0
execute unless data block 8 280 8 {Items:[{Slot:0b,id:"minecraft:gravel",Count:1b}]} unless data block 8 280 8 {Items:[{Slot:1b,id:"minecraft:sand",Count:2b}]} run scoreboard players set valid cache_reload 0
execute if data block 8 280 8 {Items:[{Slot:1b,id:"minecraft:sand",Count:2b}]} run scoreboard players set done cache_reload 1
execute unless score energy cache_reload matches 0..1280 run scoreboard players set valid cache_reload 0
execute unless score donor cache_reload matches 0..6400 run scoreboard players set valid cache_reload 0
execute unless score progress cache_reload matches 0..3808 run scoreboard players set valid cache_reload 0
execute if score done cache_reload matches 1 unless data block 8 280 8 {Progress:0.0d} run scoreboard players set valid cache_reload 0
scoreboard players operation paid cache_reload = progress cache_reload
execute if score done cache_reload matches 1 run scoreboard players add paid cache_reload 3840
scoreboard players operation total cache_reload = energy cache_reload
scoreboard players operation total cache_reload += donor cache_reload
scoreboard players set expected cache_reload 7680
scoreboard players operation expected cache_reload -= paid cache_reload
execute unless score total cache_reload = expected cache_reload run scoreboard players set valid cache_reload 0
