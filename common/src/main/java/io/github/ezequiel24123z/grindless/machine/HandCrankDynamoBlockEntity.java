package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.container.NeighbourCache;
import io.github.ezequiel24123z.grindless.energy.FluxStorage;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Hand Crank Dynamo: F0, 8 FU/t, and the first power in the game.
 *
 * <p>Eight FU per tick runs the Crude Extractor and nothing else, which makes the player's first
 * real decision what to power. That is the whole game in miniature, and it is why T0 generation is
 * deliberately this small.
 *
 * <p>It is also the simplest possible demonstration of the tick model (ADR-0042). Cranking stores
 * a fixed charge; the dynamo then pushes that charge to its neighbours and <b>unsubscribes the
 * moment it runs dry</b>. A dynamo nobody has cranked costs nothing at all.
 */
public final class HandCrankDynamoBlockEntity extends MachineBlockEntity {

    private static final String KEY_CHARGE = "Charge";

    /** One crank's worth of Flux: five seconds of output, so cranking is a deliberate action
     * rather than a thing you hold down. */
    public static final long CHARGE_PER_CRANK = FluxTier.F0.nominal() * 20L * 5L;

    /** Pushing is throttled rather than run every tick. At 8 FU/t the difference is invisible in
     * play and it is five times less work. */
    private static final int PUSH_PERIOD = 5;

    private final Map<Direction, NeighbourCache> neighbours = new EnumMap<>(Direction.class);

    private long charge;
    private TickSubscription pushing;

    public HandCrankDynamoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HAND_CRANK_DYNAMO.get(), pos, state);
    }

    @Override
    protected ConditionEnvelope narrowEnvelope() {
        // A dynamo runs no processes, so it holds no conditions. Its envelope is ambient, and
        // it is the same at every mark because there is nothing to widen.
        return ConditionEnvelope.builder().build();
    }

    @Override
    protected ConditionEnvelope fullEnvelope() {
        return narrowEnvelope();
    }

    @Override
    protected FluxTier ratedTier() {
        return FluxTier.F0;
    }

    /** Charge remaining, in FU. */
    public long charge() {
        return charge;
    }

    /**
     * Cranks the dynamo once.
     *
     * <p>Charge accumulates rather than resetting, so cranking twice is worth twice as much and a
     * player is never punished for topping it up early.
     */
    public void crank() {
        charge += CHARGE_PER_CRANK;
        setChanged();
        // The crank is exactly the kind of state change the subscription model needs told about:
        // it is what makes pushing worth doing, and nothing else would notice it.
        updateSubscriptions();
    }

    @Override
    protected void updateSubscriptions() {
        if (charge > 0L && getLevel() != null) {
            pushing = subscriptions().subscribe(pushing, this::push);
        } else if (pushing != null) {
            pushing.unsubscribe();
            pushing = null;
        }
    }

    private void push() {
        if (!isDue(PUSH_PERIOD)) {
            return;
        }
        // Output is rated per tick, so a throttled push moves a whole period's worth at once.
        // That keeps the dynamo at exactly 8 FU/t however often this actually runs.
        long allowance = Math.min(charge, FluxTier.F0.nominal() * PUSH_PERIOD);
        long budget = allowance;
        for (Direction side : Direction.values()) {
            if (budget <= 0L) {
                break;
            }
            if (!containerConfig().allowsExtract(side)) {
                continue;
            }
            FluxStorage target = neighbours
                    .computeIfAbsent(side, s -> new NeighbourCache(getLevel(), getBlockPos(), s))
                    .get();
            if (target == null || !target.canReceive()) {
                continue;
            }
            budget -= target.receive(budget, false);
        }
        long spent = allowance - budget;
        if (spent > 0L) {
            charge -= spent;
            setChanged();
        }
        // Re-evaluate every time, so running dry unsubscribes rather than leaving the dynamo
        // ticking forever over neighbours that are already full.
        updateSubscriptions();
    }

    /**
     * Drops cached neighbours when a bordering block changes.
     *
     * <p>Capability invalidation covers a neighbour being broken, but cannot cover one
     * <em>appearing</em> — there was nothing there to invalidate — so this path is required as
     * well (ADR-0044).
     */
    public void onNeighbourChanged() {
        neighbours.values().forEach(NeighbourCache::invalidate);
        updateSubscriptions();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong(KEY_CHARGE, charge);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        charge = Math.max(0L, tag.getLong(KEY_CHARGE));
    }
}
