scoreboard players set raw cache_reload 0
scoreboard players set crushed cache_reload 0
scoreboard players set ingot cache_reload 0
execute store result score witnesses cache_reload run execute if entity @e[type=minecraft:armor_stand,tag=grindless_cache_reload_probe]
execute as @e[type=minecraft:armor_stand,tag=grindless_cache_reload_raw,limit=1] if predicate grindless_cache_reload:raw run scoreboard players set raw cache_reload 1
execute as @e[type=minecraft:armor_stand,tag=grindless_cache_reload_crushed,limit=1] if predicate grindless_cache_reload:crushed run scoreboard players set crushed cache_reload 1
execute as @e[type=minecraft:armor_stand,tag=grindless_cache_reload_ingot,limit=1] if predicate grindless_cache_reload:ingot run scoreboard players set ingot cache_reload 1
