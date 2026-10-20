# Explicit native regression entry point, never called by load/begin.
# Synthetic stocks exercise the oracle, not renewable production or the 24-marker scenario.
schedule clear grindless_renewable:begin
function grindless_renewable:begin
schedule clear grindless_renewable:poll
scoreboard players set oracle_ok renewable_check 1

# The first observation may precede ignition. It must stay in phase 1 with exact balance.
function grindless_renewable:poll
schedule clear grindless_renewable:poll
execute unless score coal renewable_check matches 1 run scoreboard players set oracle_ok renewable_check 0
execute unless score fuel_valid renewable_check matches 1 run scoreboard players set oracle_ok renewable_check 0
execute unless score balance renewable_check matches 0 run scoreboard players set oracle_ok renewable_check 0
execute unless score phase renewable_check matches 1 run scoreboard players set oracle_ok renewable_check 0
execute if score oracle_ok renewable_check matches 1 run say RENEWABLE-ORACLE-PRE-IGNITION-OK

# One native coal is consumed and its first 8 FU are stored.
data merge block 8 280 7 {Fuel:[],Burn:1599,Duration:1600,Energy:{FluxStored:8L}}
function grindless_renewable:metrics
execute unless score coal renewable_check matches 0 run scoreboard players set oracle_ok renewable_check 0
execute unless score fuel_valid renewable_check matches 1 run scoreboard players set oracle_ok renewable_check 0
execute unless score balance renewable_check matches 0 run scoreboard players set oracle_ok renewable_check 0
execute if score oracle_ok renewable_check matches 1 run say RENEWABLE-ORACLE-IGNITION-TRANSITION-OK

# Eight paid cycles of each source, 31 retained biomass and one biomass ignition.
data merge block 8 280 8 {Items:[{Slot:1b,id:"minecraft:clay_ball",Count:8b}]}
data merge block 8 280 7 {Fuel:[{Slot:0b,id:"grindless:biomass",Count:31b}],Burn:199,Duration:200,Energy:{FluxStored:1288L}}
function grindless_renewable:poll
schedule clear grindless_renewable:poll
execute unless score coal renewable_check matches 0 run scoreboard players set oracle_ok renewable_check 0
execute unless score fuel_valid renewable_check matches 1 run scoreboard players set oracle_ok renewable_check 0
execute unless score bio renewable_check matches 8 run scoreboard players set oracle_ok renewable_check 0
execute unless score burnt renewable_check matches 1 run scoreboard players set oracle_ok renewable_check 0
execute unless score balance renewable_check matches 0 run scoreboard players set oracle_ok renewable_check 0
execute unless score phase renewable_check matches 2 run scoreboard players set oracle_ok renewable_check 0
execute if score oracle_ok renewable_check matches 1 run say RENEWABLE-ORACLE-BIOMASS-IGNITION-OK

# A real 1-FU discrepancy must still stop the poll.
data merge block 8 280 7 {Energy:{FluxStored:1287L}}
function grindless_renewable:poll
schedule clear grindless_renewable:poll
execute unless score balance renewable_check matches 1 run scoreboard players set oracle_ok renewable_check 0
execute unless score phase renewable_check matches -1 run scoreboard players set oracle_ok renewable_check 0
execute if score oracle_ok renewable_check matches 1 run say RENEWABLE-ORACLE-NONZERO-IMBALANCE-OK

# Injected biomass is not free fuel either: one extra item creates a -1,600-FU discrepancy.
scoreboard players set phase renewable_check 2
data merge block 8 280 7 {Fuel:[{Slot:0b,id:"grindless:biomass",Count:32b}],Energy:{FluxStored:1288L}}
function grindless_renewable:poll
execute unless score balance renewable_check matches -1600 run scoreboard players set oracle_ok renewable_check 0
execute unless score phase renewable_check matches -1 run scoreboard players set oracle_ok renewable_check 0
execute if score oracle_ok renewable_check matches 1 run say RENEWABLE-ORACLE-EXTRA-BIOMASS-REJECTED

# A second coal is extra external fuel, not a larger startup allowance.
function grindless_renewable:begin
schedule clear grindless_renewable:poll
data merge block 8 280 7 {Fuel:[{Slot:0b,id:"minecraft:coal",Count:2b}]}
function grindless_renewable:poll
execute unless score fuel_count renewable_check matches 2 run scoreboard players set oracle_ok renewable_check 0
execute unless score fuel_valid renewable_check matches 0 run scoreboard players set oracle_ok renewable_check 0
execute unless score coal renewable_check matches -1 run scoreboard players set oracle_ok renewable_check 0
execute unless score phase renewable_check matches -1 run scoreboard players set oracle_ok renewable_check 0
execute if score oracle_ok renewable_check matches 1 run say RENEWABLE-ORACLE-EXTRA-COAL-REJECTED

# Another burnable item cannot impersonate the named initial coal.
function grindless_renewable:begin
schedule clear grindless_renewable:poll
data merge block 8 280 7 {Fuel:[{Slot:0b,id:"minecraft:charcoal",Count:1b}]}
function grindless_renewable:poll
execute unless score fuel_valid renewable_check matches 0 run scoreboard players set oracle_ok renewable_check 0
execute unless score coal renewable_check matches -1 run scoreboard players set oracle_ok renewable_check 0
execute unless score phase renewable_check matches -1 run scoreboard players set oracle_ok renewable_check 0
execute if score oracle_ok renewable_check matches 1 run say RENEWABLE-ORACLE-OTHER-FUEL-REJECTED

# Native ItemStack loading discards a malformed Count. It must not recreate presumed coal.
function grindless_renewable:begin
schedule clear grindless_renewable:poll
data merge block 8 280 7 {Fuel:[{Slot:0b,id:"minecraft:coal",Count:"unreadable"}]}
function grindless_renewable:poll
execute unless score coal renewable_check matches 0 run scoreboard players set oracle_ok renewable_check 0
execute unless score balance renewable_check matches 12800 run scoreboard players set oracle_ok renewable_check 0
execute unless score phase renewable_check matches -1 run scoreboard players set oracle_ok renewable_check 0
execute if score oracle_ok renewable_check matches 1 run say RENEWABLE-ORACLE-MALFORMED-FUEL-REJECTED

# Missing native inventory is a failed read, never an assumed zero/one coal.
function grindless_renewable:begin
schedule clear grindless_renewable:poll
setblock 8 280 7 minecraft:stone
function grindless_renewable:poll
execute unless score fuel_read renewable_check matches 0 run scoreboard players set oracle_ok renewable_check 0
execute unless score fuel_valid renewable_check matches 0 run scoreboard players set oracle_ok renewable_check 0
execute unless score coal renewable_check matches -1 run scoreboard players set oracle_ok renewable_check 0
execute unless score phase renewable_check matches -1 run scoreboard players set oracle_ok renewable_check 0
execute if score oracle_ok renewable_check matches 1 run say RENEWABLE-ORACLE-UNAVAILABLE-INVENTORY-REJECTED

# Exact pre-ignition balance does not extend the original 600/601 poll boundary.
function grindless_renewable:begin
schedule clear grindless_renewable:poll
scoreboard players set tries renewable_check 599
function grindless_renewable:poll
schedule clear grindless_renewable:poll
execute unless score tries renewable_check matches 600 run scoreboard players set oracle_ok renewable_check 0
execute unless score phase renewable_check matches 1 run scoreboard players set oracle_ok renewable_check 0
function grindless_renewable:poll
execute unless score tries renewable_check matches 601 run scoreboard players set oracle_ok renewable_check 0
execute unless score coal renewable_check matches 1 run scoreboard players set oracle_ok renewable_check 0
execute unless score balance renewable_check matches 0 run scoreboard players set oracle_ok renewable_check 0
execute unless score phase renewable_check matches -1 run scoreboard players set oracle_ok renewable_check 0
execute if score oracle_ok renewable_check matches 1 run say RENEWABLE-ORACLE-NO-IGNITION-BOUND-PRESERVED

# Leave no synthetic inventory/work in an active production observation.
function grindless_renewable:begin
schedule clear grindless_renewable:poll
scoreboard players set phase renewable_check 0
execute if score oracle_ok renewable_check matches 1 run say RENEWABLE-ORACLE-REGRESSION-OK
execute unless score oracle_ok renewable_check matches 1 run say RENEWABLE-ORACLE-REGRESSION-FAILED
