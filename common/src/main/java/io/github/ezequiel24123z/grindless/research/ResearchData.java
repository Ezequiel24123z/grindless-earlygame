package io.github.ezequiel24123z.grindless.research;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Which blueprints this world has unlocked.
 *
 * <p>World-scoped, not per-player: research is a production target the factory feeds, and a
 * hopper pushing Data Cores into a terminal has to count (ADR-0057). A later session that wants
 * per-player trees can split this; unlocking twice is harmless because the set is idempotent.
 */
public final class ResearchData extends SavedData {

    public static final String KEY = "grindless_research";

    private static final String TAG_UNLOCKED = "Unlocked";

    private final EnumSet<Blueprint> unlocked = EnumSet.noneOf(Blueprint.class);

    public static ResearchData get(ServerLevel level) {
        return get(level.getServer());
    }

    /**
     * The overworld copy, so nether and end share the tree. Research is a property of the save,
     * not of the dimension the terminal happens to stand in.
     */
    public static ResearchData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(ResearchData::load, ResearchData::new, KEY);
    }

    public boolean isUnlocked(Blueprint blueprint) {
        return unlocked.contains(blueprint);
    }

    /**
     * Marks {@code blueprint} researched. Returns whether this call was the one that unlocked it.
     */
    public boolean unlock(Blueprint blueprint) {
        if (blueprint == null || !unlocked.add(blueprint)) {
            return false;
        }
        setDirty();
        return true;
    }

    public Set<Blueprint> unlocked() {
        return Collections.unmodifiableSet(unlocked);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Blueprint blueprint : unlocked) {
            list.add(StringTag.valueOf(blueprint.id()));
        }
        tag.put(TAG_UNLOCKED, list);
        return tag;
    }

    public static ResearchData load(CompoundTag tag) {
        ResearchData data = new ResearchData();
        ListTag list = tag.getList(TAG_UNLOCKED, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            Blueprint blueprint = Blueprint.byId(list.getString(i));
            if (blueprint != null) {
                data.unlocked.add(blueprint);
            }
        }
        return data;
    }
}
