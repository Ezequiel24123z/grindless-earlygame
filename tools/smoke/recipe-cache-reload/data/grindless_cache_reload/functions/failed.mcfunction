scoreboard players set phase cache_reload -1
data modify storage grindless:recipe_cache_reload awaiting_reload set value 0b
execute if score error cache_reload matches 1 run say CACHE-RELOAD-FAIL-CHUNK-NOT-LOADED
execute if score error cache_reload matches 2 run say CACHE-RELOAD-FAIL-IDLE-BASELINE-OR-TAGS
execute if score error cache_reload matches 3 run say CACHE-RELOAD-FAIL-OPTIONAL-PACK-NOT-STAGED-OR-NOT-DISABLED
execute if score error cache_reload matches 4 run say CACHE-RELOAD-FAIL-RELOAD-TAG-BARRIER
execute if score error cache_reload matches 5 run say CACHE-RELOAD-FAIL-IDENTITY-BALANCE-OR-TICK-BOUND
execute if score error cache_reload matches 6 run say CACHE-RELOAD-FAIL-IDLE-WAKE-OR-CYCLE-TIMEOUT
execute if score error cache_reload matches 7 run say CACHE-RELOAD-FAIL-DUPLICATE-WORK-OR-OUTPUT
execute if score error cache_reload matches 8 run say CACHE-RELOAD-FAIL-PUBLICATION-TIMEOUT
