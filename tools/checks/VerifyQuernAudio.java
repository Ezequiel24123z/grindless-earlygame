package io.github.ezequiel24123z.grindless.client.audio;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Exercises the actual ownership policy with a fake engine, not a native/client listening pass. */
public final class VerifyQuernAudio {
    private static final Path SOURCE = Path.of("common/src/main/java/io/github/ezequiel24123z/grindless");
    private static int checks;
    private static int failures;

    public static void main(String[] args) throws IOException, ReflectiveOperationException {
        ownership();
        familyReplacement();
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

    /** Family tokens are immutable source metadata, not another tracker or persisted epoch. */
    private static void familyReplacement() {
        Fixture f = new Fixture();
        FamilySource quern = new FamilySource("QUERN");
        FamilySource mixer = new FamilySource("CLAY_MIXER");
        f.sources.put(11L, quern);
        f.sources.put(12L, mixer);
        f.step();
        FakeVoice old = f.engine.lastAt(11L);
        FakeVoice neighbor = f.engine.lastAt(12L);
        check("one tracker owns both family tokens independently in the same snapshot",
                f.engine.started.size() == 2 && old.source == quern && neighbor.source == mixer);
        f.engine.engineTick();
        f.step();
        old.active = false;
        f.step();
        FamilySource replacement = new FamilySource("CLAY_MIXER");
        f.sources.put(11L, replacement);
        f.step();
        check("a new family/source cannot inherit the retired source's recovery backoff",
                old.stopped && f.engine.started.size() == 3
                        && f.engine.lastAt(11L).source == replacement && f.engine.maxSamePosition == 1);
        check("replacing one family leaves its other-family neighbor's ownership intact",
                !neighbor.stopped && f.engine.lastAt(12L) == neighbor);

        f = new Fixture();
        f.engine.accept = false;
        f.sources.put(3L, quern);
        f.step();
        for (int tick = 0; tick < QuernLoopTracker.ADMISSION_TICKS; tick++) {
            f.step();
        }
        old = f.engine.last();
        f.engine.accept = true;
        f.sources.put(3L, mixer);
        f.step();
        check("family replacement releases cancelled-source suppression without reload or an eligibility gap",
                old.stopped && f.engine.rejections == 1 && f.engine.started.size() == 2
                        && f.engine.last().source == mixer && f.engine.maxSamePosition == 1);
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
        check("the shared policy retains the original range, admission grace and recovery bound",
                QuernLoopTracker.RANGE == 16 && QuernLoopTracker.ADMISSION_TICKS == 5
                        && QuernLoopTracker.RETRY_TICKS == 40);
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
        String audio = sourceCode("client/audio/T0MachineAudio.java").replaceAll("\\s+", " ");
        String client = sourceCode("client/GrindlessClient.java");
        String effects = sourceCode("machine/MachineEffects.java");
        String tank = sourceCode("fluid/BasicTankBlock.java");
        String tankEntity = sourceCode("fluid/BasicTankBlockEntity.java");
        String fluids = sourceCode("fluid/FluidLogic.java").replaceAll("\\s+", " ");
        int dispatchStart = effects.indexOf("switch (geometry) {");
        int dispatchEnd = effects.indexOf("if (status == MachineStatus.OUT_OF_BAND)", dispatchStart);
        String dispatch = dispatchStart >= 0 && dispatchEnd > dispatchStart
                ? effects.substring(dispatchStart, dispatchEnd) : "";
        check("only client initialization binds the manager", client.contains("T0MachineAudio.register();"));
        check("the renamed adapter replaces rather than duplicates the old client binding",
                Files.notExists(SOURCE.resolve("client/audio/QuernAudio.java"))
                        && !client.contains("QuernAudio")
                        && Pattern.compile("T0MachineAudio\\.register\\(\\)").matcher(client).results().count() == 1);
        check("discovery visits only already-loaded chunk block entities",
                audio.contains("getChunk(x, z, ChunkStatus.FULL, false)")
                        && audio.contains("chunk.getBlockEntities().values()")
                        && !audio.contains("getAllInChunk") && !audio.contains("betweenClosed"));
        check("discovery uses detached numeric position identities",
                audio.contains("eligible.put(machine.getBlockPos().asLong(), machine)"));
        check("both families share one tracker and one bounded discovery scan",
                Pattern.compile("new QuernLoopTracker<>").matcher(audio).results().count() == 1
                        && Pattern.compile("chunk\\.getBlockEntities\\(\\)\\.values\\(\\)")
                        .matcher(audio).results().count() == 1
                        && audio.contains("QuernLoopTracker.minChunk(listener.x)")
                        && audio.contains("QuernLoopTracker.maxChunk(listener.x)")
                        && audio.contains("QuernLoopTracker.minChunk(listener.z)")
                        && audio.contains("QuernLoopTracker.maxChunk(listener.z)"));
        check("native eligibility checks the current block, source identity and running status",
                audio.contains("chunk.getBlockEntities().get(pos) != machine")
                        && audio.contains("machine.isRemoved()") && audio.contains("machine.getLevel() != level")
                        && audio.contains("block.kind() == machine.kind()")
                        && audio.contains("familyFor(block.kind()) == family")
                        && audio.contains("state.getValue(ProcessMachineBlock.STATUS) == MachineStatus.RUNNING"));
        check("only Quern and Clay Mixer select a working family; every other process kind is excluded",
                audio.contains("private static Family familyFor(ProcessMachineKind kind) { return switch (kind) { "
                        + "case QUERN -> Family.GRINDING; case CLAY_MIXER -> Family.WET_WORK; default -> null; }; }"));
        check("the final source kind and captured selected family remain authoritative at native admission",
                sourceCode("machine/ProcessMachineBlockEntity.java").contains("private final ProcessMachineKind kind;")
                        && audio.contains("family == null || familyFor(machine.kind()) != family")
                        && audio.contains("new MachineSound(position, source, familyFor(source.kind()))")
                        && audio.contains("private final Family family;") && audio.contains("this.family = family;")
                        && audio.contains("client.gameRenderer.getMainCamera().getPosition(), family)"));
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
        check("the native voice uses the selected event and positional BLOCKS loop",
                audio.contains("super(family.event.get(), SoundSource.BLOCKS,")
                        && audio.contains("looping = true;") && audio.contains("delay = 0;")
                        && audio.contains("relative = false;") && audio.contains("SoundInstance.Attenuation.LINEAR")
                        && audio.contains("volume = family.volume;") && audio.contains("pitch = 1.0F;")
                        && audio.contains("GRINDING(ModSounds.T0_GRINDING, 1.0F)"));
        check("Mixer uses only the wet-work bed at the initial technical volume, without changing Quern",
                audio.contains("private enum Family { GRINDING(ModSounds.T0_GRINDING, 1.0F), "
                        + "WET_WORK(ModSounds.T0_WET_WORK, 0.35F);"));
        check("retirement marks a late-admitted voice stopped before asking the sound manager",
                audio.indexOf("sound.retire();") < audio.indexOf("getSoundManager().stop(sound)")
                        && audio.contains("if (isStopped() || machine == null"));
        check("only Quern opts out of the old shared mill hum",
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
        check("only Mixer opts out of the washer hum, not Settler or Washer",
                effects.contains("case MIXER -> washer(status, level, pos, random, false);")
                        && effects.contains("washer(status, level, pos, random, true);")
                        && effects.contains("case SETTLER -> washer(status, level, pos, random);")
                        && effects.contains("case WASHER -> washer(status, level, pos, random);")
                        && Pattern.compile("washer\\(status, level, pos, random, false\\)")
                        .matcher(effects).results().count() == 1);
        String washer = effects.substring(effects.indexOf("private static void washer("),
                effects.indexOf("private static void cell(")).replaceAll("\\s+", " ");
        String washerFaults = washer.substring(washer.indexOf("case BLOCKED"));
        check("Mixer retains washer running/blocked splashes, starved smoke and ordinary-fault silence",
                washer.contains("if (hum) { sound(level, pos, ModSounds.MACHINE_HUM.get(), random, 0.04F, 0.15F, 1.2F); }")
                        && washer.contains("ParticleTypes.SPLASH, x, pos.getY() + 0.85")
                        && washerFaults.contains("ParticleTypes.SPLASH, x, pos.getY() + 1.0")
                        && washerFaults.contains("ParticleTypes.SMOKE, x, pos.getY() + 0.9")
                        && !washerFaults.contains("sound("));
        check("a stationary full Basic Tank reports stored fluid, not operation",
                fluids.contains("public static MachineStatus tankStatus(boolean holding) { "
                        + "return holding ? MachineStatus.RUNNING : MachineStatus.IDLE; }")
                        && tankEntity.contains("FluidLogic.tankStatus(!tank.buffer.isEmpty())")
                        && tank.contains("MachineEffects.animate(BlockCatalogue.Geometry.TANK, 1,"));
        check("passive TANK has one independent empty effects arm, not generator feedback",
                Pattern.compile("\\bTANK\\b").matcher(dispatch).results().count() == 1
                        && Pattern.compile("(?m)^\\s*case\\s+TANK\\s*->\\s*\\{\\s*}\\s*$")
                        .matcher(dispatch).find());
        check("active combustion and industrial families retain generator feedback",
                List.of("GENERATOR", "KILN", "BOILER", "FIREBOX", "INDUSTRIAL").stream()
                        .allMatch(geometry -> dispatch.contains("case " + geometry
                                + " -> generator(status, level, pos, random);")));
        check("dynamo feedback and pylon warnings keep their own routes",
                dispatch.contains("case DYNAMO -> dynamo(status, front, level, pos, random);")
                        && dispatch.contains("case PYLON -> pylon(tier, status, level, pos, random);")
                        && effects.contains("ModSounds.RELAY_CLICK.get()")
                        && effects.contains("ModSounds.BROWNOUT_ALARM.get()"));
        check("IDLE still returns before any geometry feedback",
                Pattern.compile("if \\(status == MachineStatus.IDLE\\) \\{\\s*return;\\s*}"
                        + "\\s*switch \\(geometry\\)").matcher(effects).find());
        check("the common audio helper has no loader-specific imports",
                !audio.contains("net.minecraftforge") && !audio.contains("net.fabricmc"));
    }

    private static String sourceCode(String path) throws IOException {
        return Files.readString(SOURCE.resolve(path)).replaceAll(
                "(?s)/\\*.*?\\*/|//[^\\r\\n]*|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'", " ");
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

    private record FamilySource(String kind) {
    }

    private static final class FakeVoice implements QuernLoopTracker.Voice {
        private final long position;
        private final Object source;
        private long heartbeat;
        private boolean active;
        private boolean stopped;

        private FakeVoice(long position, Object source, boolean active) {
            this.position = position;
            this.source = source;
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
            FakeVoice voice = new FakeVoice(position, source, accept && !replace);
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

        private FakeVoice lastAt(long position) {
            return started.stream().filter(voice -> voice.position == position)
                    .reduce((first, second) -> second).orElseThrow();
        }

        private void engineTick() {
            started.stream().filter(v -> v.active && !v.stopped).forEach(v -> v.heartbeat++);
        }
    }
}
