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
 * over pylons or machines. Manual-link upkeep is demand on that loop, not a walk of the edges.
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
    private static final String TAG_LINKS = "Links";
    private static final String TAG_A = "A";
    private static final String TAG_B = "B";
    private static final String TAG_BANKS = "Banks";
    private static final String TAG_EXTRA = "Extra";

    private final PylonIndex index = new PylonIndex();
    private final Map<Integer, FluxNetwork> networks = new HashMap<>();
    private final Map<BlockPos, Integer> membership = new HashMap<>();
    private final Set<ManualLink> links = new HashSet<>();
    private final Map<BlockPos, Long> banks = new HashMap<>();

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

    /** Every manual link currently in this dimension. */
    public Set<ManualLink> links() {
        return Collections.unmodifiableSet(links);
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
     *
     * <p>Transformers and capacitor banks are not pylons. They never appear here.
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
     * Manual links count as neighbours here, otherwise a conduit trunk would not pull a newly
     * placed pylon onto the network it was drawn to.
     */
    public void addPylon(BlockPos pos, PylonTier tier) {
        BlockPos key = pos.immutable();
        if (index.contains(key)) {
            removePylon(key);
        }
        index.add(key, tier);

        Set<Integer> neighbourNetworks = new HashSet<>();
        for (BlockPos linked : neighboursOf(key, tier)) {
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
        assignBanks();
        setDirty();
    }

    /**
     * Removes a pylon, splitting its network if that disconnects it.
     *
     * <p>Removal is the expensive direction and is deliberately allowed to be: it recomputes
     * connectivity by flood fill over the remaining members. That is fine because it happens when
     * a player breaks a block, not every tick — the whole point of ADR-0007 is to pay here so
     * nothing has to be paid in the tick loop. Manual edges that touched the pylon go with it.
     */
    public void removePylon(BlockPos pos) {
        BlockPos key = pos.immutable();
        PylonTier tier = index.tierAt(key);
        if (tier == null) {
            return;
        }
        Integer id = membership.remove(key);
        index.remove(key);
        dropLinksAt(key);

        FluxNetwork network = id == null ? null : networks.get(id);
        if (network == null) {
            assignBanks();
            setDirty();
            return;
        }
        network.removePylon(key, tier);
        if (network.isEmpty()) {
            networks.remove(network.id());
            assignBanks();
            setDirty();
            return;
        }
        resplit(network);
        assignBanks();
        setDirty();
    }

    /**
     * Draws a manual link between two pylons, merging their networks if they were separate.
     *
     * @return {@code true} if the edge is now present (created or already there)
     */
    public boolean addManualLink(BlockPos a, BlockPos b) {
        BlockPos from = a.immutable();
        BlockPos to = b.immutable();
        if (from.equals(to) || !index.contains(from) || !index.contains(to)) {
            return false;
        }
        ManualLink link = ManualLink.of(from, to);
        boolean added = links.add(link);
        FluxNetwork first = networkAt(from);
        FluxNetwork second = networkAt(to);
        if (first != null && second != null && first.id() != second.id()) {
            Set<Integer> ids = new HashSet<>();
            ids.add(first.id());
            ids.add(second.id());
            FluxNetwork target = largestOf(ids);
            FluxNetwork source = target.id() == first.id() ? second : first;
            absorb(target, source);
        }
        if (added) {
            assignBanks();
            setDirty();
        }
        return true;
    }

    /**
     * Drops the manual link between two pylons, splitting the network if that was the last path.
     *
     * @return {@code true} if an edge was removed
     */
    public boolean removeManualLink(BlockPos a, BlockPos b) {
        ManualLink link = ManualLink.of(a, b);
        if (!links.remove(link)) {
            return false;
        }
        FluxNetwork network = networkAt(a);
        if (network != null) {
            resplit(network);
        }
        assignBanks();
        setDirty();
        return true;
    }

    /** Whether a manual edge already joins these pylons. */
    public boolean hasManualLink(BlockPos a, BlockPos b) {
        return links.contains(ManualLink.of(a, b));
    }

    /**
     * Registers a capacitor bank at {@code pos}.
     *
     * <p>The bank's extra capacity is assigned to whichever network currently covers it. No
     * covering pylon means the extra is held until one does — the bank is not a pylon.
     */
    public void addBank(BlockPos pos, long extra) {
        banks.put(pos.immutable(), Math.max(0L, extra));
        assignBanks();
        setDirty();
    }

    /** Unregisters the capacitor bank at {@code pos}, if any. */
    public void removeBank(BlockPos pos) {
        if (banks.remove(pos.immutable()) != null) {
            assignBanks();
            setDirty();
        }
    }

    /**
     * Recomputes the connected components of {@code network} and splits it if it fell apart.
     *
     * <p>Energy follows pylon capacity: a fragment holding a third of the network's pylons keeps a
     * third of its charge. Bank extra is reassigned after the split, so a capacitor cannot skew
     * the share or manufacture energy by being broken and replaced. Any other rule either destroys
     * energy or lets a player manufacture it by breaking a pylon.
     */
    private void resplit(FluxNetwork network) {
        List<Set<BlockPos>> components = connectedComponents(network.pylons());
        if (components.size() <= 1) {
            return;
        }
        network.setExtraCapacity(0L);
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

    /**
     * Groups {@code members} into sets that are transitively linked, by auto-range or by a
     * manual edge.
     */
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
                for (BlockPos linked : neighboursOf(current, tier)) {
                    if (unvisited.remove(linked)) {
                        queue.add(linked);
                    }
                }
            }
            components.add(component);
        }
        return components;
    }

    /** Auto-range neighbours plus every manual edge from {@code pos}. */
    private List<BlockPos> neighboursOf(BlockPos pos, PylonTier tier) {
        List<BlockPos> linked = new ArrayList<>(index.linkedTo(pos, tier));
        BlockPos key = pos.immutable();
        for (ManualLink link : links) {
            BlockPos other = link.other(key);
            if (other != null && index.contains(other) && !linked.contains(other)) {
                linked.add(other);
            }
        }
        return linked;
    }

    private void dropLinksAt(BlockPos pos) {
        links.removeIf(link -> link.touches(pos));
    }

    /**
     * Writes each bank's extra onto the network that covers it, and nowhere else.
     *
     * <p>Called after every topology change rather than every tick. A bank in overlapping
     * coverage follows {@link #networkCovering}: the first covering pylon wins.
     */
    private void assignBanks() {
        Map<Integer, Long> extra = new HashMap<>();
        for (Map.Entry<BlockPos, Long> bank : banks.entrySet()) {
            FluxNetwork network = networkCovering(bank.getKey());
            if (network != null) {
                extra.merge(network.id(), bank.getValue(), Long::sum);
            }
        }
        for (FluxNetwork network : networks.values()) {
            network.setExtraCapacity(extra.getOrDefault(network.id(), 0L));
        }
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
        source.setExtraCapacity(0L);
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
     *
     * <p>Manual-link upkeep is charged here, once per edge, onto the network that holds it. A
     * walk of every pylon is exactly the cost ADR-0007 refused.
     */
    public void tickNetworks(StatusSink sink) {
        for (ManualLink link : links) {
            FluxNetwork network = networkAt(link.a());
            if (network != null) {
                network.addUpkeep(link.upkeep());
            }
        }
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

        ListTag linkList = new ListTag();
        for (ManualLink link : links) {
            CompoundTag linkTag = new CompoundTag();
            linkTag.putLong(TAG_A, link.a().asLong());
            linkTag.putLong(TAG_B, link.b().asLong());
            linkList.add(linkTag);
        }
        tag.put(TAG_LINKS, linkList);

        ListTag bankList = new ListTag();
        for (Map.Entry<BlockPos, Long> bank : banks.entrySet()) {
            CompoundTag bankTag = new CompoundTag();
            bankTag.putLong(TAG_POS, bank.getKey().asLong());
            bankTag.putLong(TAG_EXTRA, bank.getValue());
            bankList.add(bankTag);
        }
        tag.put(TAG_BANKS, bankList);
        return tag;
    }

    /**
     * Rebuilds from saved data.
     *
     * <p>The index is rebuilt from the saved membership rather than saved separately, so the two
     * cannot disagree. Tier ordinals are bounds-checked: they are the one field that silently
     * becomes garbage if a later version reorders the enum, and an unchecked read would throw
     * during world load, which a player sees as a corrupt save rather than as a mod bug. Links
     * that name a missing pylon are dropped: a player who deleted a world backup mid-write
     * should lose a trunk, not the load.
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

        ListTag linkList = tag.getList(TAG_LINKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < linkList.size(); i++) {
            CompoundTag linkTag = linkList.getCompound(i);
            BlockPos a = BlockPos.of(linkTag.getLong(TAG_A));
            BlockPos b = BlockPos.of(linkTag.getLong(TAG_B));
            if (data.index.contains(a) && data.index.contains(b) && !a.equals(b)) {
                data.links.add(ManualLink.of(a, b));
            }
        }

        ListTag bankList = tag.getList(TAG_BANKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < bankList.size(); i++) {
            CompoundTag bankTag = bankList.getCompound(i);
            data.banks.put(BlockPos.of(bankTag.getLong(TAG_POS)), Math.max(0L, bankTag.getLong(TAG_EXTRA)));
        }
        data.assignBanks();
        return data;
    }
}
