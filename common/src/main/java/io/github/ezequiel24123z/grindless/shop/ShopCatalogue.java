package io.github.ezequiel24123z.grindless.shop;

import io.github.ezequiel24123z.grindless.recipe.BootstrapProcesses;
import io.github.ezequiel24123z.grindless.source.SourceLogic;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** The recovery exchange's sole price, batch and unlock authority. No money or reward balance. */
public final class ShopCatalogue {
    private static final Pattern ITEM_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern OFFER_ID = Pattern.compile("[a-z0-9_]+");

    public enum Role { EMERGENCY, RECOVERY, CONVENIENCE, DECORATION }
    public enum PaymentSource { EXISTING_MOD_PRODUCTS, MANUAL_FINITE_COMPATIBILITY }

    public record Goods(String item, int count) {
        public Goods {
            if (item == null || !ITEM_ID.matcher(item).matches() || count < 1 || count > 64) {
                throw new IllegalArgumentException("Invalid recovery goods");
            }
        }
    }

    public record Offer(String id, Map<String, Integer> payment, Goods goods, Set<String> requires,
                        Role role, PaymentSource paymentSource, String surplusSource,
                        String productionAlternative, String progressionRisk) {
        public Offer {
            if (id == null || !OFFER_ID.matcher(id).matches() || payment == null || payment.isEmpty()
                    || goods == null || role == null || paymentSource == null || requires == null
                    || surplusSource == null || surplusSource.isBlank()
                    || productionAlternative == null || productionAlternative.isBlank()
                    || progressionRisk == null || progressionRisk.isBlank()) {
                throw new IllegalArgumentException("Incomplete recovery offer");
            }
            payment.forEach((item, count) -> {
                if (item == null || !ITEM_ID.matcher(item).matches() || count == null || count < 1 || count > 64) {
                    throw new IllegalArgumentException("Invalid recovery payment: " + id);
                }
            });
            payment = java.util.Collections.unmodifiableMap(new TreeMap<>(payment));
            requires = Set.copyOf(requires);
        }
    }

    private static final String AGGREGATE = SourceLogic.AGGREGATE;
    private static final String BIOMASS = SourceLogic.BIOMASS;
    private static final String BOARDS = SourceLogic.FIBREBOARD;
    private static final String GROG = BootstrapProcesses.GROG;
    private static final String GEOLOGY = "source/geological_aggregate; water, eligible bed and paid work required";
    private static final String BIOLOGY = "source/biomass; geological aggregate, water, sunlight and paid work required";
    private static final String CERAMICS = "bootstrap/insulators or ceramic recovery; fresh clay, water, fuel and work remain necessary";

    private static final List<Offer> OFFERS = List.of(
            emergency("bootstrap_stone", 32, "minecraft:cobblestone", 8,
                    "Punitively priced manual restart; native dry aggregate is far more productive"),
            emergency("bootstrap_wood", 32, "minecraft:oak_log", 4,
                    "Finite manually dug terrain, not the canonical fibreboard producer"),
            emergency("bootstrap_water", 64, "minecraft:water_bucket", 1,
                    "One paid replacement container; no recurring free reward or reset of the starter claim"),
            offer("stone", Map.of(AGGREGATE, 4), "minecraft:cobblestone", 4, Set.of("quern"),
                    Role.RECOVERY, GEOLOGY, "bootstrap/dry_aggregate", "Costlier than direct Quern output; no machine sold"),
            offer("gravel", Map.of(AGGREGATE, 4), "minecraft:gravel", 4, Set.of("quern"),
                    Role.RECOVERY, GEOLOGY, "bootstrap/dry_aggregate", "Forfeits the dry route's cobblestone coproduct"),
            offer("sand", Map.of(AGGREGATE, 4), "minecraft:sand", 4, Set.of("quern"),
                    Role.EMERGENCY, GEOLOGY, "bootstrap/dry_aggregate + bootstrap/sand", "Requires the Quern quest; forfeits coproducts"),
            offer("clay", Map.of(AGGREGATE, 8), "minecraft:clay_ball", 4, Set.of("clay_mixer"),
                    Role.EMERGENCY, GEOLOGY, "source/geological_aggregate clay coproduct", "Does not sell unfired ceramics or insulators"),
            offer("wood", Map.of(BOARDS, 16), "minecraft:oak_log", 2, Set.of("clay_mixer"),
                    Role.RECOVERY, BIOLOGY + "; bootstrap/fibreboard", "Use fibreboard directly through grindless:boards",
                    "Optional vanilla wood; fewer equivalent boards than payment"),
            offer("food", Map.of(BIOMASS, 8), "minecraft:bread", 2, Set.of("quern"),
                    Role.EMERGENCY, BIOLOGY, "Existing finite starter food; optional vanilla food routes",
                    "Small paid food recovery, not a free recurring kit"),
            offer("sapling", Map.of(BOARDS, 8, AGGREGATE, 4), "minecraft:oak_sapling", 1, Set.of("quern"),
                    Role.CONVENIENCE, BIOLOGY + "; bootstrap/fibreboard", "Optional vanilla tree growth",
                    "Trees are optional; no canonical resource depends on farming them"),
            offer("seeds", Map.of(BIOMASS, 4, AGGREGATE, 2), "minecraft:wheat_seeds", 4, Set.of("quern"),
                    Role.CONVENIENCE, BIOLOGY, "Optional vanilla wheat growth",
                    "No crop, village, mob or structure becomes a mandatory dependency"),
            offer("water", Map.of(AGGREGATE, 8, "minecraft:bucket", 1), "minecraft:water_bucket", 1, Set.of("water"),
                    Role.RECOVERY, GEOLOGY + "; existing empty bucket returned filled", "source/hand_pump",
                    "Refill only: consumes one real empty bucket; never mints containers"),
            offer("fuel", Map.of(BIOMASS, 16), "minecraft:charcoal", 1, Set.of("clay_mixer"),
                    Role.EMERGENCY, BIOLOGY, "bootstrap/biocarbon",
                    "One fuel per exchange at eight times the native biomass cost"),
            offer("bricks", Map.of(GROG, 8, AGGREGATE, 4), "minecraft:brick", 4, Set.of("ceramics"),
                    Role.RECOVERY, CERAMICS, "bootstrap/bricks",
                    "After ceramics only; grog-to-brick-to-grog loses payment, no insulator bypass"),
            offer("dirt", Map.of(AGGREGATE, 8), "minecraft:dirt", 8, Set.of("settling"),
                    Role.DECORATION, GEOLOGY, "bootstrap/wet_aggregate",
                    "Half the native dirt yield per aggregate and no metal/redstone coproducts"),
            offer("grass", Map.of(AGGREGATE, 4, BIOMASS, 4), "minecraft:grass_block", 4, Set.of("settling"),
                    Role.DECORATION, GEOLOGY + "; " + BIOLOGY, "Optional terrain and vanilla spreading",
                    "Landscaping only; not a source of canonical biomass"),
            offer("glass", Map.of(GROG, 16, AGGREGATE, 8), "minecraft:glass", 4, Set.of("calibrate"),
                    Role.DECORATION, CERAMICS + "; " + GEOLOGY, "bootstrap/glass",
                    "Only after physical calibration; first glass and the core must already be produced"));

    private ShopCatalogue() {
    }

    public static List<Offer> offers() {
        return OFFERS;
    }

    public static Offer byId(String id) {
        return OFFERS.stream().filter(offer -> offer.id().equals(id)).findFirst().orElse(null);
    }

    private static Offer offer(String id, Map<String, Integer> payment, String item, int count,
                               Set<String> requires, Role role, String source, String alternative, String risk) {
        return new Offer(id, payment, new Goods(item, count), requires, role,
                PaymentSource.EXISTING_MOD_PRODUCTS, source, alternative, risk);
    }

    private static Offer emergency(String id, int dirt, String item, int count, String risk) {
        return new Offer(id, Map.of("minecraft:dirt", dirt), new Goods(item, count), Set.of("starter_supplies"),
                Role.EMERGENCY, PaymentSource.MANUAL_FINITE_COMPATIBILITY,
                "Bare-hand Superflat dirt; finite manual compatibility recovery, not mod-owned renewable supply",
                "Preserved free starter; establish geological/biological production and native pumping", risk);
    }
}
