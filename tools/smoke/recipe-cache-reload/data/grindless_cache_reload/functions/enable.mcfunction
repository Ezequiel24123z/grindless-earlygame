say CACHE-RELOAD-IDLE-MISS-RAW-CRUSHED-VALID
scoreboard players set phase cache_reload 2
scoreboard players set prev_paid cache_reload 0
execute store result score prev_time cache_reload run time query gametime
data modify storage grindless:recipe_cache_reload awaiting_reload set value 1b
execute store success score enable_ok cache_reload run datapack enable "file/recipe-cache-reload-optional" last
execute unless score enable_ok cache_reload matches 1 run scoreboard players set error cache_reload 3
execute unless score enable_ok cache_reload matches 1 run function grindless_cache_reload:failed
