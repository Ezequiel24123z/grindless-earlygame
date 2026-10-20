function grindless_cache_reload:sample
execute unless score done cache_reload matches 1 run scoreboard players set valid cache_reload 0
execute unless score paid cache_reload matches 3840 run scoreboard players set valid cache_reload 0
execute unless score total cache_reload matches 3840 run scoreboard players set valid cache_reload 0
execute if score valid cache_reload matches 0 run scoreboard players set error cache_reload 7
execute if score valid cache_reload matches 0 run function grindless_cache_reload:failed
execute if score phase cache_reload matches 4 run scoreboard players add settled_ticks cache_reload 1
execute if score phase cache_reload matches 4 if score settled_ticks cache_reload matches 41 run say CACHE-RELOAD-NO-DUPLICATE-WORK
execute if score phase cache_reload matches 4 if score settled_ticks cache_reload matches 41 run scoreboard players set phase cache_reload 5
