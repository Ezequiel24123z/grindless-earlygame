package io.github.ezequiel24123z.grindless.forge.quest;

import dev.ftb.mods.ftbquests.api.ItemFilterAdapter;
import io.github.ezequiel24123z.grindless.item.ControlMatrixItem;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A physical Matrix predicate for the framework's unmodified, nonconsuming ItemTask. */
public final class FtbMatrixFilter implements ItemFilterAdapter {

    public static final String BINDING = "grindless:quest_matrix";

    @Override
    public String getName() {
        return "Grindless";
    }

    @Override
    public boolean isFilterStack(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(BINDING);
    }

    @Override
    public boolean doesItemMatch(ItemStack filterStack, ItemStack toCheck) {
        return getMatcher(filterStack).test(toCheck);
    }

    @Override
    public Matcher getMatcher(ItemStack filterStack) {
        if (!isFilterStack(filterStack) || !filterStack.getTag().contains(BINDING, Tag.TAG_STRING)) {
            return NO_MATCH;
        }
        QuestCatalogue.Task declared = QuestCatalogue.byId(filterStack.getTag().getString(BINDING));
        if (declared == null || declared.evidence() != QuestCatalogue.Evidence.MATRIX
                || !sameSubject(filterStack, declared)
                || !(filterStack.getItem() instanceof ControlMatrixItem filterItem)
                || filterItem.spec(filterStack)
                .filter(spec -> spec.rating() == declared.matrixRating()).isEmpty()) {
            return NO_MATCH;
        }
        return candidate -> sameSubject(candidate, declared)
                && candidate.getItem() instanceof ControlMatrixItem candidateItem
                && candidateItem.spec(candidate)
                .filter(spec -> spec.rating() == declared.matrixRating()).isPresent();
    }

    @Override
    public boolean hasItemTagFilter() {
        return false;
    }

    @Override
    public ItemStack makeTagFilterStack(TagKey<Item> tag) {
        throw new UnsupportedOperationException("Matrix quest filters cannot be converted to item tags");
    }

    private static boolean sameSubject(ItemStack stack, QuestCatalogue.Task task) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(task.subject());
    }
}
