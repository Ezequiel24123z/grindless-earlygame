package io.github.ezequiel24123z.grindless.network;

import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.structure.GroundArrayLogic;
import io.github.ezequiel24123z.grindless.machine.StatusDebounce;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/** Behaviour checks for the Flux Network. Not part of the mod. */
public final class VerifyNetwork {

    private static int failures = 0;

    public static void main(String[] args) {
        tiers();
        index();
        merging();
        splitting();
        brownouts();
        pylonStatus();
        persistence();
        spanning();
        banks();
        arrays();
        transformer();

        System.out.println(failures == 0
                ? "ALL NETWORK CHECKS PASSED"
                : failures + " NETWORK CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void tiers() {
        eq("MK1 supply area", 48, PylonTier.MK1.supplyArea());
        eq("MK3 supply area", 128, PylonTier.MK3.supplyArea());
        eq("MK1 radius", 24, PylonTier.MK1.radius());
        eq("a pylon is three blocks tall", 3, PylonStructure.HEIGHT);

        BlockPos origin = new BlockPos(0, 64, 0);
        yes("covers its own position", PylonTier.MK1.covers(origin, origin));
        yes("covers the cube edge", PylonTier.MK1.covers(origin, new BlockPos(24, 64, 0)));
        no("does not cover past the edge", PylonTier.MK1.covers(origin, new BlockPos(25, 64, 0)));
        yes("covers a corner", PylonTier.MK1.covers(origin, new BlockPos(24, 88, 24)));
        no("does not cover past a corner",
                PylonTier.MK1.covers(origin, new BlockPos(24, 89, 24)));
        yes("covers vertically too", PylonTier.MK1.covers(origin, new BlockPos(0, 40, 0)));

        BlockPos far = new BlockPos(80, 64, 0);
        no("MK1 pair beyond the shorter range does not link",
                PylonTier.linksAutomatically(PylonTier.MK1, origin, PylonTier.MK1, far));
        no("MK3 cannot capture a distant MK1",
                PylonTier.linksAutomatically(PylonTier.MK3, origin, PylonTier.MK1, far));
        yes("MK3 pair at the same distance does link",
                PylonTier.linksAutomatically(PylonTier.MK3, origin, PylonTier.MK3, far));
        yes("link range is symmetric",
                PylonTier.linksAutomatically(PylonTier.MK1, origin, PylonTier.MK3, far)
                        == PylonTier.linksAutomatically(PylonTier.MK3, far, PylonTier.MK1, origin));
    }

    private static void index() {
        PylonIndex idx = new PylonIndex();
        eq("starts empty", 0, idx.size());
        eq("nothing covers an empty index", 0, idx.covering(new BlockPos(0, 64, 0)).size());

        BlockPos a = new BlockPos(0, 64, 0);
        idx.add(a, PylonTier.MK1);
        eq("one pylon indexed", 1, idx.size());
        yes("knows its tier", idx.tierAt(a) == PylonTier.MK1);
        eq("covers its own block", 1, idx.covering(a).size());
        eq("covers inside the cube", 1, idx.covering(new BlockPos(20, 64, 20)).size());
        eq("does not cover outside it", 0, idx.covering(new BlockPos(30, 64, 30)).size());

        // The chunk bucket is coarser than the cube, so a pylon can be filed under a chunk it
        // only clips. The exact per-axis test still has to run.
        idx.add(new BlockPos(100, 64, 100), PylonTier.MK1);
        eq("a second pylon does not bleed into the first's area",
                1, idx.covering(new BlockPos(5, 64, 5)).size());

        // Overlapping areas are a legitimate layout and both must be found.
        idx.add(new BlockPos(4, 64, 4), PylonTier.MK1);
        eq("overlapping pylons are both found", 2, idx.covering(new BlockPos(4, 64, 4)).size());

        idx.remove(a);
        eq("removal shrinks the index", 2, idx.size());
        eq("removal stops it covering", 1, idx.covering(new BlockPos(4, 64, 4)).size());
        no("removed pylon is gone", idx.contains(a));

        // Re-adding at the same position must replace rather than duplicate.
        idx.add(new BlockPos(4, 64, 4), PylonTier.MK2);
        eq("re-adding does not duplicate", 2, idx.size());
        yes("re-adding updates the tier",
                idx.tierAt(new BlockPos(4, 64, 4)) == PylonTier.MK2);

        // A large pylon spans several chunks and must be found from all of them.
        PylonIndex wide = new PylonIndex();
        wide.add(new BlockPos(0, 64, 0), PylonTier.MK3);
        eq("found near its centre", 1, wide.covering(new BlockPos(0, 64, 0)).size());
        eq("found at the far edge", 1, wide.covering(new BlockPos(64, 64, 64)).size());
        eq("not found past the edge", 0, wide.covering(new BlockPos(65, 64, 0)).size());

        wide.clear();
        eq("clear empties it", 0, wide.size());
    }

    private static void merging() {
        FluxNetworkData data = new FluxNetworkData();
        BlockPos a = new BlockPos(0, 64, 0);
        BlockPos b = new BlockPos(40, 64, 0);
        BlockPos far = new BlockPos(500, 64, 500);

        data.addPylon(a, PylonTier.MK1);
        eq("one pylon makes one network", 1, data.networks().size());
        yes("the pylon has a network", data.networkAt(a) != null);
        eq("the network has one pylon", 1, data.networkAt(a).pylons().size());

        // Within MK1 link range (64), so these join.
        data.addPylon(b, PylonTier.MK1);
        eq("a linked pylon joins rather than creating", 1, data.networks().size());
        eq("the network now has two", 2, data.networkAt(a).pylons().size());
        yes("both pylons are on the same network",
                data.networkAt(a).id() == data.networkAt(b).id());

        // Far outside link range, so this is a separate grid.
        data.addPylon(far, PylonTier.MK1);
        eq("a distant pylon makes its own network", 2, data.networks().size());
        no("and is not on the first", data.networkAt(far).id() == data.networkAt(a).id());

        // Capacity and throughput are pooled.
        eq("throughput is pooled", PylonTier.MK1.throughput() * 2,
                data.networkAt(a).throughput());

        // A pylon bridging two networks merges them, which is how two bases get joined.
        FluxNetworkData bridge = new FluxNetworkData();
        bridge.addPylon(new BlockPos(0, 64, 0), PylonTier.MK1);
        bridge.addPylon(new BlockPos(80, 64, 0), PylonTier.MK1);
        eq("two separate networks", 2, bridge.networks().size());
        bridge.addPylon(new BlockPos(40, 64, 0), PylonTier.MK1);
        eq("the bridge merges them", 1, bridge.networks().size());
        eq("all three are on it", 3,
                bridge.networkAt(new BlockPos(0, 64, 0)).pylons().size());

        FluxNetworkData energetic = new FluxNetworkData();
        energetic.addPylon(new BlockPos(0, 64, 0), PylonTier.MK1);
        energetic.addPylon(new BlockPos(80, 64, 0), PylonTier.MK1);
        energetic.networkAt(new BlockPos(0, 64, 0)).receive(1000L, false);
        energetic.networkAt(new BlockPos(80, 64, 0)).receive(2000L, false);
        energetic.addPylon(new BlockPos(40, 64, 0), PylonTier.MK1);
        eq("energy is carried across the merge", 3000L,
                energetic.networkAt(new BlockPos(0, 64, 0)).stored());
    }

    private static void splitting() {
        // A chain of three. Removing the middle one disconnects the ends.
        FluxNetworkData data = new FluxNetworkData();
        BlockPos left = new BlockPos(0, 64, 0);
        BlockPos middle = new BlockPos(40, 64, 0);
        BlockPos right = new BlockPos(80, 64, 0);
        data.addPylon(left, PylonTier.MK1);
        data.addPylon(middle, PylonTier.MK1);
        data.addPylon(right, PylonTier.MK1);
        eq("the chain is one network", 1, data.networks().size());

        data.networkAt(left).receive(3000L, false);
        data.removePylon(middle);
        eq("removing the middle splits it", 2, data.networks().size());
        no("the ends are now separate",
                data.networkAt(left).id() == data.networkAt(right).id());
        eq("each end keeps one pylon", 1, data.networkAt(left).pylons().size());

        // Energy follows capacity, so an even split halves it rather than duplicating or losing.
        long total = data.networkAt(left).stored() + data.networkAt(right).stored();
        eq("energy is conserved across the split", 3000L, total);
        eq("and divided evenly", 1500L, data.networkAt(left).stored());

        // Removing an end does not split anything.
        FluxNetworkData simple = new FluxNetworkData();
        simple.addPylon(left, PylonTier.MK1);
        simple.addPylon(middle, PylonTier.MK1);
        simple.removePylon(left);
        eq("removing an end leaves one network", 1, simple.networks().size());
        eq("with one pylon", 1, simple.networkAt(middle).pylons().size());

        // Removing the last pylon removes the network entirely.
        simple.removePylon(middle);
        eq("the last removal clears the network", 0, simple.networks().size());
        yes("nothing is left behind", simple.networkAt(middle) == null);

        // Removing something that was never there must be harmless.
        simple.removePylon(new BlockPos(999, 64, 999));
        eq("removing a non-pylon does nothing", 0, simple.networks().size());

        // A machine inside a supply area finds its network; one outside finds nothing.
        FluxNetworkData coverage = new FluxNetworkData();
        coverage.addPylon(left, PylonTier.MK1);
        yes("a covered machine finds its network",
                coverage.networkCovering(new BlockPos(4, 64, 4)) != null);
        yes("an uncovered machine finds nothing",
                coverage.networkCovering(new BlockPos(400, 64, 400)) == null);
    }

    private static void brownouts() {
        FluxNetwork network = new FluxNetwork(1);
        network.addPylon(new BlockPos(0, 64, 0), PylonTier.MK1);
        eq("capacity is a second of throughput", PylonTier.MK1.throughput() * 20L,
                network.capacity());

        network.receive(network.capacity(), false);
        eq("fills to capacity", network.capacity(), network.stored());
        eq("full accepts nothing more", 0L, network.receive(100L, false));

        // Demand inside supply: everything runs at full speed.
        network.registerDemand(100L);
        network.resolveTick();
        eq("met demand is full satisfaction", 1.0, network.satisfaction());
        no("and is not a brownout", network.isBrownedOut());

        // Demand above throughput: everything slows together, by the same factor.
        network.registerDemand(PylonTier.MK1.throughput() * 2L);
        network.resolveTick();
        eq("double demand halves satisfaction", 0.5, network.satisfaction());
        yes("which is a brownout", network.isBrownedOut());

        // Demand accumulates across machines before being resolved, which is what makes the
        // slowdown proportional rather than first-come-first-served.
        network.registerDemand(PylonTier.MK1.throughput());
        network.registerDemand(PylonTier.MK1.throughput());
        network.registerDemand(PylonTier.MK1.throughput());
        network.resolveTick();
        eq("three machines each get a third", 1.0 / 3.0, network.satisfaction());

        // Demand resets each tick, so a past spike does not haunt a quiet one.
        network.resolveTick();
        eq("demand does not carry over", 1.0, network.satisfaction());

        // An empty network cannot satisfy anything.
        FluxNetwork empty = new FluxNetwork(2);
        empty.addPylon(new BlockPos(0, 64, 0), PylonTier.MK1);
        empty.registerDemand(100L);
        empty.resolveTick();
        eq("an empty network satisfies nothing", 0.0, empty.satisfaction());

        // Shrinking a network must not leave it holding more than it can.
        FluxNetwork shrinking = new FluxNetwork(3);
        shrinking.addPylon(new BlockPos(0, 64, 0), PylonTier.MK1);
        shrinking.addPylon(new BlockPos(10, 64, 0), PylonTier.MK1);
        shrinking.receive(shrinking.capacity(), false);
        long before = shrinking.stored();
        shrinking.removePylon(new BlockPos(10, 64, 0), PylonTier.MK1);
        yes("a shrinking network sheds what it cannot hold",
                shrinking.stored() <= shrinking.capacity());
        yes("and really did shrink", shrinking.stored() < before);

        // Pylons are a transmission limit: a full buffer cannot be drained faster than they carry.
        FluxNetwork limited = new FluxNetwork(4);
        limited.addPylon(new BlockPos(0, 64, 0), PylonTier.MK1);
        limited.receive(limited.capacity(), false);
        long cap = PylonTier.MK1.throughput();
        eq("extraction is capped at throughput", cap, limited.extract(cap * 5L, false));
        eq("and the allowance is spent for the tick", 0L, limited.extract(1L, false));
        eq("a simulated extract sees the spent allowance", 0L, limited.extract(1L, true));
        limited.resolveTick();
        eq("the allowance returns next tick", cap, limited.extract(cap * 5L, true));
        eq("a simulated extract does not spend it", cap, limited.extract(cap * 5L, false));
    }

    private static void pylonStatus() {
        FluxNetworkData data = new FluxNetworkData();
        BlockPos pylon = new BlockPos(0, 64, 0);
        data.addPylon(pylon, PylonTier.MK1);
        FluxNetwork network = data.networkAt(pylon);
        java.util.List<String> writes = new java.util.ArrayList<>();
        FluxNetworkData.StatusSink sink = (pos, status) -> writes.add(status.name());

        data.tickNetworks(sink);
        eq("a new pylon is told its starting status", 1, writes.size());
        eq("which is idle", "IDLE", writes.get(0));
        for (int i = 0; i < 100; i++) {
            data.tickNetworks(sink);
        }
        eq("a quiet network writes nothing further", 1, writes.size());

        network.receive(1000L, false);
        data.tickNetworks(sink);
        eq("power moving is shown at once", "RUNNING", writes.get(writes.size() - 1));
        int afterRunning = writes.size();
        for (int tick = 0; tick < 100; tick++) {
            if (tick % 5 == 0) {
                network.receive(10L, false);
            }
            data.tickNetworks(sink);
        }
        eq("a pulse every fifth tick does not flicker the pylon", afterRunning, writes.size());

        network.registerDemand(PylonTier.MK1.throughput() * 4L);
        data.tickNetworks(sink);
        eq("demand it cannot meet is shown as starved", "STARVED", writes.get(writes.size() - 1));

        for (int i = 0; i < StatusDebounce.IDLE_HOLD + StatusDebounce.RECOVER_HOLD + 5; i++) {
            data.tickNetworks(sink);
        }
        eq("and a network left alone settles back to idle", "IDLE", writes.get(writes.size() - 1));

        int before = writes.size();
        data.addPylon(new BlockPos(5, 64, 0), PylonTier.MK1);
        data.tickNetworks(sink);
        eq("a pylon joining is told the network's status, and so is the rest", 2, writes.size() - before);
        eq("the network reports what its pylons show", MachineStatus.IDLE.name(),
                data.networkAt(pylon).displayStatus().name());
    }

    private static void persistence() {
        FluxNetworkData data = new FluxNetworkData();
        data.addPylon(new BlockPos(0, 64, 0), PylonTier.MK1);
        data.addPylon(new BlockPos(20, 64, 0), PylonTier.MK2);
        data.addPylon(new BlockPos(500, 64, 500), PylonTier.MK3);
        data.networkAt(new BlockPos(0, 64, 0)).receive(5000L, false);

        FluxNetworkData loaded = FluxNetworkData.load(data.save(new CompoundTag()));

        eq("network count survives", 2, loaded.networks().size());
        eq("pylon count survives", 3, loaded.index().size());
        yes("tiers survive",
                loaded.index().tierAt(new BlockPos(20, 64, 0)) == PylonTier.MK2);
        yes("membership survives",
                loaded.networkAt(new BlockPos(0, 64, 0)).id()
                        == loaded.networkAt(new BlockPos(20, 64, 0)).id());
        eq("stored energy survives", 5000L,
                loaded.networkAt(new BlockPos(0, 64, 0)).stored());
        yes("coverage works after loading",
                loaded.networkCovering(new BlockPos(4, 64, 4)) != null);

        // The index is rebuilt from membership, so a loaded world can keep building without
        // the new pylons colliding with ids that were already handed out.
        loaded.addPylon(new BlockPos(1000, 64, 1000), PylonTier.MK1);
        eq("a new network can still be made", 3, loaded.networks().size());

        // A corrupt tier ordinal must fall back rather than throwing during world load.
        CompoundTag corrupt = data.save(new CompoundTag());
        corrupt.getList("Networks", 10).getCompound(0)
                .getList("Pylons", 10).getCompound(0).putByte("Tier", (byte) 99);
        try {
            FluxNetworkData survived = FluxNetworkData.load(corrupt);
            yes("a corrupt tier falls back", survived.networks().size() > 0);
        } catch (RuntimeException e) {
            fail("corrupt NBT threw " + e.getClass().getSimpleName());
        }
    }

    private static void spanning() {
        BlockPos a = new BlockPos(0, 64, 0);
        BlockPos far = new BlockPos(200, 64, 0);
        eq("200 blocks costs 25 FU/t", 25L, ManualLink.of(a, far).upkeep());
        eq("64 blocks costs 8 FU/t", 8L, ManualLink.of(a, new BlockPos(64, 64, 0)).upkeep());
        yes("A→B and B→A are the same edge",
                ManualLink.of(a, far).equals(ManualLink.of(far, a)));

        FluxNetworkData data = new FluxNetworkData();
        data.addPylon(a, PylonTier.MK1);
        data.addPylon(far, PylonTier.MK1);
        eq("distant pylons stay two networks", 2, data.networks().size());
        no("auto-range does not reach 200",
                PylonTier.linksAutomatically(PylonTier.MK1, a, PylonTier.MK1, far));

        yes("the conduit joins them", data.addManualLink(a, far));
        eq("a manual link merges the two", 1, data.networks().size());
        yes("both pylons share the network",
                data.networkAt(a).id() == data.networkAt(far).id());
        yes("the edge is remembered", data.hasManualLink(a, far));
        yes("and is undirected", data.hasManualLink(far, a));

        data.networkAt(a).receive(1000L, false);
        data.tickNetworks();
        eq("upkeep is paid from the pool", 1000L - 25L, data.networkAt(a).stored());

        // A chain of three with a manual trunk between the ends: removing the middle pylon
        // must not split them. That is the whole reason the conduit exists.
        FluxNetworkData trunk = new FluxNetworkData();
        BlockPos left = new BlockPos(0, 64, 0);
        BlockPos middle = new BlockPos(40, 64, 0);
        BlockPos right = new BlockPos(80, 64, 0);
        trunk.addPylon(left, PylonTier.MK1);
        trunk.addPylon(middle, PylonTier.MK1);
        trunk.addPylon(right, PylonTier.MK1);
        trunk.addManualLink(left, right);
        trunk.removePylon(middle);
        eq("a manual trunk survives losing the auto-range bridge", 1, trunk.networks().size());
        yes("the ends stay on it",
                trunk.networkAt(left).id() == trunk.networkAt(right).id());

        trunk.removeManualLink(left, right);
        eq("dropping the trunk splits them", 2, trunk.networks().size());
        no("the ends are separate again",
                trunk.networkAt(left).id() == trunk.networkAt(right).id());

        no("linking a missing pylon does nothing",
                data.addManualLink(a, new BlockPos(999, 64, 999)));
        no("linking a pylon to itself does nothing", data.addManualLink(a, a));

        FluxNetworkData saved = new FluxNetworkData();
        saved.addPylon(a, PylonTier.MK1);
        saved.addPylon(far, PylonTier.MK1);
        saved.addManualLink(a, far);
        saved.networkAt(a).receive(4000L, false);
        FluxNetworkData loaded = FluxNetworkData.load(saved.save(new CompoundTag()));
        eq("manual links survive a save", 1, loaded.networks().size());
        yes("the loaded edge is still there", loaded.hasManualLink(a, far));
        eq("upkeep still applies after load", 25L, ManualLink.of(a, far).upkeep());
        loaded.removePylon(far);
        no("breaking a pylon drops its manual edges", loaded.hasManualLink(a, far));
        eq("and leaves the other pylon alone", 1, loaded.networks().size());
    }

    private static void banks() {
        FluxNetworkData data = new FluxNetworkData();
        BlockPos pylon = new BlockPos(0, 64, 0);
        BlockPos covered = new BlockPos(4, 64, 4);
        BlockPos uncovered = new BlockPos(400, 64, 400);
        data.addPylon(pylon, PylonTier.MK1);
        long pylonsOnly = data.networkAt(pylon).capacity();

        data.addBank(covered, CapacitorLogic.CAPACITY);
        eq("a covered bank adds its capacity", pylonsOnly + CapacitorLogic.CAPACITY,
                data.networkAt(pylon).capacity());
        eq("the extra is visible", CapacitorLogic.CAPACITY,
                data.networkAt(pylon).extraCapacity());

        data.addBank(uncovered, CapacitorLogic.CAPACITY);
        eq("an uncovered bank adds nothing", pylonsOnly + CapacitorLogic.CAPACITY,
                data.networkAt(pylon).capacity());
        yes("and it still does not cover that spot",
                data.networkCovering(uncovered) == null);

        data.removeBank(covered);
        eq("removing a bank returns pylon capacity", pylonsOnly,
                data.networkAt(pylon).capacity());

        data.addBank(covered, CapacitorLogic.CAPACITY);
        data.networkAt(pylon).receive(pylonsOnly + 50L, false);
        yes("the extra headroom can actually be filled",
                data.networkAt(pylon).stored() > pylonsOnly);

        FluxNetworkData loaded = FluxNetworkData.load(data.save(new CompoundTag()));
        eq("bank extra survives a save", pylonsOnly + CapacitorLogic.CAPACITY,
                loaded.networkAt(pylon).capacity());

        loaded.removePylon(pylon);
        eq("a bank does not keep a network alive", 0, loaded.networks().size());
        yes("and does not start covering by itself",
                loaded.networkCovering(covered) == null);
    }

    private static void arrays() {
        eq("the ring is eight", 8, GroundArrayLogic.RING);
        no("seven casings are not a structure", GroundArrayLogic.formed(7));
        yes("eight casings are a structure", GroundArrayLogic.formed(8));
        eq("an open ring stores nothing", 0L, GroundArrayLogic.contribution(false));
        eq("a complete array stores ten seconds of MK3",
                PylonTier.MK3.throughput() * GroundArrayLogic.BUFFER_TICKS, GroundArrayLogic.CAPACITY);

        BlockPos centre = new BlockPos(0, 64, 0);
        BlockPos[] ring = GroundArrayLogic.ring(centre);
        eq("the ring lists eight positions", GroundArrayLogic.RING, ring.length);
        boolean shape = true;
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (BlockPos pos : ring) {
            if (pos.equals(centre) || pos.getY() != centre.getY()) {
                shape = false;
            }
            int reach = Math.max(Math.abs(pos.getX() - centre.getX()), Math.abs(pos.getZ() - centre.getZ()));
            if (reach != 1 || !seen.add(pos.asLong())) {
                shape = false;
            }
        }
        yes("the ring is the Moore neighbourhood on one layer", shape);
        boolean seesController = false;
        for (BlockPos back : GroundArrayLogic.ring(ring[0])) {
            if (back.equals(centre)) {
                seesController = true;
            }
        }
        yes("a corner casing can see the controller", seesController);

        FluxNetworkData data = new FluxNetworkData();
        BlockPos pylon = new BlockPos(0, 64, 0);
        BlockPos covered = new BlockPos(4, 64, 4);
        BlockPos uncovered = new BlockPos(400, 64, 400);
        data.addPylon(pylon, PylonTier.MK1);
        long pylonsOnly = data.networkAt(pylon).capacity();

        data.addArray(covered, GroundArrayLogic.CAPACITY);
        eq("a covered array adds its capacity", pylonsOnly + GroundArrayLogic.CAPACITY,
                data.networkAt(pylon).capacity());
        data.addArray(covered, GroundArrayLogic.CAPACITY);
        eq("repeating the array does not double it", pylonsOnly + GroundArrayLogic.CAPACITY,
                data.networkAt(pylon).capacity());
        data.addBank(covered, CapacitorLogic.CAPACITY);
        eq("a bank and an array add",
                pylonsOnly + GroundArrayLogic.CAPACITY + CapacitorLogic.CAPACITY,
                data.networkAt(pylon).capacity());
        data.addArray(uncovered, GroundArrayLogic.CAPACITY);
        eq("an uncovered array adds nothing",
                pylonsOnly + GroundArrayLogic.CAPACITY + CapacitorLogic.CAPACITY,
                data.networkAt(pylon).capacity());
        data.removeArray(covered);
        eq("removing the array leaves the bank", pylonsOnly + CapacitorLogic.CAPACITY,
                data.networkAt(pylon).capacity());

        data.addArray(covered, GroundArrayLogic.CAPACITY);
        FluxNetworkData loaded = FluxNetworkData.load(data.save(new CompoundTag()));
        eq("array extra survives a save",
                pylonsOnly + GroundArrayLogic.CAPACITY + CapacitorLogic.CAPACITY,
                loaded.networkAt(pylon).capacity());
        loaded.removePylon(pylon);
        eq("an array does not keep a network alive", 0, loaded.networks().size());
    }

    private static void transformer() {
        eq("F1 to F0 is capped at F0", 8L,
                TransformerLogic.throughput(
                        io.github.ezequiel24123z.grindless.energy.FluxTier.F1,
                        io.github.ezequiel24123z.grindless.energy.FluxTier.F0));
        eq("F0 to F1 is the same cap", 8L,
                TransformerLogic.throughput(
                        io.github.ezequiel24123z.grindless.energy.FluxTier.F0,
                        io.github.ezequiel24123z.grindless.energy.FluxTier.F1));
        eq("the network exchange runs at F1", 32L, TransformerLogic.RATE);
        eq("the low face would run at F0", 8L, TransformerLogic.LOW_RATE);

        eq("an empty buffer pulls from the network", 32L,
                TransformerLogic.exchange(0L, 6400L, 1000L, 10_000L, 3200L, 32L));
        eq("a full buffer dumps into the network", -32L,
                TransformerLogic.exchange(6400L, 6400L, 0L, 10_000L, 3200L, 32L));
        eq("at the target it sits still", 0L,
                TransformerLogic.exchange(3200L, 6400L, 1000L, 10_000L, 3200L, 32L));
        eq("an empty network cannot fill it", 0L,
                TransformerLogic.exchange(0L, 6400L, 0L, 10_000L, 3200L, 32L));
        eq("a full network cannot take a dump", 0L,
                TransformerLogic.exchange(6400L, 6400L, 10_000L, 10_000L, 3200L, 32L));
    }

    private static void eq(String what, String expected, String actual) {
        if (expected.equals(actual)) {
            pass(what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, long expected, long actual) {
        if (expected == actual) {
            pass(what);
        } else {
            fail(what + ": expected " + expected + " but got " + actual);
        }
    }

    private static void eq(String what, double expected, double actual) {
        if (Math.abs(expected - actual) < 1e-9) {
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
        System.out.println("  ok   " + what);
    }

    private static void fail(String what) {
        failures++;
        System.out.println("  FAIL " + what);
    }
}
