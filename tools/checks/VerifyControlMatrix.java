package io.github.ezequiel24123z.grindless.matrix;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.recipe.IngredientSpec;

/** Behaviour checks for Control Matrix ratings and architectures. Not part of the mod. */
public final class VerifyControlMatrix {

    private static int failures;

    public static void main(String[] args) {
        architectures();
        substitution();
        ingredientRoles();
        invalidSpecs();

        System.out.println(failures == 0
                ? "ALL CONTROL MATRIX CHECKS PASSED"
                : failures + " CONTROL MATRIX CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void architectures() {
        eq("architecture id round-trips", MatrixArchitecture.RELAY,
                MatrixArchitecture.byId("relay"));
        yes("Relay starts at T1", MatrixArchitecture.RELAY.isFrontier(FluxTier.F1));
        yes("Relay ends at T3", MatrixArchitecture.RELAY.isFrontier(FluxTier.F3));
        no("Relay cannot make T4", MatrixArchitecture.RELAY.supports(FluxTier.F4));
        yes("Integrated has a retrospective T1 route",
                MatrixArchitecture.INTEGRATED.supports(FluxTier.F1));
        no("retrospective T1 is not an Integrated frontier",
                MatrixArchitecture.INTEGRATED.isFrontier(FluxTier.F1));
        yes("Integrated starts its frontier at T4",
                MatrixArchitecture.INTEGRATED.isFrontier(FluxTier.F4));
        yes("Causal reaches T15", MatrixArchitecture.CAUSAL.isFrontier(FluxTier.F15));
    }

    private static void substitution() {
        ControlMatrixSpec t3 = new ControlMatrixSpec(MatrixArchitecture.RELAY, FluxTier.F3);
        eq("technology rating follows aligned ordinal", 3, t3.technologyRating());
        yes("T3 can control T1", t3.canControl(FluxTier.F1));
        yes("T3 can control T3", t3.canControl(FluxTier.F3));
        no("T3 cannot control T4", t3.canControl(FluxTier.F4));
        no("matrices do not gate T0", t3.canControl(FluxTier.F0));

        ControlMatrixSpec retrospective =
                new ControlMatrixSpec(MatrixArchitecture.INTEGRATED, FluxTier.F2);
        no("an old rating on a later architecture is retrospective", retrospective.isFrontier());
        yes("its physical rating still controls T2", retrospective.canControl(FluxTier.F2));
    }

    private static void invalidSpecs() {
        rejects("there is no T0 Control Matrix",
                () -> new ControlMatrixSpec(MatrixArchitecture.RELAY, FluxTier.F0));
        rejects("Relay cannot carry T4",
                () -> new ControlMatrixSpec(MatrixArchitecture.RELAY, FluxTier.F4));
        rejects("architecture is required",
                () -> new ControlMatrixSpec(null, FluxTier.F1));
    }

    private static void ingredientRoles() {
        String relay = "grindless:relay_matrix";
        IngredientSpec exact = IngredientSpec.matrix(relay, FluxTier.F1, 2);
        IngredientSpec minimum = IngredientSpec.minimumControl(relay, FluxTier.F1, 2);
        IngredientSpec f2 = IngredientSpec.minimumControl(relay, FluxTier.F2, 2);
        eq("the old four-argument constructor remains exactly rated", exact,
                new IngredientSpec(IngredientSpec.ITEM, relay, 2, 1));
        eq("the old ordinary constructor retains its contract", IngredientSpec.item(relay, 2),
                new IngredientSpec(IngredientSpec.ITEM, relay, 2, 0));
        eq("the new role does not alter canonical input identity", exact.qualified(), minimum.qualified());
        no("exact and minimum inputs are distinct data", exact.equals(minimum));
        eq("exact input role is explicit", IngredientSpec.MatrixRole.EXACT, exact.matrixRole());
        eq("minimum input role is explicit", IngredientSpec.MatrixRole.MINIMUM_CONTROL, minimum.matrixRole());
        for (MatrixArchitecture architecture : MatrixArchitecture.values()) {
            String id = "grindless:" + architecture.id() + "_matrix";
            for (FluxTier rating : FluxTier.values()) {
                if (!architecture.supports(rating)) {
                    continue;
                }
                ControlMatrixSpec physical = new ControlMatrixSpec(architecture, rating);
                String label = architecture + "/" + rating;
                eq("exact manufacturing preserves item identity and rating " + label,
                        architecture == MatrixArchitecture.RELAY && rating == FluxTier.F1,
                        exact.acceptsMatrix(id, physical));
                yes("minimum F1 accepts validated sufficient capacity " + label,
                        minimum.acceptsMatrix(id, physical));
                eq("minimum F2 checks capacity rather than architecture " + label,
                        physical.canControl(FluxTier.F2), f2.acceptsMatrix(id, physical));
            }
        }
        no("an absent physical specification is never admission", minimum.acceptsMatrix(relay, null));
        no("an absent validated item identity is never admission",
                minimum.acceptsMatrix(null, new ControlMatrixSpec(MatrixArchitecture.RELAY, FluxTier.F1)));
        rejects("minimum control cannot require F0",
                () -> IngredientSpec.minimumControl(relay, FluxTier.F0, 1));
        rejects("minimum control requires a rating",
                () -> IngredientSpec.minimumControl(relay, null, 1));
        rejects("minimum control cannot use an empty batch",
                () -> IngredientSpec.minimumControl(relay, FluxTier.F1, 0));
        rejects("minimum control cannot use a negative batch",
                () -> IngredientSpec.minimumControl(relay, FluxTier.F1, -1));
        rejects("a minimum role cannot hide an unspecified rating",
                () -> new IngredientSpec(IngredientSpec.ITEM, relay, 1, 0,
                        IngredientSpec.MatrixRole.MINIMUM_CONTROL));
        rejects("a minimum role cannot turn a tag into a Matrix",
                () -> new IngredientSpec(IngredientSpec.TAG, relay, 1, 1,
                        IngredientSpec.MatrixRole.MINIMUM_CONTROL));
        rejects("a minimum role cannot turn a fluid into a Matrix",
                () -> new IngredientSpec(IngredientSpec.FLUID, relay, 1, 1,
                        IngredientSpec.MatrixRole.MINIMUM_CONTROL));
        rejects("the typed role is required",
                () -> new IngredientSpec(IngredientSpec.ITEM, relay, 1, 1, null));
    }

    private static void eq(String what, boolean expected, boolean actual) {
        if (expected == actual) {
            pass(what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void rejects(String what, Runnable action) {
        try {
            action.run();
            fail(what + ": expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            pass(what);
        }
    }

    private static void eq(String what, int expected, int actual) {
        if (expected == actual) {
            pass(what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            pass(what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void yes(String what, boolean actual) {
        if (actual) {
            pass(what);
        } else {
            fail(what + ": expected true");
        }
    }

    private static void no(String what, boolean actual) {
        if (!actual) {
            pass(what);
        } else {
            fail(what + ": expected false");
        }
    }

    private static void pass(String what) {
        System.out.println("PASS  " + what);
    }

    private static void fail(String what) {
        failures++;
        System.out.println("FAIL  " + what);
    }
}
