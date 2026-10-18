scoreboard players set phase exosuit_check 1
# Isolate item registration/spawning from incidental terrain, motion, damage and pickup.
fill 7 99 7 9 101 9 minecraft:air
execute store success score helmet_spawn exosuit_check run summon minecraft:item 8 100 8 {Tags:["grindless_exosuit_smoke"],NoGravity:1b,Invulnerable:1b,PickupDelay:32767s,Motion:[0.0d,0.0d,0.0d],Item:{id:"grindless:flux_exosuit_helmet",Count:1b}}
execute store success score chest_spawn exosuit_check run summon minecraft:item 8 100 8 {Tags:["grindless_exosuit_smoke"],NoGravity:1b,Invulnerable:1b,PickupDelay:32767s,Motion:[0.0d,0.0d,0.0d],Item:{id:"grindless:flux_exosuit_chestplate",Count:1b}}
execute store success score tap_spawn exosuit_check run summon minecraft:item 8 100 8 {Tags:["grindless_exosuit_smoke"],NoGravity:1b,Invulnerable:1b,PickupDelay:32767s,Motion:[0.0d,0.0d,0.0d],Item:{id:"grindless:network_tap",Count:1b}}
execute store success score legs_spawn exosuit_check run summon minecraft:item 8 100 8 {Tags:["grindless_exosuit_smoke"],NoGravity:1b,Invulnerable:1b,PickupDelay:32767s,Motion:[0.0d,0.0d,0.0d],Item:{id:"grindless:exoskeleton_legs",Count:1b}}
