package io.github.ezequiel24123z.grindless.client.audio;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Exercises the actual ownership policy with a fake engine, not a native/client listening pass. */
public final class VerifyQuernAudio {
    private static final Path SOURCE = Path.of("common/src/main/java/io/github/ezequiel24123z/grindless");
    private static int checks;
    private static int failures;

    public static void main(String[] args) throws IOException, ReflectiveOperationException {
        ownership();
        recovery();
        admission();
        pauseAndVolume();
        scopeChanges();
        distances();
        nativeBindings();
        System.out.println(checks + " QUERN AUDIO CHECKS, " + failures + " FAILED");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void ownership() {
        Fixture f = new Fixture();
        f.step();
        check("no eligible Quern means no playback", f.engine.started.isEmpty());
        Object first = new String("quern");
        f.sources.put(11L, first);
        f.step();
        FakeVoice initial = f.engine.last();
        check("a running Quern starts one voice immediately", f.engine.started.size() == 1);
        f.step();
        check("awaiting the first native tick never duplicates a voice", f.engine.started.size() == 1);
        for (int tick = 0; tick < 200; tick++) {
            f.engine.engineTick();
            f.step();
        }
        check("native ticks retain exactly one owned voice", f.engine.started.size() == 1 && !initial.stopped);
        f.sources.put(11L, new String("quern"));
        f.step();
        check("equal-but-distinct replacement sources retire the previous identity",
                initial.stopped && f.engine.started.size() == 2);
        check("replacement stops before a new same-position voice starts", f.engine.maxSamePosition == 1);
        FakeVoice replacement = f.engine.last();
        f.sources.clear();
        f.step();
        check("a no-longer-running/removed/unloaded source stops and removes ownership", replacement.stopped);
        int stopped = f.engine.stops;
        f.step();
        check("removal is idempotent", f.engine.stops == stopped);
        f.sources.put(11L, first);
        f.step();
        check("the same position can restart after eligibility returns", f.engine.started.size() == 3);
        for (long key = 12; key < 19; key++) {
            f.sources.put(key, new Object());
        }
        f.engine.engineTick();
        f.step();
        check("eight eligible positions own eight independent voices",
                f.engine.started.stream().filter(v -> !v.stopped).count() == 8);
        f.loops.clear();
        check("clearing stops every owned voice", f.engine.started.stream().allMatch(v -> v.stopped));
        stopped = f.engine.stops;
        f.loops.clear();
        check("repeated cleanup does not stop unrelated or already-released voices", f.engine.stops == stopped);
    }

    private static void recovery() {
        Fixture f = Fixture.running();
        FakeVoice old = f.engine.last();
        old.active = false;
        f.step();
        check("stopAll loss retires an admitted instance even without its stopped flag", old.stopped);
        for (int tick = 1; tick < QuernLoopTracker.RETRY_TICKS; tick++) {
            f.step();
        }
        check("an admitted loss cannot restart before the exact recovery bound", f.engine.started.size() == 1);
        f.step();
        check("stopAll recovery creates a fresh instance at the bound", f.engine.started.size() == 2);
        check("late playback of the old instance is disabled before replacement", old.stopped
                && f.engine.maxSamePosition == 1);
        f.engine.engineTick();
        f.step();
        for (int tick = 0; tick < QuernLoopTracker.ADMISSION_TICKS; tick++) {
            f.step();
        }
        check("a stale ticking lease is retired even when engine isActive stays true", f.engine.last().stopped);

        f = Fixture.running();
        old = f.engine.last();
        f.loops.clear();
        f.step();
        check("resource reload cleanup stops before creating the new eligible voice",
                old.stopped && f.engine.started.size() == 2 && f.engine.maxSamePosition == 1);
        f = Fixture.running();
        old = f.engine.last();
        old.stopped = true;
        f.step();
        check("a natively stopped voice is not retained", f.engine.stops == 1);
        f.sources.clear();
        f.step();
        for (int tick = 0; tick <= QuernLoopTracker.RETRY_TICKS; tick++) {
            f.step();
        }
        check("an unloaded source cannot restart from a pending recovery", f.engine.started.size() == 1);

        f = Fixture.running();
        f.engine.last().active = false;
        f.step();
        f.paused = true;
        for (int tick = 0; tick < 100; tick++) {
            f.step();
        }
        f.paused = false;
        for (int tick = 1; tick < QuernLoopTracker.RETRY_TICKS; tick++) {
            f.step();
        }
        check("pause does not consume the admitted-loss backoff", f.engine.started.size() == 1);
        f.step();
        check("recovery resumes after the remaining unpaused backoff", f.engine.started.size() == 2);
    }

    private static void admission() {
        Fixture f = new Fixture();
        f.engine.accept = false;
        f.sources.put(3L, new Object());
        f.step();
        for (int tick = 1; tick < QuernLoopTracker.ADMISSION_TICKS; tick++) {
            f.step();
        }
        check("cancelled admission receives the full grace interval", !f.engine.last().stopped);
        f.step();
        check("an unadmitted instance is explicitly stopped before release",
                f.engine.last().stopped && f.engine.rejections == 1);
        for (int tick = 0; tick < 500; tick++) {
            f.step();
        }
        check("cancellation or replacement cannot cause repeated unknown voices",
                f.engine.started.size() == 1 && f.engine.rejections == 1);
        f.sources.clear();
        f.step();
        f.engine.accept = true;
        f.sources.put(3L, new Object());
        f.step();
        check("a new running episode releases the cancellation latch", f.engine.started.size() == 2);
        f = new Fixture();
        f.sources.put(3L, new Object());
        f.step();
        for (int tick = 1; tick < QuernLoopTracker.ADMISSION_TICKS; tick++) {
            f.step();
        }
        f.engine.engineTick();
        f.step();
        check("admission on the final allowed tick is retained",
                !f.engine.last().stopped && f.engine.rejections == 0);

        f = new Fixture();
        f.engine.replace = true;
        f.sources.put(3L, new Object());
        f.step();
        for (int tick = 0; tick < 500; tick++) {
            f.step();
        }
        check("a substituted engine instance cannot trigger duplicate replacement requests",
                f.engine.substitutions == 1 && f.engine.started.size() == 1 && f.engine.last().stopped);
        f.loops.clear();
        f.engine.replace = false;
        f.step();
        check("resource reload releases a suppressed admission without changing the running source",
                f.engine.started.size() == 2);
    }

    private static void pauseAndVolume() {
        Fixture f = new Fixture();
        f.sources.put(4L, new Object());
        f.paused = true;
        for (int tick = 0; tick < 100; tick++) {
            f.step();
        }
        check("a paused client does not start playback", f.engine.started.isEmpty());
        f.paused = false;
        f.step();
        f.paused = true;
        for (int tick = 0; tick < 100; tick++) {
            f.step();
        }
        check("pause cannot consume admission grace or generate retries",
                f.engine.started.size() == 1 && !f.engine.last().stopped && f.engine.rejections == 0);
        f.paused = false;
        f.engine.engineTick();
        f.step();
        f.paused = true;
        for (int tick = 0; tick < 100; tick++) {
            f.step();
        }
        check("native pause retains an admitted voice without heartbeat expiry", !f.engine.last().stopped);
        f.paused = false;
        f.engine.engineTick();
        f.step();
        check("resume reuses the paused voice", f.engine.started.size() == 1);
        f.enabled = false;
        f.step();
        check("MASTER/BLOCKS disablement retires existing playback", f.engine.last().stopped);
        for (int tick = 0; tick < 100; tick++) {
            f.step();
        }
        check("disabled audio neither attempts admission nor consumes its grace",
                f.engine.started.size() == 1 && f.engine.rejections == 0);
        f.enabled = true;
        f.step();
        check("restoring volume starts a still-running source without a world/state change",
                f.engine.started.size() == 2);

        f = new Fixture();
        f.sources.put(4L, new Object());
        f.enabled = false;
        for (int tick = 0; tick < 100; tick++) {
            f.step();
        }
        f.enabled = true;
        f.step();
        check("a source first discovered while muted starts when unmuted",
                f.engine.started.size() == 1 && f.engine.rejections == 0);
        f.paused = true;
        f.enabled = false;
        f.step();
        check("muting while paused still retires ownership", f.engine.last().stopped);
    }

    private static void scopeChanges() throws ReflectiveOperationException {
        Fixture f = Fixture.running();
        FakeVoice old = f.engine.last();
        f.world = new Object();
        f.step();
        check("world replacement retires old ownership even at the same position/source token",
                old.stopped && f.engine.started.size() == 2);
        old = f.engine.last();
        f.player = new Object();
        f.step();
        check("player replacement also retires old ownership", old.stopped && f.engine.started.size() == 3);
        old = f.engine.last();
        f.paused = true;
        f.world = new Object();
        f.step();
        check("world changes clear ownership even during pause", old.stopped && f.engine.started.size() == 3);
        f.paused = false;
        f.step();
        old = f.engine.last();
        f.player = null;
        f.step();
        check("a missing player disconnects the old voice", old.stopped);
        f.player = new Object();
        f.step();
        old = f.engine.last();
        f.world = null;
        f.step();
        check("a missing world disconnects the old voice", old.stopped);
        check("world identity is weak, not a retained old level",
                QuernLoopTracker.class.getDeclaredField("world").getType() == WeakReference.class);
        check("player identity is weak, not a retained old player",
                QuernLoopTracker.class.getDeclaredField("player").getType() == WeakReference.class);
    }

    private static void distances() {
        check("a centered source is eligible", QuernLoopTracker.inRange(0, 0, 0));
        check("the near side of attenuation is eligible", QuernLoopTracker.inRange(15.999, 0, 0));
        check("the exact inaudible boundary is excluded", !QuernLoopTracker.inRange(16, 0, 0));
        check("distance is three-dimensional", !QuernLoopTracker.inRange(0, -16, 0)
                && !QuernLoopTracker.inRange(12, 0, 12));
        check("nonfinite distance is never eligible", !QuernLoopTracker.inRange(Double.NaN, 0, 0)
                && !QuernLoopTracker.inRange(Double.POSITIVE_INFINITY, 0, 0));
        boolean bounded = true;
        boolean complete = true;
        for (double listener : List.of(-32.5, -16.001, -16.0, -0.5, 0.0, 0.5, 15.999, 16.0, 32.5)) {
            int min = QuernLoopTracker.minChunk(listener);
            int max = QuernLoopTracker.maxChunk(listener);
            bounded &= max - min + 1 <= 3;
            for (int block = -64; block <= 64; block++) {
                if (QuernLoopTracker.inRange(block + 0.5 - listener, 0, 0)) {
                    complete &= (block >> 4) >= min && (block >> 4) <= max;
                }
            }
        }
        check("nearby discovery is bounded to at most nine X/Z chunks", bounded);
        check("chunk bounds cover eligible block centers across negative and positive edges", complete);

        Fixture f = Fixture.running();
        FakeVoice old = f.engine.last();
        f.sources.clear();
        f.step();
        check("leaving the audible snapshot stops ownership", old.stopped);
        f.sources.put(1L, new Object());
        f.step();
        check("returning inside range can start a fresh voice", f.engine.started.size() == 2);
    }

    private static void nativeBindings() throws IOException {
        String audio = Files.readString(SOURCE.resolve("client/audio/QuernAudio.java")).replaceAll("\\s+", " ");
        String client = Files.readString(SOURCE.resolve("client/GrindlessClient.java"));
        String effects = Files.readString(SOURCE.resolve("machine/MachineEffects.java"));
        check("only client initialization binds the manager", client.contains("QuernAudio.register();"));
        check("discovery visits only already-loaded chunk block entities",
                audio.contains("getChunk(x, z, ChunkStatus.FULL, false)")
                        && audio.contains("chunk.getBlockEntities().values()")
                        && !audio.contains("getAllInChunk") && !audio.contains("betweenClosed"));
        check("discovery uses detached numeric position identities",
                audio.contains("eligible.put(machine.getBlockPos().asLong(), machine)"));
        check("native eligibility checks the current block, source identity and running status",
                audio.contains("chunk.getBlockEntities().get(pos) != machine")
                        && audio.contains("machine.isRemoved()") && audio.contains("machine.getLevel() != level")
                        && audio.contains("block.kind() == ProcessMachineKind.QUERN")
                        && audio.contains("state.getValue(ProcessMachineBlock.STATUS) == MachineStatus.RUNNING"));
        check("source lifetime does not retain the block entity's old level",
                audio.contains("WeakReference<ProcessMachineBlockEntity> source"));
        check("native category disablement cannot consume admission grace",
                audio.contains("getSoundSourceVolume(SoundSource.MASTER) > 0.0F")
                        && audio.contains("getSoundSourceVolume(SoundSource.BLOCKS) > 0.0F")
                        && audio.contains("loops.tick(level, client.player, Map.of(), paused, enabled)"));
        check("pause and the reload overlay freeze ownership time",
                audio.contains("client.isPaused() || client.getOverlay() != null"));
        check("disconnect, respawn, shutdown and resource reload retire ownership",
                audio.contains("CLIENT_PLAYER_QUIT.register") && audio.contains("CLIENT_PLAYER_RESPAWN.register")
                        && audio.contains("CLIENT_STOPPING.register") && audio.contains("PackType.CLIENT_RESOURCES")
                        && audio.contains("ResourceManagerReloadListener"));
        check("the native voice uses the exact event and positional BLOCKS loop",
                audio.contains("super(ModSounds.T0_GRINDING.get(), SoundSource.BLOCKS,")
                        && audio.contains("looping = true;") && audio.contains("delay = 0;")
                        && audio.contains("relative = false;") && audio.contains("SoundInstance.Attenuation.LINEAR")
                        && audio.contains("volume = 1.0F;") && audio.contains("pitch = 1.0F;"));
        check("retirement marks a late-admitted voice stopped before asking the sound manager",
                audio.indexOf("sound.retire();") < audio.indexOf("getSoundManager().stop(sound)")
                        && audio.contains("if (isStopped() || machine == null"));
        check("only Quern opts out of the old shared hum",
                effects.contains("case QUERN -> mill(status, level, pos, random, false);")
                        && effects.contains("mill(status, level, pos, random, true);")
                        && effects.contains("case PUMP -> mill(status, level, pos, random);")
                        && effects.contains("case EPUMP -> mill(status, level, pos, random);")
                        && effects.contains("case MILL -> mill(status, level, pos, random);")
                        && effects.contains("case EXTRACTOR -> extractor(status, level, pos, random);"));
        check("Quern keeps shared running particles and blocked smoke",
                effects.contains("ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.7")
                        && effects.contains("ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.9")
                        && effects.contains("if (hum) {"));
        check("the common audio helper has no loader-specific imports",
                !audio.contains("net.minecraftforge") && !audio.contains("net.fabricmc"));
    }

    private static void check(String description, boolean passed) {
        checks++;
        if (!passed) {
            failures++;
            System.err.println("FAIL " + description);
        }
    }

    private static final class Fixture {
        private final FakePlayback engine = new FakePlayback();
        private final QuernLoopTracker<Object, FakeVoice> loops = new QuernLoopTracker<>(engine);
        private final Map<Long, Object> sources = new HashMap<>();
        private Object world = new Object();
        private Object player = new Object();
        private boolean paused;
        private boolean enabled = true;

        private void step() {
            loops.tick(world, player, sources, paused, enabled);
        }

        private static Fixture running() {
            Fixture fixture = new Fixture();
            fixture.sources.put(1L, new Object());
            fixture.step();
            fixture.engine.engineTick();
            fixture.step();
            return fixture;
        }
    }

    private static final class FakeVoice implements QuernLoopTracker.Voice {
        private final long position;
        private long heartbeat;
        private boolean active;
        private boolean stopped;

        private FakeVoice(long position, boolean active) {
            this.position = position;
            this.active = active;
        }

        @Override
        public long heartbeat() {
            return heartbeat;
        }

        @Override
        public boolean isStopped() {
            return stopped;
        }
    }

    private static final class FakePlayback implements QuernLoopTracker.Playback<Object, FakeVoice> {
        private final List<FakeVoice> started = new ArrayList<>();
        private boolean accept = true;
        private boolean replace;
        private int stops;
        private int rejections;
        private int substitutions;
        private long maxSamePosition;

        @Override
        public FakeVoice start(long position, Object source) {
            FakeVoice voice = new FakeVoice(position, accept && !replace);
            if (replace) {
                substitutions++;
            }
            started.add(voice);
            maxSamePosition = Math.max(maxSamePosition, started.stream()
                    .filter(v -> v.position == position && !v.stopped).count());
            return voice;
        }

        @Override
        public void stop(FakeVoice voice) {
            voice.stopped = true;
            voice.active = false;
            stops++;
        }

        @Override
        public boolean isActive(FakeVoice voice) {
            return voice.active;
        }

        @Override
        public void notAdmitted(long position) {
            rejections++;
        }

        private FakeVoice last() {
            return started.get(started.size() - 1);
        }

        private void engineTick() {
            started.stream().filter(v -> v.active && !v.stopped).forEach(v -> v.heartbeat++);
        }
    }
}
