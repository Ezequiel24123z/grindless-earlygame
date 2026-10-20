package io.github.ezequiel24123z.grindless.client.audio;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.fluid.HandPumpBlock;
import io.github.ezequiel24123z.grindless.fluid.HandPumpBlockEntity;
import io.github.ezequiel24123z.grindless.machine.CrudeExtractorBlock;
import io.github.ezequiel24123z.grindless.machine.CrudeExtractorBlockEntity;
import io.github.ezequiel24123z.grindless.machine.HandCrankDynamoBlock;
import io.github.ezequiel24123z.grindless.machine.HandCrankDynamoBlockEntity;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlock;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineKind;
import io.github.ezequiel24123z.grindless.registry.ModBlocks;
import io.github.ezequiel24123z.grindless.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/** Physical working beds and sparse state-edge feedback, owned by the current client. */
public final class T0MachineAudio {

    private final T0SoundscapePolicy<BlockEntity> feedback = new T0SoundscapePolicy<>();
    private WeakReference<Object> onsetWorld = new WeakReference<>(null);
    private WeakReference<Object> onsetPlayer = new WeakReference<>(null);
    private final QuernLoopTracker<BlockEntity, MachineSound> loops =
            new QuernLoopTracker<>(new QuernLoopTracker.Playback<>() {
                @Override
                public MachineSound start(long position, BlockEntity source) {
                    // Admission rechecks mutable status; constructing a voice captures only its stable mechanism.
                    MachineSound sound = new MachineSound(position, source, familyFor(source));
                    Minecraft.getInstance().getSoundManager().play(sound);
                    return sound;
                }

                @Override
                public void stop(MachineSound sound) {
                    sound.retire();
                    Minecraft.getInstance().getSoundManager().stop(sound);
                }

                @Override
                public boolean isActive(MachineSound sound) {
                    return Minecraft.getInstance().getSoundManager().isActive(sound);
                }

                @Override
                public void notAdmitted(long position) {
                    Grindless.LOG.debug("T0 machine audio at {} was not admitted; suppressing repeats until "
                            + "eligibility changes or resources reload", BlockPos.of(position));
                }
            });

    private T0MachineAudio() {
    }

    public static void register() {
        T0MachineAudio audio = new T0MachineAudio();
        ClientTickEvent.CLIENT_POST.register(audio::tick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> audio.clear());
        ClientPlayerEvent.CLIENT_PLAYER_RESPAWN.register((oldPlayer, newPlayer) -> audio.clear());
        ClientLifecycleEvent.CLIENT_STOPPING.register(client -> audio.clear());
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES,
                (ResourceManagerReloadListener) resources -> audio.clear(),
                Grindless.id("t0_machine_audio"));
    }

    private void clear() {
        loops.clear();
        feedback.clear();
        onsetWorld.clear();
        onsetPlayer.clear();
    }

    private void tick(Minecraft client) {
        ClientLevel level = client.level;
        boolean paused = client.isPaused() || client.getOverlay() != null;
        boolean enabled = enabled(client);
        if (onsetWorld.get() != level || onsetPlayer.get() != client.player || !enabled) {
            feedback.clear();
            onsetWorld = new WeakReference<>(level);
            onsetPlayer = new WeakReference<>(client.player);
        }
        if (level == null || client.player == null || !enabled || paused) {
            loops.tick(level, client.player, Map.of(), paused, enabled);
            return;
        }
        Vec3 listener = client.gameRenderer.getMainCamera().getPosition();
        Map<Long, BlockEntity> eligible = new HashMap<>();
        Map<Long, BlockEntity> cranks = new HashMap<>();
        for (int x = QuernLoopTracker.minChunk(listener.x); x <= QuernLoopTracker.maxChunk(listener.x); x++) {
            for (int z = QuernLoopTracker.minChunk(listener.z); z <= QuernLoopTracker.maxChunk(listener.z); z++) {
                LevelChunk chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                for (var blockEntity : chunk.getBlockEntities().values()) {
                    if (!present(level, chunk, blockEntity, listener)) {
                        continue;
                    }
                    var state = chunk.getBlockState(blockEntity.getBlockPos());
                    Family family = familyFor(blockEntity);
                    if (eligible(level, chunk, blockEntity, listener, family)) {
                        eligible.put(blockEntity.getBlockPos().asLong(), blockEntity);
                    }
                    if (blockEntity instanceof HandCrankDynamoBlockEntity
                            && state.is(ModBlocks.HAND_CRANK_DYNAMO.get())
                            && state.getValue(HandCrankDynamoBlock.STATUS) == MachineStatus.RUNNING) {
                        cranks.put(blockEntity.getBlockPos().asLong(), blockEntity);
                    }
                }
            }
        }
        Map<Long, BlockEntity> audible = T0SoundscapePolicy.nearest(eligible,
                machine -> machine.getBlockPos().distToCenterSqr(listener.x, listener.y, listener.z));
        loops.tick(level, client.player, audible, false, true);
        feedback.onsets(cranks).forEach((position, source) -> {
            BlockPos pos = source.getBlockPos();
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ModSounds.RELAY_CLICK.get(), SoundSource.BLOCKS, 0.18F, 1.0F, false);
        });
    }

    private static boolean enabled(Minecraft client) {
        return client.options.getSoundSourceVolume(SoundSource.MASTER) > 0.0F
                && client.options.getSoundSourceVolume(SoundSource.BLOCKS) > 0.0F;
    }

    private static boolean present(ClientLevel level, LevelChunk chunk,
                                   BlockEntity machine, Vec3 listener) {
        BlockPos pos = machine.getBlockPos();
        return !machine.isRemoved() && machine.getLevel() == level
                && chunk.getBlockEntities().get(pos) == machine
                && QuernLoopTracker.inRange(pos.getX() + 0.5 - listener.x,
                        pos.getY() + 0.5 - listener.y, pos.getZ() + 0.5 - listener.z);
    }

    private static boolean eligible(ClientLevel level, LevelChunk chunk,
                                    BlockEntity machine, Vec3 listener, Family family) {
        return family != null && present(level, chunk, machine, listener)
                && familyFor(machine) == family
                && runningBlock(machine, chunk.getBlockState(machine.getBlockPos()))
                && machine.getBlockPos().distToCenterSqr(listener.x, listener.y, listener.z)
                        < family.radius * family.radius;
    }

    private static boolean runningBlock(BlockEntity source, BlockState state) {
        if (source instanceof ProcessMachineBlockEntity machine
                && state.getBlock() instanceof ProcessMachineBlock block
                && block.kind() == machine.kind()) {
            return state.getValue(ProcessMachineBlock.STATUS) == MachineStatus.RUNNING;
        }
        return (source instanceof CrudeExtractorBlockEntity && state.is(ModBlocks.CRUDE_EXTRACTOR.get())
                && state.getValue(CrudeExtractorBlock.STATUS) == MachineStatus.RUNNING)
                || (source instanceof HandPumpBlockEntity && state.is(ModBlocks.HAND_PUMP.get())
                && state.getValue(HandPumpBlock.STATUS) == MachineStatus.RUNNING);
    }

    private static Family familyFor(BlockEntity source) {
        if (source instanceof ProcessMachineBlockEntity machine) {
            return familyFor(machine.kind());
        }
        if (source instanceof CrudeExtractorBlockEntity) {
            return Family.EXTRACTION;
        }
        return source instanceof HandPumpBlockEntity ? Family.WATER_FLOW : null;
    }

    private static Family familyFor(ProcessMachineKind kind) {
        return switch (kind) {
            case QUERN -> Family.GRINDING;
            case CLAY_MIXER -> Family.WET_WORK;
            case SETTLING_TROUGH, GEOLOGICAL_COLLECTOR -> Family.WATER_FLOW;
            default -> null;
        };
    }

    private enum Family {
        GRINDING(ModSounds.T0_GRINDING, 1.0F),
        WET_WORK(ModSounds.T0_WET_WORK, 0.35F),
        WATER_FLOW(ModSounds.T0_WATER_FLOW, 0.25F, 10),
        EXTRACTION(ModSounds.T0_MINERAL_EXTRACTION, 0.3F, 12);

        private final RegistrySupplier<SoundEvent> event;
        private final float volume;
        private final int radius;

        Family(RegistrySupplier<SoundEvent> event, float volume) {
            this(event, volume, QuernLoopTracker.RANGE);
        }

        Family(RegistrySupplier<SoundEvent> event, float volume, int radius) {
            this.event = event;
            this.volume = volume;
            this.radius = radius;
        }
    }

    private static final class MachineSound extends AbstractTickableSoundInstance implements QuernLoopTracker.Voice {
        private final WeakReference<BlockEntity> source;
        private final Family family;
        private long heartbeat;

        private MachineSound(long position, BlockEntity source, Family family) {
            super(family.event.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.source = new WeakReference<>(source);
            this.family = family;
            BlockPos pos = BlockPos.of(position);
            x = pos.getX() + 0.5;
            y = pos.getY() + 0.5;
            z = pos.getZ() + 0.5;
            volume = family.volume;
            pitch = 1.0F;
            looping = true;
            delay = 0;
            relative = false;
            attenuation = SoundInstance.Attenuation.LINEAR;
        }

        @Override
        public boolean canPlaySound() {
            Minecraft client = Minecraft.getInstance();
            BlockEntity machine = source.get();
            if (isStopped() || machine == null || client.level == null || client.player == null
                    || !enabled(client)) {
                return false;
            }
            BlockPos pos = machine.getBlockPos();
            LevelChunk chunk = client.level.getChunkSource().getChunk(
                    pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, false);
            return chunk != null && eligible(client.level, chunk, machine,
                    client.gameRenderer.getMainCamera().getPosition(), family);
        }

        @Override
        public void tick() {
            heartbeat++;
            if (!canPlaySound()) {
                stop();
            }
        }

        @Override
        public long heartbeat() {
            return heartbeat;
        }

        private void retire() {
            stop();
        }
    }
}
