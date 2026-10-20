execute if score phase cache_reload matches 2 run scoreboard players add publish_ticks cache_reload 1
execute if score phase cache_reload matches 3 if score reload_seen cache_reload matches 1 run scoreboard players add run_ticks cache_reload 1
function grindless_cache_reload:sample
execute unless data block 8 280 8 {Recipe:""} unless data block 8 280 8 {Recipe:"b1/cache_probe"} run scoreboard players set valid cache_reload 0
execute unless data block 8 280 8 {WorkRecipe:""} unless data block 8 280 8 {WorkRecipe:"b1/cache_probe"} run scoreboard players set valid cache_reload 0
execute if score paid cache_reload matches 1.. unless data block 8 280 8 {WorkRecipe:"b1/cache_probe"} run scoreboard players set valid cache_reload 0
execute if score done cache_reload matches 0 if score progress cache_reload matches 1.. unless data block 8 280 8 {Recipe:"b1/cache_probe"} run scoreboard players set valid cache_reload 0
execute store result score now cache_reload run time query gametime
scoreboard players operation time_step cache_reload = now cache_reload
scoreboard players operation time_step cache_reload -= prev_time cache_reload
scoreboard players operation step cache_reload = paid cache_reload
scoreboard players operation step cache_reload -= prev_paid cache_reload
scoreboard players set limit cache_reload 32
scoreboard players operation limit cache_reload *= time_step cache_reload
execute if score time_step cache_reload matches ..-1 run scoreboard players set valid cache_reload 0
execute if score step cache_reload matches ..-1 run scoreboard players set valid cache_reload 0
execute if score step cache_reload > limit cache_reload run scoreboard players set valid cache_reload 0
execute if score valid cache_reload matches 0 run scoreboard players set error cache_reload 5
execute if score valid cache_reload matches 0 run function grindless_cache_reload:failed
execute if score valid cache_reload matches 1 if score woke cache_reload matches 0 if score done cache_reload matches 0 if score progress cache_reload matches 1.. run say CACHE-RELOAD-IDLE-WOKE-B1
execute if score valid cache_reload matches 1 if score done cache_reload matches 0 if score progress cache_reload matches 1.. run scoreboard players set woke cache_reload 1
scoreboard players operation prev_paid cache_reload = paid cache_reload
scoreboard players operation prev_time cache_reload = now cache_reload
execute if score phase cache_reload matches 3 if score done cache_reload matches 1 if score woke cache_reload matches 1 if score reload_seen cache_reload matches 1 run function grindless_cache_reload:completed
execute if score phase cache_reload matches 2 if score publish_ticks cache_reload matches 400.. run scoreboard players set error cache_reload 8
execute if score phase cache_reload matches 2 if score publish_ticks cache_reload matches 400.. run function grindless_cache_reload:failed
execute if score phase cache_reload matches 3 if score reload_seen cache_reload matches 1 if score run_ticks cache_reload matches 400.. run scoreboard players set error cache_reload 6
execute if score phase cache_reload matches 3 if score reload_seen cache_reload matches 1 if score run_ticks cache_reload matches 400.. run function grindless_cache_reload:failed
