package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.container.ContainerConfig;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.energy.SimpleFluxStorage;
import io.github.ezequiel24123z.grindless.network.FluxNetwork;
import io.github.ezequiel24123z.grindless.network.FluxNetworkData;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.process.ConditionState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The base every Grindless machine is built on.
 *
 * <p>Brings together the layers written separately: a Flux buffer, a chassis mark and its upgrades,
 * the condition state the machine is currently holding, the shared container contract, and the tick
 * subscriptions that decide whether any of it costs anything this tick.
 *
 * <h2>Ticking</h2>
 *
 * <p>This class is registered as a ticking block entity, but {@link #serverTick} is close to free
 * when there is nothing to do: it runs the subscription list, and an idle machine's list is empty
 * (ADR-0042). Subclasses must never override it to add unconditional per-tick work — that is the
 * exact cost the design exists to avoid. Subscribe the work instead, from
 * {@link #updateSubscriptions()}.
 *
 * <h2>The rule subclasses must follow</h2>
 *
 * <p>Anything that can change whether work is needed must call {@link #updateSubscriptions()}: a
 * buffer gaining its first item, a side being reconfigured, a neighbour appearing, the machine
 * being enabled. A machine that forgets is silently broken and very hard to diagnose, because
 * doing nothing is also what it does when it is working correctly.
 *
 * <h2>Vanilla only</h2>
 *
 * <p>This class lives in {@code common}, so it may only override methods vanilla actually
 * declares. {@code onLoad()} in particular looks like the obvious place to initialise and is a
 * <em>Forge addition</em> — using it compiles on Forge and would not exist for any other loader,
 * which is exactly the kind of leak {@code @ExpectPlatform} exists to prevent (ADR-0001).
 */
public abstract class MachineBlockEntity extends BlockEntity {

    private static final String KEY_ENERGY = "Energy";
    private static final String KEY_MARK = "ChassisMark";
    private static final String KEY_CONFIG = "Config";
    private static final String KEY_UPGRADES = "Upgrades";
    private static final String KEY_CONDITIONS = "Conditions";

    private final TickSubscriptions subscriptions = new TickSubscriptions();
    private final ContainerConfig containerConfig = new ContainerConfig();
    private final int tickOffset;

    /**
     * Never replaced after construction.
     *
     * <p>That is load-bearing rather than tidiness: Forge hands this buffer out as a capability,
     * and anything holding it — a neighbouring machine's cache, a pipe from another mod — would
     * keep pointing at the discarded object if it were ever swapped. Mutate it, never reassign it.
     */
    private final SimpleFluxStorage energy;

    private ChassisMark mark = ChassisMark.MK_I;
    private UpgradeSet upgrades = UpgradeSet.EMPTY;
    private ConditionState conditions = ConditionState.AMBIENT;
    private boolean initialised;

    /** Set by a Logic Controller. Not saved: the controller is the source of truth (ADR-0083). */
    private boolean logisticsHold;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.tickOffset = TickOffset.forPosition(pos);
        this.energy = createEnergyBuffer();
        // Configuration is a subscription input as much as contents are, so a face being
        // closed or a target changed has to re-evaluate what this machine needs to run.
        containerConfig.listeners().add(this::updateSubscriptions);
    }

    // ---- What a machine is ----------------------------------------------------------------

    /**
     * This machine's envelope at its narrowest, the MK I capability.
     *
     * <p>Together with {@link #fullEnvelope()} this defines the machine's whole mark ladder, since
     * intermediate marks interpolate between the two (ADR-0027).
     */
    protected abstract ConditionEnvelope narrowEnvelope();

    /** This machine's envelope at MK V, its physical ceiling. */
    protected abstract ConditionEnvelope fullEnvelope();

    /**
     * The Flux tier this machine is rated for. Under-volting slows it rather than stalling it.
     *
     * <p><b>Must return a constant.</b> This is called from the constructor to size the energy
     * buffer, which means it runs <em>before</em> the subclass constructor body — so an
     * implementation that reads a subclass field will see that field's default value, not the
     * one it was about to be assigned. Returning an enum constant is always safe; returning
     * anything derived from instance state is not.
     */
    protected abstract FluxTier ratedTier();

    /** Seconds of rated throughput the buffer holds. Two is enough to ride out a brief shortfall
     * without turning every machine into a battery. */
    protected int bufferSeconds() {
        return 2;
    }

    /** The conditions this machine can currently hold, which is its mark's widening of the
     * machine's own range. */
    public ConditionEnvelope envelope() {
        return mark.widen(narrowEnvelope(), fullEnvelope());
    }

    // ---- State ----------------------------------------------------------------------------

    public ChassisMark chassisMark() {
        return mark;
    }

    /**
     * Applies a chassis upgrade kit, in place.
     *
     * <p>The machine keeps its position, contents, configuration and connections; nothing is ever
     * rebuilt or re-piped. The energy buffer is untouched — a mark widens the <em>condition
     * envelope</em> (ADR-0027), and the machine's own power rating is a property of the machine
     * rather than of its chassis.
     */
    public void setChassisMark(ChassisMark newMark) {
        if (this.mark == newMark) {
            return;
        }
        this.mark = newMark;
        setChanged();
        updateSubscriptions();
    }

    public UpgradeSet upgrades() {
        return upgrades;
    }

    /**
     * Replaces the installed upgrades.
     *
     * <p>Subclasses holding a cached recipe must drop it here, not only when inputs change: an
     * upgrade can alter cycle time and parallelism, and a machine that keeps running its
     * previously-resolved recipe after being retuned is a genuinely confusing bug (ADR-0043).
     */
    public void setUpgrades(UpgradeSet newUpgrades) {
        this.upgrades = newUpgrades;
        setChanged();
        updateSubscriptions();
    }

    public SimpleFluxStorage energy() {
        return energy;
    }

    /** Whether a Logic Controller is holding this machine idle (ADR-0083). */
    public boolean logisticsHold() {
        return logisticsHold;
    }

    /**
     * Holds or releases this machine. A hold publishes idle. Releasing lets the next tick
     * show the real status. The flag is not written to NBT.
     */
    public void setLogisticsHold(boolean hold) {
        if (this.logisticsHold == hold) {
            return;
        }
        this.logisticsHold = hold;
        if (hold && getLevel() != null) {
            MachineProperties.publish(getLevel(), getBlockPos(), MachineStatus.IDLE);
        }
    }

    public ContainerConfig containerConfig() {
        return containerConfig;
    }

    /** What this machine is currently holding, as opposed to what it could. */
    public ConditionState conditions() {
        return conditions;
    }

    protected void setConditions(ConditionState newConditions) {
        this.conditions = newConditions;
        setChanged();
        updateSubscriptions();
    }

    /** How fast this machine runs, before conditions are applied. Below 1.0 is faster. */
    public double timeMultiplier() {
        return upgrades.timeMultiplier();
    }

    // ---- Power ----------------------------------------------------------------------------

    /**
     * The Flux Network supplying this machine, or {@code null} if no pylon covers it.
     *
     * <p>A machine is powered because it stands inside a supply area, full stop — there are no
     * wires, no per-face connections and no cable loss. The lookup is an index hit rather than a
     * graph walk (ADR-0046), but it is still not free, so callers should ask once per tick and
     * not once per operation.
     */
    public FluxNetwork network() {
        return getLevel() instanceof ServerLevel server
                ? FluxNetworkData.get(server).networkCovering(getBlockPos())
                : null;
    }

    /**
     * Declares this machine's intended draw for the coming tick.
     *
     * <p>Half of the two-phase brownout (ADR-0046): every machine declares first, the network
     * works out one satisfaction fraction, and only then does anyone draw. Drawing directly
     * instead would hand full power to whichever machines happen to tick first and starve the
     * rest, which is the failure mode the design rejects.
     */
    protected void requestPower(long amount) {
        FluxNetwork network = network();
        if (network != null) {
            network.registerDemand(amount);
        }
    }

    /**
     * Draws up to {@code amount}, scaled by how much of the network's demand can be met.
     *
     * <p>Returns what was actually drawn, which may be less than asked for during a brownout.
     * Callers scale their progress by the shortfall rather than stalling: a machine on an
     * overloaded network runs slowly and visibly, never stopping with no explanation.
     */
    protected long drawPower(long amount) {
        FluxNetwork network = network();
        if (network == null || amount <= 0L) {
            return 0L;
        }
        long share = Math.round(amount * network.satisfaction());
        return network.extract(share, false);
    }

    // ---- Ticking --------------------------------------------------------------------------

    protected TickSubscriptions subscriptions() {
        return subscriptions;
    }

    /** A stable per-machine offset, so throttled work spreads across ticks instead of landing on
     * all of them at once. */
    protected int tickOffset() {
        return tickOffset;
    }

    /** Whether throttled work is due this tick. */
    protected boolean isDue(int period) {
        Level level = getLevel();
        return level != null && TickOffset.isDue(level.getGameTime(), tickOffset, period);
    }

    /**
     * Runs once, on this machine's first server tick, before subscriptions are first evaluated.
     *
     * <p>The place for setup that needs a loaded level and loaded neighbours — rejoining a
     * network, resolving a neighbour, reading the block state — none of which is reliable in the
     * constructor or during {@code load}. Vanilla offers no load-completed hook that
     * {@code common} may use, so this is it.
     */
    protected void onFirstTick() {
    }

    /**
     * Re-evaluates which work this machine needs running.
     *
     * <p>Called whenever an input to that decision changes. Implementations subscribe work when it
     * becomes necessary and unsubscribe it when it is not; both halves matter, since a machine
     * that only ever subscribes never goes idle and the whole model is pointless.
     */
    protected void updateSubscriptions() {
    }

    /** Runs this machine's subscribed work. Not for overriding — subscribe work instead. */
    public final void serverTick() {
        if (!initialised) {
            initialised = true;
            // Deferred to the first tick rather than done on load. Two reasons: vanilla has no
            // load-completed hook that `common` can use — `onLoad` is a Forge addition — and the
            // decision usually needs neighbours, which are not reliably available while the chunk
            // is still loading. By the first tick both are true.
            onFirstTick();
            updateSubscriptions();
        }
        if (logisticsHold) {
            return;
        }
        subscriptions.tick();
    }

    /** The ticker to hand to {@code BlockEntityType}. Server-side only; machines have no
     * client-side logic beyond rendering, which is driven by synced state rather than by ticking. */
    public static <T extends MachineBlockEntity> void tick(Level level, BlockPos pos,
                                                           BlockState state, T machine) {
        if (!level.isClientSide()) {
            machine.serverTick();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Listeners hold a reference to this block entity, so leaving them registered keeps a
        // removed machine reachable. That presents as a chunk that will not unload.
        subscriptions.clear();
        containerConfig.listeners().clear();
    }

    // ---- Persistence ----------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(KEY_ENERGY, energy.save(new CompoundTag()));
        tag.putByte(KEY_MARK, (byte) mark.ordinal());
        tag.put(KEY_CONFIG, containerConfig.save(new CompoundTag()));
        tag.put(KEY_UPGRADES, upgrades.save(new CompoundTag()));
        tag.put(KEY_CONDITIONS, conditions.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // Bounds-checked: an out-of-range ordinal from a future version would otherwise throw
        // during world load, which a player sees as a corrupt save rather than as a mod bug.
        byte ordinal = tag.getByte(KEY_MARK);
        ChassisMark[] marks = ChassisMark.values();
        this.mark = ordinal >= 0 && ordinal < marks.length ? marks[ordinal] : ChassisMark.MK_I;
        this.energy.load(tag.getCompound(KEY_ENERGY));
        this.upgrades = UpgradeSet.load(tag.getCompound(KEY_UPGRADES));
        this.conditions = tag.contains(KEY_CONDITIONS)
                ? ConditionState.load(tag.getCompound(KEY_CONDITIONS))
                : ConditionState.AMBIENT;
        // Note: this fires the config's listeners, so updateSubscriptions() runs here on a
        // subclass that is only partly loaded — a subclass field read now holds its default,
        // not its saved value. That is survivable rather than correct by accident: the first
        // serverTick re-evaluates subscriptions with everything in place. Subclasses must
        // therefore not rely on this call having seen their state.
        containerConfig.load(tag.getCompound(KEY_CONFIG));
    }

    /**
     * Builds this machine's energy buffer.
     *
     * <p>The default is a <em>load</em>: it accepts power at its rated tier and never gives any
     * back, because a machine is not a battery and should not quietly become one when a pipe is
     * pointed at it.
     *
     * <p>Generators override this to invert it — no external insertion, extraction at the rated
     * tier — so that the buffer they fill is the same object the rest of the world pulls from.
     * Keeping generated power in a separate field would mean exposing an energy capability that
     * silently swallows anything inserted and never hands out what the machine actually made.
     *
     * <p>Called from the constructor, so it must not read subclass fields. See {@link #ratedTier}.
     */
    protected SimpleFluxStorage createEnergyBuffer() {
        FluxTier tier = ratedTier();
        return new SimpleFluxStorage(bufferCapacity(), tier.nominal(), 0L, this::setChanged);
    }

    /** Buffer size in FU, from the rated tier and {@link #bufferSeconds()}. */
    protected final long bufferCapacity() {
        return ratedTier().nominal() * 20L * bufferSeconds();
    }
}
