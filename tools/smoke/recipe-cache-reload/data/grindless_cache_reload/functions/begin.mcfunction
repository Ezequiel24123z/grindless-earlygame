scoreboard objectives add cache_reload dummy
scoreboard players set phase cache_reload 0
scoreboard players set ready_tries cache_reload 0
scoreboard players set idle_ticks cache_reload 0
scoreboard players set run_ticks cache_reload 0
scoreboard players set settled_ticks cache_reload 0
scoreboard players set reload_seen cache_reload 0
scoreboard players set woke cache_reload 0
scoreboard players set error cache_reload 0
data modify storage grindless:recipe_cache_reload awaiting_reload set value 0b
forceload add 0 0
schedule function grindless_cache_reload:ready 1t replace
