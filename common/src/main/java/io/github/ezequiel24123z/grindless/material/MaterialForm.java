package io.github.ezequiel24123z.grindless.material;

/**
 * The form axis of the item catalogue (ADR-0032).
 *
 * <p>The catalogue is a matrix, not a list: Grindless authors these forms and the materials come
 * from the pack's tags, so one authored form instantly covers every material installed. That is
 * the only shape that survives a material set which is not known until the pack loads (ADR-0004).
 *
 * <p>Each form carries the tag path it is discovered and resolved under. Where another mod already
 * provides an item for a material and form, Grindless uses <em>that</em> item rather than
 * registering a rival one.
 */
public enum MaterialForm {

    // Ore-line forms. The grade is what each contributes to final metal yield, and it is a
    // property of the form rather than of the material — which is what lets one generated recipe
    // set cover the whole pack (ADR-0035).
    ORE("ores", 1.00, true),
    RAW("raw_materials", 1.00, true),
    CRUSHED("crushed_materials", 2.00, false),
    WASHED("washed_crushed", 2.00, false),
    OXIDE("oxides", 0.0, false),
    PURIFIED("purified_materials", 2.55, false),
    DUST("dusts", 2.00, true),

    // Metal and formed stock.
    NUGGET("nuggets", 0.0, true),
    INGOT("ingots", 0.0, true),
    GEM("gems", 0.0, true),
    STORAGE_BLOCK("storage_blocks", 0.0, true),
    PLATE("plates", 0.0, true),
    FOIL("foils", 0.0, false),
    ROD("rods", 0.0, true),
    BOLT("bolts", 0.0, false),
    GEAR("gears", 0.0, true),
    RING("rings", 0.0, false),
    WIRE("wires", 0.0, false),
    FINE_WIRE("fine_wires", 0.0, false),
    COIL("coils", 0.0, false);

    private final String tagPath;
    private final double grade;
    private final boolean conventional;

    MaterialForm(String tagPath, double grade, boolean conventional) {
        this.tagPath = tagPath;
        this.grade = grade;
        this.conventional = conventional;
    }

    /**
     * Whether other mods agree on a tag for this form, so that Grindless can share it with them.
     *
     * <p>{@code forge:ingots/tin} is followed by essentially every mod; {@code forge:bolts/tin} is
     * followed by none. Conventional forms are published and consumed under {@code forge:}, which
     * is what makes a recipe's ingredient interchangeable with another mod's item. Forms without a
     * convention are Grindless's own intermediates and live under {@code grindless:}, published so
     * other mods can opt in, because claiming an unagreed {@code forge:} tag would only mean
     * nothing else ever fills it (ADR-0050, the same boundary ADR-0033 draws for reagents).
     */
    public boolean isConventional() {
        return conventional;
    }

    /** The tag namespace this form is shared under: {@code forge} or {@code grindless}. */
    public String tagNamespace() {
        return conventional ? "forge" : "grindless";
    }

    /** The tag path for one material of this form, such as {@code ingots/tin}. */
    public String tagPath(String material) {
        return tagPath + "/" + material;
    }

    /**
     * The tag directory this form lives under, such as {@code ingots} in
     * {@code forge:ingots/copper}.
     */
    public String tagPath() {
        return tagPath;
    }

    /**
     * How much final metal a unit of this form contributes, or {@code 0} for forms that are not
     * part of the ore line.
     */
    public double grade() {
        return grade;
    }

    /** Whether this form carries an ore-line grade. */
    public boolean isOreLine() {
        return grade > 0.0;
    }

    /** The form published under {@code tagPath}, such as {@code ingots}, or {@code null}. */
    public static MaterialForm byTagPath(String tagPath) {
        for (MaterialForm form : values()) {
            if (form.tagPath.equals(tagPath)) {
                return form;
            }
        }
        return null;
    }
}
