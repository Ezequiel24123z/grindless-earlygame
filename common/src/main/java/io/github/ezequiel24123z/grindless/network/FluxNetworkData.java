package io.github.ezequiel24123z.grindless.network;

import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Every Flux Network in one dimension, and the spatial index that finds them.
 *
 * <h2>Why this is level data and not block-entity data</h2>
 *
 * <p>Network membership lives here rather than on the pylons themselves (ADR-0007). Block entities
 * unload with their chunks, so a network whose middle is unloaded would fragment into pieces that
 * do not know about each other — and rejoin, in some order, on reload. Level-wide saved data has
 * no such topology, so an unloaded chunk changes nothing.
 *
 * <p>Membership is recomputed only on placement, breakage or a manual link change. Nothing here
 * runs per tick except {@link #tickNetworks()}, which is a short loop over networks rather than
 * over pylons or machines.
 */
public final class FluxNetworkData extends SavedData {

    /** The storage key. Per dimension, because a network cannot span dimensions. */
    public static final String KEY = "grindless_flux";

    private static final String TAG_NETWORKS = "Networks";
    private static final String TAG_PYLONS = "Pylons";
    private static final String TAG_ID = "Id";
    private static final String TAG_STORED = "Stored";
    private static final String TAG_POS = "Pos";
    private static final String TAG_TIER = "Tier";
    private static final String TAG_NEXT_ID = "NextId";

    private final PylonIndex index = new PylonIndex();
    private final Map<Integer, FluxNetwork> networks = new HashMap<>();
    private final Map<BlockPos, Integer> membership = new HashMap<>();

    private int nextId = 1;

    /**
     * The network data for {@code level}, creating it if this dimension has none yet.
     *
     * <p>Per dimension, because a network cannot span dimensions — a pylon in the Nether is not
     * within link range of one in the Overworld in any meaningful sense, and pretending otherwise
     * would make coordinates mean two different things in one index.
     */
    public static FluxNetworkData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FluxNetworkData::load, FluxNetworkData::new, KEY);
    }

    /** The spatial index over pylon positions. */
    public PylonIndex index() {
        return index;
    }

    /** Every network currently in this dimension. */
    public Collection<FluxNetwork> networks() {
        return Collections.unmodifiableCollection(networks.values());
    }

    /** The network a pylon belongs to, or {@code null} if that position holds no pylon. */
    public FluxNetwork networkAt(BlockPos pylon) {
        Integer id = membership.get(pylon.immutable());
        return id == null ? null : networks.get(id);
    }

    /**
     * The network powering {@code pos}, or {@code null} if nothing covers it.
     *
     * <p>This is the call a machine makes, so it is the one that has to be cheap: an index lookup
     * and a short scan of the pylons filed under that chunk. Where supply areas overlap, the
     * first covering pylon wins — overlapping networks are a layout the player chose, and
     * splitting a machine's draw between them would make its behaviour depend on iteration order.
     */
    public FluxNetwork networkCovering(BlockPos pos) {
        for (BlockPos pylon : index.covering(pos)) {
            Integer id = membership.get(pylon);
            if (id != null) {
                FluxNetwork network = networks.get(id);
                if (network != null) {
                    return network;
                }
            }
        }
        return null;
    }

    // ---- Topology ---------------------------------------------------------------------------

    /**
     * Registers a pylon, merging it into every network it links to.
     *
     * <p>Linking to several networks at once merges them all into one, which is how a player
     * joins two separate bases by dropping a pylon between them. The surviving network is the
     * largest, so the common case — a small outpost joining a big base — moves the fewest entries.
     */
    public void addPylon(BlockPos pos, PylonTier tier) {
        BlockPos key = pos.immutable();
        if (index.contains(key)) {
            removePylon(key);
        }
        index.add(key, tier);

        Set<Integer> neighbourNetworks = new HashSet<>();
        for (BlockPos linked : index.linkedTo(key, tier)) {
            Integer id = membership.get(linked);
            if (id != null) {
                neighbourNetworks.add(id);
            }
        }

        FluxNetwork target;
        if (neighbourNetworks.isEmpty()) {
            target = new FluxNetwork(nextId++);
            networks.put(target.id(), target);
        } else {
            target = largestOf(neighbourNetworks);
            for (Integer id : neighbourNetworks) {
                if (id != target.id()) {
                    absorb(target, networks.get(id));
                }
            }
        }

        target.addPylon(key, tier);
        membership.put(key, target.id());
        setDirty();
    }

    /**
     * Removes a pylon, splitting its network if that disconnects it.
     *
     * <p>Removal is the expensive direction and is deliberately allowed to be: it recomputes
     * connectivity by flood fill over the remaining members. That is fine because it happens when
     * a player breaks a block, not every tick — the whole point of ADR-0007 is to pay here so
     * nothing has to be paid in the tick loop.
     */
    public void removePylon(BlockPos pos) {
        BlockPos key = pos.immutable();
        PylonTier tier = index.tierAt(key);
        if (tier == null) {
            return;
        }
        Integer id = membership.remove(key);
        index.remove(key);

        FluxNetwork network = id == null ? null : networks.get(id);
        if (network == null) {
            setDirty();
            return;
        }
        network.removePylon(key, tier);
        if (network.isEmpty()) {
            networks.remove(network.id());
            setDirty();
            return;
        }
        resplit(network);
        setDirty();
    }

    /**
     * Recomputes the connected components of {@code network} and splits it if it fell apart.
     *
     * <p>Energy follows capacity: a fragment holding a third of the network's pylons keeps a third
     * of its charge. Any other rule either destroys energy or lets a player manufacture it by
     * breaking a pylon.
     */
    private void resplit(FluxNetwork network) {
        List<Set<BlockPos>> components = connectedComponents(network.pylons());
        if (components.size() <= 1) {
            return;
        }
        long storedBefore = network.stored();
        long capacityBefore = network.capacity();

        // The first component keeps the original network so its id survives, which keeps any
        // reference to it valid; the rest become new networks.
        boolean first = true;
        for (Set<BlockPos> component : components) {
            FluxNetwork target;
            if (first) {
                first = false;
                target = network;
                for (BlockPos pylon : new ArrayList<>(network.pylons())) {
                    if (!component.contains(pylon)) {
                        PylonTier tier = index.tierAt(pylon);
                        if (tier != null) {
                            network.removePylon(pylon, tier);
                        }
                    }
                }
            } else {
                target = new FluxNetwork(nextId++);
                networks.put(target.id(), target);
                for (BlockPos pylon : component) {
                    PylonTier tier = index.tierAt(pylon);
                    if (tier != null) {
                        target.addPylon(pylon, tier);
                    }
                }
            }
            for (BlockPos pylon : component) {
                membership.put(pylon, target.id());
            }
            if (capacityBefore > 0L) {
                double share = (double) target.capacity() / (double) capacityBefore;
                target.setStored(Math.round(storedBefore * share));
            }
        }
    }

    /** Groups {@code members} into sets that are transitively within link range of each other. */
    private List<Set<BlockPos>> connectedComponents(Set<BlockPos> members) {
        Set<BlockPos> unvisited = new HashSet<>(members);
        List<Set<BlockPos>> components = new ArrayList<>();
        while (!unvisited.isEmpty()) {
            BlockPos seed = unvisited.iterator().next();
            Set<BlockPos> component = new HashSet<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            queue.add(seed);
            unvisited.remove(seed);
            while (!queue.isEmpty()) {
                BlockPos current = queue.poll();
                component.add(current);
                PylonTier tier = index.tierAt(current);
                if (tier == null) {
                    continue;
                }
                for (BlockPos linked : index.linkedTo(current, tier)) {
                    if (unvisited.remove(linked)) {
                        queue.add(linked);
                    }
                }
            }
            components.add(component);
        }
        return components;
    }

    private FluxNetwork largestOf(Set<Integer> ids) {
        FluxNetwork largest = null;
        for (Integer id : ids) {
            FluxNetwork candidate = networks.get(id);
            if (candidate != null
                    && (largest == null || candidate.pylons().size() > largest.pylons().size())) {
                largest = candidate;
            }
        }
        return largest;
    }

    /** Moves every pylon and all the energy from {@code source} into {@code target}. */
    private void absorb(FluxNetwork target, FluxNetwork source) {
        if (source == null || source == target) {
            return;
        }
        for (BlockPos pylon : new ArrayList<>(source.pylons())) {
            PylonTier tier = index.tierAt(pylon);
            if (tier != null) {
                target.addPylon(pylon, tier);
            }
            membership.put(pylon, target.id());
        }
        // Energy moves across rather than being discarded: merging two half-full networks must
        // not be a way to lose power, or players will learn to empty a network before linking it.
        target.receive(source.stored(), false);
        networks.remove(source.id());
    }

    // ---- Per tick ---------------------------------------------------------------------------

    /** Where a network's displayed status is written; the world, or a stand-in in a check. */
    @FunctionalInterface
    public interface StatusSink {
        void show(BlockPos pylon, MachineStatus status);
    }

    /** Resolves supply against demand without showing anything. */
    public void tickNetworks() {
        tickNetworks((pylon, status) -> { });
    }

    /**
     * Resolves each network's supply against the demand registered this tick, and shows the result
     * on its pylons.
     *
     * <p>A loop over networks, not over pylons or machines, so its cost is the number of separate
     * power grids a player has built — a number that stays small even in a large base. Pylons are
     * written only when their network's displayed status changes or its membership does, which is
     * a handful of times a minute at most thanks to the debounce, never every tick.
     */
    public void tickNetworks(StatusSink sink) {
        for (FluxNetwork network : networks.values()) {
            network.resolveTick();
            if (network.takeDisplayStale()) {
                MachineStatus status = network.displayStatus();
                for (BlockPos pylon : network.pylons()) {
                    sink.show(pylon, status);
                }
            }
        }
    }

    // ---- Persistence ------------------------------------------------------------------------

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag networkList = new ListTag();
        for (FluxNetwork network : networks.values()) {
            CompoundTag networkTag = new CompoundTag();
            networkTag.putInt(TAG_ID, network.id());
            networkTag.putLong(TAG_STORED, network.stored());
            ListTag pylonList = new ListTag();
            for (BlockPos pylon : network.pylons()) {
                PylonTier tier = index.tierAt(pylon);
                if (tier == null) {
                    continue;
                }
                CompoundTag pylonTag = new CompoundTag();
                pylonTag.putLong(TAG_POS, pylon.asLong());
                pylonTag.putByte(TAG_TIER, (byte) tier.ordinal());
                pylonList.add(pylonTag);
            }
            networkTag.put(TAG_PYLONS, pylonList);
            networkList.add(networkTag);
        }
        tag.put(TAG_NETWORKS, networkList);
        tag.putInt(TAG_NEXT_ID, nextId);
        return tag;
    }

    /**
     * Rebuilds from saved data.
     *
     * <p>The index is rebuilt from the saved membership rather than saved separately, so the two
     * cannot disagree. Tier ordinals are bounds-checked: they are the one field that silently
     * becomes garbage if a later version reorders the enum, and an unchecked read would throw
     * during world load, which a player sees as a corrupt save rather than as a mod bug.
     */
    public static FluxNetworkData load(CompoundTag tag) {
        FluxNetworkData data = new FluxNetworkData();
        PylonTier[] tiers = PylonTier.values();
        ListTag networkList = tag.getList(TAG_NETWORKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < networkList.size(); i++) {
            CompoundTag networkTag = networkList.getCompound(i);
            FluxNetwork network = new FluxNetwork(networkTag.getInt(TAG_ID));
            ListTag pylonList = networkTag.getList(TAG_PYLONS, Tag.TAG_COMPOUND);
            for (int j = 0; j < pylonList.size(); j++) {
                CompoundTag pylonTag = pylonList.getCompound(j);
                BlockPos pos = BlockPos.of(pylonTag.getLong(TAG_POS));
                byte ordinal = pylonTag.getByte(TAG_TIER);
                PylonTier tier = ordinal >= 0 && ordinal < tiers.length
                        ? tiers[ordinal]
                        : PylonTier.MK1;
                data.index.add(pos, tier);
                network.addPylon(pos, tier);
                data.membership.put(pos.immutable(), network.id());
            }
            network.setStored(networkTag.getLong(TAG_STORED));
            data.networks.put(network.id(), network);
        }
        data.nextId = Math.max(1, tag.getInt(TAG_NEXT_ID));
        return data;
    }
}
