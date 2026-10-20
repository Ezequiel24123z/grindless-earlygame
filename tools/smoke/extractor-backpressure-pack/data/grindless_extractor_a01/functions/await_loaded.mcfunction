scoreboard players add load_tries extractor_a01 1
execute in grindless:luna if loaded 4 80 4 if loaded 20 80 4 in grindless:drift if loaded 4 80 4 in grindless:luna run function grindless_extractor_a01:setup
execute unless score ready extractor_a01 matches 1 if score load_tries extractor_a01 matches ..79 run schedule function grindless_extractor_a01:await_loaded 1t replace
execute unless score ready extractor_a01 matches 1 if score load_tries extractor_a01 matches 80.. run say A01-FAILED-CHUNKS-NOT-LOADED
