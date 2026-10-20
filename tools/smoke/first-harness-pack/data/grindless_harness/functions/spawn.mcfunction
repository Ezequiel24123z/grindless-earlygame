scoreboard players set phase harness_check 1
# Match the existing exosuit fixture: one owned item per ID, without terrain or pickup hazards.
fill 7 99 7 9 101 9 minecraft:air
execute store success score helmet_spawn harness_check run summon minecraft:item 8 100 8 {Tags:["grindless_harness_smoke"],NoGravity:1b,Invulnerable:1b,PickupDelay:32767s,Motion:[0.0d,0.0d,0.0d],Item:{id:"grindless:voltaic_helmet",Count:1b}}
execute store success score chest_spawn harness_check run summon minecraft:item 8 100 8 {Tags:["grindless_harness_smoke"],NoGravity:1b,Invulnerable:1b,PickupDelay:32767s,Motion:[0.0d,0.0d,0.0d],Item:{id:"grindless:voltaic_chestplate",Count:1b}}
execute store success score legs_spawn harness_check run summon minecraft:item 8 100 8 {Tags:["grindless_harness_smoke"],NoGravity:1b,Invulnerable:1b,PickupDelay:32767s,Motion:[0.0d,0.0d,0.0d],Item:{id:"grindless:voltaic_leggings",Count:1b}}
execute store success score boots_spawn harness_check run summon minecraft:item 8 100 8 {Tags:["grindless_harness_smoke"],NoGravity:1b,Invulnerable:1b,PickupDelay:32767s,Motion:[0.0d,0.0d,0.0d],Item:{id:"grindless:voltaic_boots",Count:1b}}
execute store success score cell_spawn harness_check run summon minecraft:item 8 100 8 {Tags:["grindless_harness_smoke"],NoGravity:1b,Invulnerable:1b,PickupDelay:32767s,Motion:[0.0d,0.0d,0.0d],Item:{id:"grindless:flux_cell",Count:1b}}
