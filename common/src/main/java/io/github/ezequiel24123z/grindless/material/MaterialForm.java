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
    ORE("ores", 1.00),
    RAW("raw_materials", 1.00),
    CRUSHED("crushed_materials", 2.00),
    PURIFIED("purified_materials", 2.55),
    DUST("dusts", 2.00),

    // Metal and formed stock.
    NUGGET("nuggets", 0.0),
    INGOT("ingots", 0.0),
    GEM("gems", 0.0),
    STORAGE_BLOCK("storage_blocks", 0.0),
    PLATE("plates", 0.0),
    FOIL("foils", 0.0),
    ROD("rods", 0.0),
    BOLT("bolts", 0.0),
    GEAR("gears", 0.0),
    RING("rings", 0.0),
    WIRE("wires", 0.0),
    FINE_WIRE("fine_wires", 0.0),
    COIL("coils", 0.0);

    private final String tagPath;
    private final double grade;

    MaterialForm(String tagPath, double grade) {
        this.tagPath = tagPath;
        this.grade = grade;
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
}
