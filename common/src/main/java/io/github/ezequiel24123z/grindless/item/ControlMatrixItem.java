package io.github.ezequiel24123z.grindless.item;

import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.matrix.ControlMatrixSpec;
import io.github.ezequiel24123z.grindless.matrix.MatrixArchitecture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** One Control Matrix architecture; NBT carries its aligned T1-T15 rating. */
public final class ControlMatrixItem extends Item {

    public static final String TAG_RATING = "Rating";

    private static final FluxTier[] TIERS = FluxTier.values();

    private final MatrixArchitecture architecture;

    public ControlMatrixItem(MatrixArchitecture architecture, Properties properties) {
        super(properties);
        this.architecture = architecture;
    }

    public MatrixArchitecture architecture() {
        return architecture;
    }

    /**
     * Reads the physical specification.
     *
     * <p>A stack without a rating is the architecture's first frontier so the original T1 Relay
     * Matrix recipe remains a normal vanilla recipe. A present but malformed value is rejected.
     */
    public Optional<ControlMatrixSpec> spec(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != this) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getTag();
        int ordinal = architecture.firstFrontier().ordinal();
        if (tag != null && tag.contains(TAG_RATING)) {
            if (!tag.contains(TAG_RATING, Tag.TAG_ANY_NUMERIC)) {
                return Optional.empty();
            }
            ordinal = tag.getInt(TAG_RATING);
        }
        if (ordinal <= FluxTier.F0.ordinal() || ordinal >= TIERS.length) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ControlMatrixSpec(architecture, TIERS[ordinal]));
        } catch (IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

    public ItemStack stackFor(FluxTier rating, int count) {
        ControlMatrixSpec requested = new ControlMatrixSpec(architecture, rating);
        ItemStack stack = new ItemStack(this, count);
        stack.getOrCreateTag().putInt(TAG_RATING, requested.rating().ordinal());
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        return spec(stack)
                .<Component>map(matrix -> Component.translatable(
                        "item.grindless.control_matrix.named",
                        matrix.technologyRating(),
                        Component.translatable("matrix.grindless.architecture." + architecture.id())))
                .orElseGet(() -> Component.translatable("item.grindless.control_matrix.invalid"));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        Optional<ControlMatrixSpec> matrix = spec(stack);
        if (matrix.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.grindless.control_matrix.invalid"));
            return;
        }
        ControlMatrixSpec value = matrix.get();
        tooltip.add(Component.translatable(
                "tooltip.grindless.control_matrix.rating",
                value.technologyRating(),
                value.rating().ordinal(),
                value.rating().nominal()));
        tooltip.add(Component.translatable(
                "tooltip.grindless.control_matrix.architecture",
                Component.translatable("matrix.grindless.architecture." + architecture.id())));
        if (!value.isFrontier()) {
            tooltip.add(Component.translatable("tooltip.grindless.control_matrix.retrospective"));
        }
    }
}
