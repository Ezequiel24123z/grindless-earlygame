package io.github.ezequiel24123z.grindless.machine;

import io.github.ezequiel24123z.grindless.container.NeighbourCache;
import io.github.ezequiel24123z.grindless.energy.FluxStorage;
import io.github.ezequiel24123z.grindless.energy.FluxTier;
import io.github.ezequiel24123z.grindless.network.FluxNetwork;
import io.github.ezequiel24123z.grindless.process.ConditionEnvelope;
import io.github.ezequiel24123z.grindless.recipe.SolarLogic;
import io.github.ezequiel24123z.grindless.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Map;

/**
 * Daylight at F1. Stays subscribed: dawn does not tell the panel, and a draining
 * buffer does not either (ADR-0081).
 */
public final class SolarArrayBlockEntity extends MachineBlockEntity {

    private final StatusDebounce display = new StatusDebounce();
    private final Map<Direction, NeighbourCache> neighbours = new EnumMap<>(Direction.class);
    private TickSubscription working;
    private int fruitlessPushes;

    public SolarArrayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_ARRAY.get(), pos, state);
    }

    @Override
    protected ConditionEnvelope narrowEnvelope() {
        return ConditionEnvelope.builder().build();
    }

    @Override
    protected ConditionEnvelope fullEnvelope() {
        return narrowEnvelope();
    }

    @Override
    protected FluxTier ratedTier() {
        return FluxTier.F1;
    }

    @Override
    protected void updateSubscriptions() {
        if (getLevel() == null) {
            return;
        }
        working = subscriptions().subscribe(working, this::work);
    }

    public void onNeighbourChanged() {
        neighbours.values().forEach(NeighbourCache::invalidate);
        fruitlessPushes = 0;
        updateSubscriptions();
    }

    private void work() {
        boolean day = daylight();
        if (day) {
            energy().setStored(energy().getStored() + SolarLogic.generate(true));
        }
        push();
        setChanged();
        publish(day);
    }

    /** The panel must not shadow itself, so the check is the block above. */
    private boolean daylight() {
        return getLevel() != null && getLevel().isDay() && getLevel().canSeeSky(getBlockPos().above());
    }

    private void push() {
        long allowance = Math.min(energy().getStored(), FluxTier.F1.nominal());
        long budget = allowance;
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
        long spent = allowance - budget;
        if (spent > 0L) {
            energy().setStored(energy().getStored() - spent);
            fruitlessPushes = 0;
        } else if (allowance > 0L) {
            fruitlessPushes++;
        }
    }

    private void publish(boolean day) {
        if (getLevel() == null || getLevel().isClientSide()) {
            return;
        }
        MachineStatus observed = SolarLogic.status(day, energy().getStored(), fruitlessPushes);
        if (display.observe(observed)) {
            MachineProperties.publish(getLevel(), getBlockPos(), display.shown());
        }
    }

    @Override
    public void setRemoved() {
        neighbours.values().forEach(NeighbourCache::invalidate);
        super.setRemoved();
    }
}
