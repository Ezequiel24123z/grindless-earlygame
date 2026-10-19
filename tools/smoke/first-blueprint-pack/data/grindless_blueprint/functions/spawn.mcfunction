scoreboard players set phase blueprint_check 1
# Isolate item registration/spawning from incidental terrain, motion, damage and pickup.
fill 7 99 7 9 101 9 minecraft:air
execute store success score tool_spawn blueprint_check run summon minecraft:item 8 100 8 {Tags:["grindless_blueprint_smoke"],NoGravity:1b,Invulnerable:1b,PickupDelay:32767s,Motion:[0.0d,0.0d,0.0d],Item:{id:"grindless:blueprint_tool",Count:1b}}
