scoreboard objectives add extractor_a01 dummy
scoreboard players set load_tries extractor_a01 0
scoreboard players set ready extractor_a01 0
execute in grindless:luna run forceload add 0 0 31 15
execute in grindless:drift run forceload add 0 0
schedule function grindless_extractor_a01:await_loaded 1t replace
