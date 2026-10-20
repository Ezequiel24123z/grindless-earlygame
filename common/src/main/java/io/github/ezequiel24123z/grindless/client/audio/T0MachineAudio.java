package io.github.ezequiel24123z.grindless.client.audio;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import io.github.ezequiel24123z.grindless.Grindless;
import io.github.ezequiel24123z.grindless.machine.MachineStatus;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlock;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineBlockEntity;
import io.github.ezequiel24123z.grindless.machine.ProcessMachineKind;
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
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/** One native positional working loop per eligible Quern or Clay Mixer, owned by the current client. */
public final class T0MachineAudio {

    private final QuernLoopTracker<ProcessMachineBlockEntity, MachineSound> loops =
            new QuernLoopTracker<>(new QuernLoopTracker.Playback<>() {
                @Override
                public MachineSound start(long position, ProcessMachineBlockEntity source) {
                    MachineSound sound = new MachineSound(position, source, familyFor(source.kind()));
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
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> audio.loops.clear());
        ClientPlayerEvent.CLIENT_PLAYER_RESPAWN.register((oldPlayer, newPlayer) -> audio.loops.clear());
        ClientLifecycleEvent.CLIENT_STOPPING.register(client -> audio.loops.clear());
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES,
                (ResourceManagerReloadListener) resources -> audio.loops.clear(),
                Grindless.id("t0_machine_audio"));
    }

    private void tick(Minecraft client) {
        ClientLevel level = client.level;
        boolean paused = client.isPaused() || client.getOverlay() != null;
        boolean enabled = enabled(client);
        if (level == null || client.player == null || !enabled || paused) {
            loops.tick(level, client.player, Map.of(), paused, enabled);
            return;
        }
        Vec3 listener = client.gameRenderer.getMainCamera().getPosition();
        Map<Long, ProcessMachineBlockEntity> eligible = new HashMap<>();
        for (int x = QuernLoopTracker.minChunk(listener.x); x <= QuernLoopTracker.maxChunk(listener.x); x++) {
            for (int z = QuernLoopTracker.minChunk(listener.z); z <= QuernLoopTracker.maxChunk(listener.z); z++) {
                LevelChunk chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                for (var blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof ProcessMachineBlockEntity machine
                            && eligible(level, chunk, machine, listener, familyFor(machine.kind()))) {
                        eligible.put(machine.getBlockPos().asLong(), machine);
                    }
                }
            }
        }
        loops.tick(level, client.player, eligible, false, true);
    }

    private static boolean enabled(Minecraft client) {
        return client.options.getSoundSourceVolume(SoundSource.MASTER) > 0.0F
                && client.options.getSoundSourceVolume(SoundSource.BLOCKS) > 0.0F;
    }

    private static boolean eligible(ClientLevel level, LevelChunk chunk,
                                    ProcessMachineBlockEntity machine, Vec3 listener, Family family) {
        BlockPos pos = machine.getBlockPos();
        if (family == null || familyFor(machine.kind()) != family
                || machine.isRemoved() || machine.getLevel() != level
                || chunk.getBlockEntities().get(pos) != machine
                || !QuernLoopTracker.inRange(pos.getX() + 0.5 - listener.x,
                        pos.getY() + 0.5 - listener.y, pos.getZ() + 0.5 - listener.z)) {
            return false;
        }
        var state = chunk.getBlockState(pos);
        return state.getBlock() instanceof ProcessMachineBlock block && block.kind() == machine.kind()
                && familyFor(block.kind()) == family
                && state.getValue(ProcessMachineBlock.STATUS) == MachineStatus.RUNNING;
    }

    private static Family familyFor(ProcessMachineKind kind) {
        return switch (kind) {
            case QUERN -> Family.GRINDING;
            case CLAY_MIXER -> Family.WET_WORK;
            default -> null;
        };
    }

    private enum Family {
        GRINDING(ModSounds.T0_GRINDING, 1.0F),
        WET_WORK(ModSounds.T0_WET_WORK, 0.35F);

        private final RegistrySupplier<SoundEvent> event;
        private final float volume;

        Family(RegistrySupplier<SoundEvent> event, float volume) {
            this.event = event;
            this.volume = volume;
        }
    }

    private static final class MachineSound extends AbstractTickableSoundInstance implements QuernLoopTracker.Voice {
        private final WeakReference<ProcessMachineBlockEntity> source;
        private final Family family;
        private long heartbeat;

        private MachineSound(long position, ProcessMachineBlockEntity source, Family family) {
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
            ProcessMachineBlockEntity machine = source.get();
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
