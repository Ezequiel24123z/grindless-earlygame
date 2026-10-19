scoreboard players add ready_tries cache_reload 1
execute if loaded 8 280 8 run function grindless_cache_reload:setup
execute if score phase cache_reload matches 0 if score ready_tries cache_reload matches ..40 run schedule function grindless_cache_reload:ready 5t replace
execute if score phase cache_reload matches 0 if score ready_tries cache_reload matches 41.. run scoreboard players set error cache_reload 1
execute if score phase cache_reload matches 0 if score ready_tries cache_reload matches 41.. run function grindless_cache_reload:failed
