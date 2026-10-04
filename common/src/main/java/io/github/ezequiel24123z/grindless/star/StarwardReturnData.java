package io.github.ezequiel24123z.grindless.star;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Where each traveller was standing when they left for the Drift.
 *
 * <p>Stored on the overworld, which a dedicated server keeps loaded. The pad on the Drift
 * is shared. The way home is not. This is not the lunar return: a moon trip and a
 * starward trip remember different doors.
 */
public final class StarwardReturnData extends SavedData {

    public static final String KEY = "grindless_starward_return";

    private static final String TAG_POINTS = "Points";
    private static final String TAG_PLAYER = "Player";
    private static final String TAG_DIMENSION = "Dimension";
    private static final String TAG_X = "X";
    private static final String TAG_Y = "Y";
    private static final String TAG_Z = "Z";

    private final Map<UUID, Point> points = new HashMap<>();

    /**
     * @param dimension dimension id, such as {@code minecraft:overworld}
     */
    public record Point(String dimension, int x, int y, int z) {
    }

    public static StarwardReturnData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                StarwardReturnData::load, StarwardReturnData::new, KEY);
    }

    public void remember(UUID player, String dimension, int x, int y, int z) {
        points.put(player, new Point(dimension, x, y, z));
        setDirty();
    }

    public Point point(UUID player) {
        return points.get(player);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Point> entry : points.entrySet()) {
            CompoundTag point = new CompoundTag();
            point.putUUID(TAG_PLAYER, entry.getKey());
            point.putString(TAG_DIMENSION, entry.getValue().dimension());
            point.putInt(TAG_X, entry.getValue().x());
            point.putInt(TAG_Y, entry.getValue().y());
            point.putInt(TAG_Z, entry.getValue().z());
            list.add(point);
        }
        tag.put(TAG_POINTS, list);
        return tag;
    }

    public static StarwardReturnData load(CompoundTag tag) {
        StarwardReturnData data = new StarwardReturnData();
        ListTag list = tag.getList(TAG_POINTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag point = list.getCompound(i);
            if (!point.hasUUID(TAG_PLAYER)) {
                continue;
            }
            data.points.put(point.getUUID(TAG_PLAYER), new Point(
                    point.getString(TAG_DIMENSION),
                    point.getInt(TAG_X),
                    point.getInt(TAG_Y),
                    point.getInt(TAG_Z)));
        }
        return data;
    }
}
