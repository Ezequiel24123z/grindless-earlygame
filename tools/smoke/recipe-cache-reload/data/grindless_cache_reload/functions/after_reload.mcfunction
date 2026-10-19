data modify storage grindless:recipe_cache_reload awaiting_reload set value 0b
function grindless_cache_reload:tags
scoreboard players set tag_ok cache_reload 1
execute unless score phase cache_reload matches 2 run scoreboard players set tag_ok cache_reload 0
execute unless score witnesses cache_reload matches 3 run scoreboard players set tag_ok cache_reload 0
execute unless score raw cache_reload matches 1 run scoreboard players set tag_ok cache_reload 0
execute unless score crushed cache_reload matches 1 run scoreboard players set tag_ok cache_reload 0
execute unless score ingot cache_reload matches 1 run scoreboard players set tag_ok cache_reload 0
execute if score tag_ok cache_reload matches 0 run scoreboard players set error cache_reload 4
execute if score tag_ok cache_reload matches 0 run function grindless_cache_reload:failed
execute if score tag_ok cache_reload matches 1 run scoreboard players set reload_seen cache_reload 1
execute if score tag_ok cache_reload matches 1 run scoreboard players set phase cache_reload 3
execute if score tag_ok cache_reload matches 1 run say CACHE-RELOAD-INGOT-TAG-PUBLISHED
