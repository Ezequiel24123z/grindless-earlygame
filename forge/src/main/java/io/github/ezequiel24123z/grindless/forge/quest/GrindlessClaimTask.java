package io.github.ezequiel24123z.grindless.forge.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import earth.terrarium.heracles.api.tasks.QuestTask;
import earth.terrarium.heracles.api.tasks.QuestTaskType;
import earth.terrarium.heracles.api.tasks.storage.defaults.BooleanTaskStorage;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.quest.QuestCatalogue;
import io.github.ezequiel24123z.grindless.quest.QuestProgress;
import net.minecraft.nbt.NumericTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Heracles 1.1.13 adapter: native completion means a recorded Grindless claim, not past possession. */
public record GrindlessClaimTask(String id, String catalogueId)
        implements QuestTask<ServerPlayer, NumericTag, GrindlessClaimTask> {

    public static final QuestTaskType<GrindlessClaimTask> TYPE = new QuestTaskType<>() {
        @Override
        public ResourceLocation id() {
            return Grindless.id("claim");
        }

        @Override
        public Codec<GrindlessClaimTask> codec(String taskId) {
            return Codec.STRING.comapFlatMap(value -> QuestCatalogue.byId(value) == null
                            ? DataResult.error(() -> "Unknown Grindless quest: " + value)
                            : DataResult.success(new GrindlessClaimTask(taskId, value)),
                    GrindlessClaimTask::catalogueId).fieldOf("quest").codec();
        }
    };

    @Override
    public NumericTag test(QuestTaskType<?> type, NumericTag progress, ServerPlayer player) {
        return storage().of(QuestProgress.get(player).claimed(player.getUUID()).contains(catalogueId));
    }

    @Override
    public NumericTag init(QuestTaskType<?> type, NumericTag progress, ServerPlayer player) {
        return test(type, progress, player);
    }

    @Override
    public float getProgress(NumericTag progress) {
        return storage().readBoolean(progress) ? 1 : 0;
    }

    @Override
    public BooleanTaskStorage storage() {
        return BooleanTaskStorage.INSTANCE;
    }

    @Override
    public QuestTaskType<GrindlessClaimTask> type() {
        return TYPE;
    }
}
