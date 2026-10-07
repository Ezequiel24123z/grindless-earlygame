package io.github.ezequiel24123z.grindless.matrix;

import io.github.ezequiel24123z.grindless.energy.FluxTier;

/**
 * A manufacturing family for Control Matrices.
 *
 * <p>Each family owns three frontier ratings and can also manufacture every older rating through
 * its retrospective routes (ADR-0107).
 */
public enum MatrixArchitecture {

    RELAY("relay", FluxTier.F1, FluxTier.F3),
    INTEGRATED("integrated", FluxTier.F4, FluxTier.F6),
    SUPERCONDUCTING("superconducting", FluxTier.F7, FluxTier.F9),
    PHOTONIC("photonic", FluxTier.F10, FluxTier.F12),
    CAUSAL("causal", FluxTier.F13, FluxTier.F15);

    private final String id;
    private final FluxTier firstFrontier;
    private final FluxTier lastFrontier;

    MatrixArchitecture(String id, FluxTier firstFrontier, FluxTier lastFrontier) {
        this.id = id;
        this.firstFrontier = firstFrontier;
        this.lastFrontier = lastFrontier;
    }

    public String id() {
        return id;
    }

    public FluxTier firstFrontier() {
        return firstFrontier;
    }

    public FluxTier lastFrontier() {
        return lastFrontier;
    }

    public boolean supports(FluxTier rating) {
        return rating != null && rating != FluxTier.F0
                && rating.ordinal() <= lastFrontier.ordinal();
    }

    public boolean isFrontier(FluxTier rating) {
        return supports(rating)
                && rating.ordinal() >= firstFrontier.ordinal();
    }

    public static MatrixArchitecture byId(String id) {
        if (id == null) {
            return null;
        }
        for (MatrixArchitecture architecture : values()) {
            if (architecture.id.equals(id)) {
                return architecture;
            }
        }
        return null;
    }
}
