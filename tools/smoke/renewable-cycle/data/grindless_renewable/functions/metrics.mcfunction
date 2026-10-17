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
execute store result score startup renewable_check run data get block 9 280 8 Energy.FluxStored
execute store result score n renewable_check run data get block 8 280 5 Energy.FluxStored
scoreboard players operation startup renewable_check += n renewable_check
scoreboard players operation stored renewable_check += startup renewable_check
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
scoreboard players set balance renewable_check 3840
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
