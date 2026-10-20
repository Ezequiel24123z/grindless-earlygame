function grindless_cache_reload:sample
function grindless_cache_reload:tags
execute unless score witnesses cache_reload matches 3 run scoreboard players set valid cache_reload 0
execute unless score raw cache_reload matches 1 run scoreboard players set valid cache_reload 0
execute unless score crushed cache_reload matches 1 run scoreboard players set valid cache_reload 0
execute unless score ingot cache_reload matches 0 run scoreboard players set valid cache_reload 0
execute unless data block 8 280 8 {Progress:0.0d,Recipe:"",WorkRecipe:"",Energy:{FluxStored:1280L},Items:[{Slot:0b,id:"minecraft:gravel",Count:1b}]} run scoreboard players set valid cache_reload 0
execute unless score donor cache_reload matches 6400 run scoreboard players set valid cache_reload 0
execute if score valid cache_reload matches 0 run scoreboard players set error cache_reload 2
execute if score valid cache_reload matches 0 run function grindless_cache_reload:failed
execute if score phase cache_reload matches 1 run scoreboard players add idle_ticks cache_reload 1
execute if score phase cache_reload matches 1 if score idle_ticks cache_reload matches 40 run function grindless_cache_reload:enable
