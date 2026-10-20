package io.github.ezequiel24123z.grindless.matrix;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Optional;

/** The existing Matrix rating format, separate from item registration so its real NBT is testable. */
public final class ControlMatrixNbt {

    public static final String RATING = "Rating";
    private static final FluxTier[] TIERS = FluxTier.values();

    private ControlMatrixNbt() {
    }

    /** Writes the canonical integral rating without introducing a second saved-world identifier. */
    public static CompoundTag save(ControlMatrixSpec matrix) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(RATING, matrix.rating().ordinal());
        return tag;
    }

    /** Reads legacy untagged frontiers; malformed explicit data remains an invalid Matrix. */
    public static Optional<ControlMatrixSpec> load(MatrixArchitecture architecture, CompoundTag tag) {
        int ordinal = architecture.firstFrontier().ordinal();
        if (tag != null && tag.contains(RATING)) {
            if (!tag.contains(RATING, Tag.TAG_ANY_NUMERIC)) {
                return Optional.empty();
            }
            ordinal = tag.getInt(RATING);
            if (tag.getDouble(RATING) != ordinal) {
                return Optional.empty();
            }
        }
        if (ordinal <= 0 || ordinal >= TIERS.length || !architecture.supports(TIERS[ordinal])) {
            return Optional.empty();
        }
        return Optional.of(new ControlMatrixSpec(architecture, TIERS[ordinal]));
    }
}
