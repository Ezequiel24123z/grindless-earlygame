fill 6 279 7 9 282 9 minecraft:air
setblock 7 280 8 grindless:thermal_generator[facing=east,status=idle]{Energy:{FluxStored:6400L},Fuel:[],Burn:0,Duration:0}
setblock 8 280 8 grindless:pulverizer[facing=north,status=idle]{ItemLayout:3,Items:[{Slot:0b,id:"minecraft:gravel",Count:1b}],Energy:{FluxStored:1280L}}
summon minecraft:armor_stand 10 280 10 {Tags:["grindless_cache_reload_probe","grindless_cache_reload_raw"],Marker:1b,Invisible:1b,Invulnerable:1b,NoGravity:1b,HandItems:[{id:"minecraft:gravel",Count:1b},{}]}
summon minecraft:armor_stand 11 280 10 {Tags:["grindless_cache_reload_probe","grindless_cache_reload_crushed"],Marker:1b,Invisible:1b,Invulnerable:1b,NoGravity:1b,HandItems:[{id:"minecraft:sand",Count:1b},{}]}
summon minecraft:armor_stand 12 280 10 {Tags:["grindless_cache_reload_probe","grindless_cache_reload_ingot"],Marker:1b,Invisible:1b,Invulnerable:1b,NoGravity:1b,HandItems:[{id:"minecraft:iron_ingot",Count:1b},{}]}
scoreboard players set phase cache_reload 1
schedule function grindless_cache_reload:poll 1t replace
