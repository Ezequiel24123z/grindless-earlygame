package io.github.ezequiel24123z.grindless.matrix;

import io.github.ezequiel24123z.grindless.energy.FluxTier;

/** The physical architecture and aligned technology rating carried by one Control Matrix. */
public record ControlMatrixSpec(MatrixArchitecture architecture, FluxTier rating) {

    public ControlMatrixSpec {
        if (architecture == null) {
            throw new IllegalArgumentException("matrix architecture is required");
        }
        if (!architecture.supports(rating)) {
            throw new IllegalArgumentException(architecture.id() + " does not support "
                    + (rating == null ? "null" : rating.name()));
        }
    }

    public int technologyRating() {
        return rating.ordinal();
    }

    public boolean isFrontier() {
        return architecture.isFrontier(rating);
    }

    /** A higher physical rating may replace a lower one, regardless of architecture. */
    public boolean canControl(FluxTier required) {
        return required != null && required != FluxTier.F0
                && rating.ordinal() >= required.ordinal();
    }
}
