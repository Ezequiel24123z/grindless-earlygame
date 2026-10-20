execute if score phase cache_reload matches 1 run function grindless_cache_reload:idle
execute if score phase cache_reload matches 2..3 run function grindless_cache_reload:observe
execute if score phase cache_reload matches 4 run function grindless_cache_reload:settled
execute if score phase cache_reload matches 1..4 run schedule function grindless_cache_reload:poll 1t replace
