package io.github.ezequiel24123z.grindless.shop;

import io.github.ezequiel24123z.grindless.recipe.BootstrapRecipes;
import io.github.ezequiel24123z.grindless.source.SourceLogic;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A sufficient exact potential certificate over entire conversion batches, not buy/sell pairs.
 * Non-increase in every repeatable row composes over cycles of any length and mixed batches.
 */
public final class EconomyCycleProof {
    public record Check(String route, String disposition, String inputPotential, String outputPotential) {
    }

    public record Certificate(boolean conservative, List<Check> checks, Map<String, Long> potential,
                              List<String> unresolved) {
        public Certificate {
            checks = List.copyOf(checks);
            potential = Collections.unmodifiableMap(new TreeMap<>(potential));
            unresolved = List.copyOf(unresolved);
        }
    }

    private EconomyCycleProof() {
    }

    public static Certificate check() {
        List<EconomyGraph.Row> rows = EconomyGraph.rows();
        return check(rows, potentials(rows));
    }

    public static Certificate check(List<EconomyGraph.Row> rows, Map<String, Long> potential) {
        List<Check> checks = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        for (EconomyGraph.Row row : rows) {
            if (row.boundary() == EconomyGraph.Boundary.FINITE_QUEST) {
                boolean bounded = row.id().startsWith("quest/") && row.inputs().equals(
                        Map.of("finite_claim:" + row.id().substring("quest/".length()), 1L));
                checks.add(new Check(row.id(), bounded ? "FINITE_ONE_CLAIM_NOT_REPEATABLE" : "UNBOUND_QUEST_BUDGET", null, null));
                if (!bounded) {
                    unresolved.add(row.id());
                }
                continue;
            }
            if (row.boundary() == EconomyGraph.Boundary.ENVIRONMENTAL_SOURCE) {
                boolean bound = row.id().equals("source/geological_aggregate")
                        && row.inputs().containsKey("environment:eligible_rock_bed")
                        || row.id().equals("source/biomass") && row.inputs().containsKey("environment:eligible_sunlight");
                bound &= row.inputs().getOrDefault("fluid:minecraft:water", 0L) > 0 && row.fuPerTick() > 0;
                checks.add(new Check(row.id(), bound ? "EXPLICIT_ENVIRONMENT_WATER_WORK_SOURCE" : "UNBOUND_SOURCE", null, null));
                if (!bound) {
                    unresolved.add(row.id());
                }
                continue;
            }
            BigInteger inputs = potential(row.inputs(), potential);
            BigInteger outputs = potential(row.outputs(), potential);
            boolean bound = inputs != null && outputs != null && outputs.compareTo(inputs) <= 0;
            checks.add(new Check(row.id(), bound ? "NON_INCREASING" : "UNPROVEN_POTENTIAL",
                    inputs == null ? null : inputs.toString(), outputs == null ? null : outputs.toString()));
            if (!bound) {
                unresolved.add(row.id());
            }
        }
        return new Certificate(unresolved.isEmpty(), checks, potential, unresolved);
    }

    /** Proof units only: not prices, physical mass, money, a wallet or gameplay data. */
    public static Map<String, Long> potentials(List<EconomyGraph.Row> rows) {
        Map<String, Long> units = new TreeMap<>();
        put(units, 64, SourceLogic.AGGREGATE);
        put(units, 8, SourceLogic.BIOMASS);
        put(units, 4, SourceLogic.FIBREBOARD, "minecraft:sand", "minecraft:clay_ball",
                "minecraft:brick", "grindless:ceramic_grog", "grindless:unfired_ceramic",
                "grindless:ceramic_insulator", "minecraft:glass", "minecraft:oak_planks", "minecraft:flint");
        put(units, 2, "minecraft:cobblestone", "minecraft:redstone",
                "minecraft:paper", "grindless:slag", "minecraft:wheat_seeds", "minecraft:poppy");
        put(units, 1, "minecraft:stick");
        put(units, 8, "minecraft:gravel", "minecraft:dirt", "minecraft:raw_iron", "minecraft:raw_copper", "minecraft:iron_ingot",
                "minecraft:copper_ingot", "minecraft:bread", "minecraft:grass_block");
        put(units, 16, "minecraft:charcoal", "minecraft:coal", "minecraft:oak_log",
                "minecraft:oak_sapling", "minecraft:bricks", "minecraft:clay", "minecraft:furnace",
                "minecraft:crafting_table");
        put(units, 24, "minecraft:bucket", "minecraft:water_bucket");
        units.put(BootstrapRecipes.COBBLE, 2L);
        units.put(BootstrapRecipes.PLANKS, 4L);
        units.put(BootstrapRecipes.IRON, 8L);
        units.put(BootstrapRecipes.COPPER, 8L);
        units.put("tag:forge:raw_materials/iron", 8L);
        units.put("tag:forge:raw_materials/copper", 8L);
        units.put("tag:grindless:carbon", 16L);
        // Craft quantities remain canonical; only otherwise unknown construction outputs get units.
        for (int pass = 0; pass < rows.size(); pass++) {
            boolean changed = false;
            for (EconomyGraph.Row row : rows) {
                if (row.boundary() != EconomyGraph.Boundary.CONVERSION || row.outputs().size() != 1
                        || !(row.id().startsWith("craft/") || row.id().startsWith("calibrate/"))) {
                    continue;
                }
                var output = row.outputs().entrySet().iterator().next();
                if (units.containsKey(output.getKey())) {
                    continue;
                }
                BigInteger input = potential(row.inputs(), units);
                if (input != null) {
                    long value = input.divide(BigInteger.valueOf(output.getValue())).longValueExact();
                    if (value > 0) {
                        units.put(output.getKey(), value);
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
        return Collections.unmodifiableMap(units);
    }

    private static BigInteger potential(Map<String, Long> batch, Map<String, Long> units) {
        BigInteger sum = BigInteger.ZERO;
        for (var amount : batch.entrySet()) {
            if (amount.getKey().startsWith("fluid:")) {
                continue;
            }
            Long unit = units.get(amount.getKey());
            if (unit == null || unit <= 0) {
                return null;
            }
            sum = sum.add(BigInteger.valueOf(unit).multiply(BigInteger.valueOf(amount.getValue())));
        }
        return sum;
    }

    private static void put(Map<String, Long> units, long value, String... items) {
        for (String item : items) {
            units.put(EconomyGraph.item(item), value);
        }
    }
}
