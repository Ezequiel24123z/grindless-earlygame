# Only an observed-empty hopper is removed. The clay already in the other hoppers remains.
setblock 8 281 8 minecraft:air
setblock 9 280 8 minecraft:chest
scoreboard players set phase renewable_check 5
say RENEWABLE-SURPLUS-DIVERTED-AFTER-STEADY-WINDOW
