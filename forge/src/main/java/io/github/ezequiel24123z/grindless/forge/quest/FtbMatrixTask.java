package io.github.ezequiel24123z.grindless.forge.quest;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.item.ControlMatrixItem;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/** Native item task with the existing Matrix parser, including valid legacy untagged F1 items. */
public final class FtbMatrixTask extends ItemTask {

    public static final TaskType TYPE = TaskTypes.register(Grindless.id("matrix"), FtbMatrixTask::new,
            () -> Icon.getIcon("grindless:item/relay_matrix"));

    private QuestCatalogue.Task catalogue;

    public FtbMatrixTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override
    public TaskType getType() {
        return TYPE;
    }

    @Override
    public boolean test(ItemStack stack) {
        QuestCatalogue.Task task = catalogue();
        return !stack.isEmpty()
                && task.subject().equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                && stack.getItem() instanceof ControlMatrixItem matrix
                && matrix.spec(stack).map(spec -> spec.rating() == task.matrixRating()).orElse(false);
    }

    @Override
    public void readData(CompoundTag tag) {
        super.readData(tag);
        readCatalogue(tag.getString("grindless_task"));
    }

    @Override
    public void writeData(CompoundTag tag) {
        super.writeData(tag);
        tag.putString("grindless_task", catalogue().id());
    }

    @Override
    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);
        readCatalogue(buffer.readUtf(64));
    }

    @Override
    public void writeNetData(FriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeUtf(catalogue().id(), 64);
    }

    private void readCatalogue(String id) {
        QuestCatalogue.Task task = QuestCatalogue.byId(id);
        if (task == null || task.evidence() != QuestCatalogue.Evidence.MATRIX
                || this.id != FtbQuestDefinitions.taskId(task.id())) {
            throw new IllegalArgumentException("Unknown or mismatched native Matrix task: " + id);
        }
        catalogue = task;
    }

    private QuestCatalogue.Task catalogue() {
        if (catalogue == null) {
            throw new IllegalStateException("Native Matrix task has not loaded its catalogue binding");
        }
        return catalogue;
    }
}
