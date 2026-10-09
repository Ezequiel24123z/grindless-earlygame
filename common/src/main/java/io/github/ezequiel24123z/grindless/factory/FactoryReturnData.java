package io.github.ezequiel24123z.grindless.factory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player return point for the Factory Portal; the shared factory gate is never a home bind. */
public final class FactoryReturnData extends SavedData {

    private static final String KEY = "grindless_factory_return";
    private static final String TAG_POINTS = "Points";
    private static final String TAG_PLAYER = "Player";
    private static final String TAG_DIMENSION = "Dimension";
    private static final String TAG_X = "X";
    private static final String TAG_Y = "Y";
    private static final String TAG_Z = "Z";

    private final Map<UUID, Point> points = new HashMap<>();

    public record Point(String dimension, int x, int y, int z) {
    }

    public static FactoryReturnData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                FactoryReturnData::load, FactoryReturnData::new, KEY);
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

    public static FactoryReturnData load(CompoundTag tag) {
        FactoryReturnData data = new FactoryReturnData();
        ListTag list = tag.getList(TAG_POINTS, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag point = list.getCompound(index);
            if (point.hasUUID(TAG_PLAYER)) {
                data.points.put(point.getUUID(TAG_PLAYER), new Point(
                        point.getString(TAG_DIMENSION), point.getInt(TAG_X),
                        point.getInt(TAG_Y), point.getInt(TAG_Z)));
            }
        }
        return data;
    }
}
