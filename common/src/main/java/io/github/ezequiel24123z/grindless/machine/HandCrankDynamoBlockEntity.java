package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.container.NeighbourCache;
import io.github.ezequiel24123z.grindless.energy.FluxStorage;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.energy.SimpleFluxStorage;
import io.github.ezequiel24123z.grindless.network.FluxNetwork;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    /** One crank's worth of Flux: five seconds of output, so cranking is a deliberate action
     * rather than a thing you hold down. */
    public static final long CHARGE_PER_CRANK = FluxTier.F0.nominal() * 20L * 5L;

    /**
     * Push every tick while charged. A five-tick burst of 40 FU empties the network for the
     * ticks in between, and a consumer then observes STARVED often enough that
     * {@link StatusDebounce} never recovers to running — the machine is working, the face says
     * it is not. Eight FU per tick fills the pool as fast as one Crude Extractor drains it, so
     * the face and the work agree.
     */
    private static final int PUSH_PERIOD = 1;

    private final Map<Direction, NeighbourCache> neighbours = new EnumMap<>(Direction.class);

    private TickSubscription pushing;

    /** Consecutive pushes that moved nothing; drives {@link PushBackoff}. Not saved: it only
     * paces polling, and a reloaded dynamo is entitled to a fresh start. */
    private int fruitlessPushes;

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

    /** Two cranks' worth, so topping up before the buffer is empty is never wasted. */
    @Override
    protected int bufferSeconds() {
        return 10;
    }

    /**
     * A generator buffer: nothing may be inserted from outside, and the rated output may be
     * drawn from it.
     *
     * <p>This is the same object the energy capability exposes, so a pipe from another mod pulls
     * the power this dynamo actually made rather than finding an empty load.
     */
    @Override
    protected SimpleFluxStorage createEnergyBuffer() {
        return new SimpleFluxStorage(bufferCapacity(), 0L, FluxTier.F0.nominal(), this::setChanged);
    }

    /** Charge remaining, in FU. */
    public long charge() {
        return energy().getStored();
    }

    /**
     * Cranks the dynamo once.
     *
     * <p>Charge accumulates rather than resetting, so cranking twice is worth twice as much and a
     * player is never punished for topping it up early. It saturates at the buffer's capacity.
     *
     * <p>Written with {@code setStored} rather than {@code receive} deliberately: this is power
     * being <em>generated inside</em> the machine, not transferred into it, and the buffer refuses
     * external insertion by design.
     */
    public void crank() {
        energy().setStored(energy().getStored() + CHARGE_PER_CRANK);
        // The crank is exactly the kind of state change the subscription model needs told about:
        // it is what makes pushing worth doing, and nothing else would notice it.
        fruitlessPushes = 0;
        updateSubscriptions();
        publishStatus();
    }

    /** Shows what the dynamo is doing on its block. Cheap when nothing changed. */
    private void publishStatus() {
        if (getLevel() != null && !getLevel().isClientSide()) {
            MachineProperties.publish(getLevel(), getBlockPos(),
                    DynamoStatus.of(energy().getStored(), fruitlessPushes));
        }
    }

    @Override
    protected void updateSubscriptions() {
        if (energy().getStored() > 0L && getLevel() != null) {
            pushing = subscriptions().subscribe(pushing, this::push);
        } else if (pushing != null) {
            pushing.unsubscribe();
            pushing = null;
        }
    }

    private void push() {
        if (!isDue(PushBackoff.period(PUSH_PERIOD, fruitlessPushes))) {
            return;
        }
        // Output is rated per tick, so a push covering several ticks (PushBackoff stretching the
        // period when nobody is listening) moves that many ticks' worth at once: the dynamo still
        // averages 8 FU/t.
        //
        // Read from the buffer directly rather than through extract(). The buffer's rate limit
        // governs what an *external* puller may take in one operation, and applying it here would
        // cap a multi-tick push at one tick's worth — quietly running the dynamo slow. This is
        // the machine's own output path, and it implements the rating itself.
        long allowance = Math.min(energy().getStored(), FluxTier.F0.nominal() * PUSH_PERIOD);
        long budget = allowance;

        // The Flux Network first. A dynamo standing inside a pylon's supply area feeds the whole
        // grid rather than only the block it happens to touch, which is the entire point of
        // area distribution — no wires, no per-face connections, no cable to forget.
        FluxNetwork network = network();
        if (network != null) {
            budget -= network.receive(budget, false);
        }

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
        // Only what a neighbour actually accepted is removed. Taking the allowance up front and
        // refunding the remainder is the version that loses power on a rounding edge.
        long spent = allowance - budget;
        if (spent > 0L) {
            energy().setStored(energy().getStored() - spent);
            fruitlessPushes = 0;
        } else {
            fruitlessPushes++;
        }
        // Running dry unsubscribes. Having charge but nobody to give it to does not: a full
        // neighbour draining raises no event, so the dynamo keeps polling, at the pace PushBackoff
        // allows, rather than sleeping through the moment a sink appears.
        updateSubscriptions();
        publishStatus();
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
        fruitlessPushes = 0;
        updateSubscriptions();
        publishStatus();
    }

    /**
     * Unregisters from every neighbour on the way out. A neighbour's capability outlives this
     * block entity, so a listener left on it would keep a removed dynamo reachable.
     */
    @Override
    public void setRemoved() {
        neighbours.values().forEach(NeighbourCache::invalidate);
        super.setRemoved();
    }

    // No persistence of its own: the charge *is* the energy buffer, which the base class already
    // saves and loads. That is the point of unifying the two — one piece of state, saved once.
}
