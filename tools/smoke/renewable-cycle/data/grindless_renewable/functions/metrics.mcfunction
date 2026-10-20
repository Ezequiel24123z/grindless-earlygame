# Read the native single-slot inventory; an unavailable read is not an empty slot.
scoreboard players set coal renewable_check -1
scoreboard players set fuel_valid renewable_check 0
scoreboard players set fuel_read renewable_check 0
scoreboard players set fuel_slots renewable_check -1
scoreboard players set fuel_count_read renewable_check 0
scoreboard players set fuel_count renewable_check -1
execute if block 8 280 7 grindless:biomass_dynamo store success score fuel_read renewable_check store result score fuel_slots renewable_check run data get block 8 280 7 Fuel
execute if score fuel_read renewable_check matches 1 if score fuel_slots renewable_check matches 0 run scoreboard players set coal renewable_check 0
execute if score fuel_read renewable_check matches 1 if score fuel_slots renewable_check matches 1 store success score fuel_count_read renewable_check store result score fuel_count renewable_check run data get block 8 280 7 Fuel[0].Count
execute if score fuel_count_read renewable_check matches 1 if data block 8 280 7 {Fuel:[{Slot:0b,id:"minecraft:coal",Count:1b}]} run scoreboard players operation coal renewable_check = fuel_count renewable_check
execute if score fuel_count_read renewable_check matches 1 if score fuel_count renewable_check matches 1..64 if data block 8 280 7 {Fuel:[{Slot:0b,id:"grindless:biomass"}]} run scoreboard players set coal renewable_check 0
execute if score fuel_read renewable_check matches 1 if score fuel_slots renewable_check matches 0..1 if score coal renewable_check matches 0..1 run scoreboard players set fuel_valid renewable_check 1
# Every clay ball witnesses one geological cycle. No clay or aggregate is seeded.
scoreboard players set geo renewable_check 0
scoreboard players set aggregate renewable_check 0
scoreboard players set n renewable_check 0
execute if data block 8 280 8 Items[{id:"minecraft:clay_ball"}] store result score n renewable_check run data get block 8 280 8 Items[{id:"minecraft:clay_ball"}].Count
scoreboard players operation geo renewable_check += n renewable_check
scoreboard players set n renewable_check 0
execute if data block 8 281 8 Items[{id:"minecraft:clay_ball"}] store result score n renewable_check run data get block 8 281 8 Items[{id:"minecraft:clay_ball"}].Count
scoreboard players operation geo renewable_check += n renewable_check
scoreboard players set n renewable_check 0
execute if data block 9 281 8 Items[{id:"minecraft:clay_ball"}] store result score n renewable_check run data get block 9 281 8 Items[{id:"minecraft:clay_ball"}].Count
scoreboard players operation geo renewable_check += n renewable_check
scoreboard players set n renewable_check 0
execute if data block 9 281 7 Items[{id:"minecraft:clay_ball"}] store result score n renewable_check run data get block 9 281 7 Items[{id:"minecraft:clay_ball"}].Count
scoreboard players operation geo renewable_check += n renewable_check
scoreboard players set n renewable_check 0
execute if data block 9 281 6 Items[{id:"minecraft:clay_ball"}] store result score n renewable_check run data get block 9 281 6 Items[{id:"minecraft:clay_ball"}].Count
scoreboard players operation geo renewable_check += n renewable_check
scoreboard players set n renewable_check 0
execute if data block 9 280 6 Items[{id:"minecraft:clay_ball"}] store result score n renewable_check run data get block 9 280 6 Items[{id:"minecraft:clay_ball"}].Count
scoreboard players operation geo renewable_check += n renewable_check
scoreboard players set n renewable_check 0
execute if data block 8 280 8 Items[{id:"grindless:mineral_aggregate"}] store result score n renewable_check run data get block 8 280 8 Items[{id:"grindless:mineral_aggregate"}].Count
scoreboard players operation aggregate renewable_check += n renewable_check
scoreboard players set n renewable_check 0
execute if data block 8 281 8 Items[{id:"grindless:mineral_aggregate"}] store result score n renewable_check run data get block 8 281 8 Items[{id:"grindless:mineral_aggregate"}].Count
scoreboard players operation aggregate renewable_check += n renewable_check
scoreboard players set n renewable_check 0
execute if data block 9 281 8 Items[{id:"grindless:mineral_aggregate"}] store result score n renewable_check run data get block 9 281 8 Items[{id:"grindless:mineral_aggregate"}].Count
scoreboard players operation aggregate renewable_check += n renewable_check
scoreboard players set n renewable_check 0
execute if data block 9 281 7 Items[{id:"grindless:mineral_aggregate"}] store result score n renewable_check run data get block 9 281 7 Items[{id:"grindless:mineral_aggregate"}].Count
scoreboard players operation aggregate renewable_check += n renewable_check
scoreboard players set n renewable_check 0
execute if data block 9 281 6 Items[{id:"grindless:mineral_aggregate"}] store result score n renewable_check run data get block 9 281 6 Items[{id:"grindless:mineral_aggregate"}].Count
scoreboard players operation aggregate renewable_check += n renewable_check
scoreboard players set n renewable_check 0
execute if data block 9 280 6 Items[{id:"grindless:mineral_aggregate"}] store result score n renewable_check run data get block 9 280 6 Items[{id:"grindless:mineral_aggregate"}].Count
scoreboard players operation aggregate renewable_check += n renewable_check
scoreboard players set n renewable_check 0
execute if data block 8 280 6 Items[{Slot:0b,id:"grindless:mineral_aggregate"}] store result score n renewable_check run data get block 8 280 6 Items[{Slot:0b,id:"grindless:mineral_aggregate"}].Count
scoreboard players operation aggregate renewable_check += n renewable_check
scoreboard players operation bio renewable_check = geo renewable_check
scoreboard players operation bio renewable_check -= aggregate renewable_check
scoreboard players set biomass renewable_check 0
execute if data block 8 280 7 Fuel[{id:"grindless:biomass"}] store result score biomass renewable_check run data get block 8 280 7 Fuel[{id:"grindless:biomass"}].Count
scoreboard players set n renewable_check 0
execute if data block 8 280 6 Items[{Slot:1b,id:"grindless:biomass"}] store result score n renewable_check run data get block 8 280 6 Items[{Slot:1b,id:"grindless:biomass"}].Count
scoreboard players operation biomass renewable_check += n renewable_check
execute store result score burn renewable_check run data get block 8 280 7 Burn
execute store result score geo_progress renewable_check run data get block 8 280 8 Progress 4
execute store result score bio_progress renewable_check run data get block 8 280 6 Progress 4
execute store result score stored renewable_check run data get block 8 280 7 Energy.FluxStored
execute store result score n renewable_check run data get block 8 280 8 Energy.FluxStored
scoreboard players operation stored renewable_check += n renewable_check
execute store result score n renewable_check run data get block 8 280 6 Energy.FluxStored
scoreboard players operation stored renewable_check += n renewable_check
scoreboard players set startup renewable_check 0
execute if data block 8 280 7 {Duration:1600} run scoreboard players operation startup renewable_check = burn renewable_check
scoreboard players operation paid renewable_check = geo renewable_check
scoreboard players operation paid renewable_check *= geo_cost renewable_check
scoreboard players operation n renewable_check = bio renewable_check
scoreboard players operation n renewable_check *= bio_cost renewable_check
scoreboard players operation paid renewable_check += n renewable_check
scoreboard players operation paid renewable_check += geo_progress renewable_check
scoreboard players operation paid renewable_check += bio_progress renewable_check
scoreboard players operation burnt renewable_check = bio renewable_check
scoreboard players operation burnt renewable_check *= four renewable_check
scoreboard players operation burnt renewable_check -= biomass renewable_check
scoreboard players operation generated renewable_check = burnt renewable_check
scoreboard players operation generated renewable_check *= fuel_value renewable_check
scoreboard players operation n renewable_check = burn renewable_check
scoreboard players operation n renewable_check *= eight renewable_check
scoreboard players operation generated renewable_check -= n renewable_check
scoreboard players operation coal_fu renewable_check = coal renewable_check
scoreboard players operation coal_fu renewable_check *= startup_value renewable_check
scoreboard players operation balance renewable_check = startup_value renewable_check
scoreboard players operation balance renewable_check -= coal_fu renewable_check
scoreboard players operation balance renewable_check += generated renewable_check
scoreboard players operation balance renewable_check -= paid renewable_check
scoreboard players operation balance renewable_check -= stored renewable_check
scoreboard players operation potential renewable_check = biomass renewable_check
scoreboard players operation potential renewable_check *= fuel_value renewable_check
scoreboard players operation potential renewable_check += n renewable_check
scoreboard players operation potential renewable_check += stored renewable_check
scoreboard players operation water renewable_check = geo renewable_check
scoreboard players operation water renewable_check += bio renewable_check
scoreboard players operation water renewable_check *= water_cost renewable_check
execute store result score now renewable_check run time query gametime
