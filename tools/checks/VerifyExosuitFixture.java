package io.github.ezequiel24123z.grindless.testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Source contracts only; actual chunk loading and scheduled entity visibility require Minecraft. */
public final class VerifyExosuitFixture {

    private static final Path SMOKE = Path.of("tools", "smoke");
    private static final Path PACK = SMOKE.resolve("first-exosuit-pack");
    private static final String TAG = "grindless_exosuit_smoke";
    private static final List<String> ITEMS = List.of(
            "flux_exosuit_helmet", "flux_exosuit_chestplate", "network_tap", "exoskeleton_legs");
    private static final List<String> PARTS = List.of("helmet", "chest", "tap", "legs");
    private static final List<String> LABELS = List.of("HELMET", "CHEST", "TAP", "LEGS");
    private static final List<String> MARKERS = List.of(
            "EXO-HELMET-OK", "EXO-CHEST-OK", "TAP-OK", "LEGS-MOD-OK");
    private static int failures;

    public static void main(String[] args) throws IOException {
        Path functions = PACK.resolve("data").resolve("grindless_exosuit").resolve("functions");
        List<String> begin = commands(functions.resolve("begin.mcfunction"));
        List<String> spawn = commands(functions.resolve("spawn.mcfunction"));
        List<String> poll = commands(functions.resolve("poll.mcfunction"));
        List<String> passed = commands(functions.resolve("passed.mcfunction"));
        List<String> failed = commands(functions.resolve("failed.mcfunction"));
        List<String> cleanup = commands(functions.resolve("cleanup.mcfunction"));
        List<String> all = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(PACK)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".mcfunction")).toList()) {
                all.addAll(commands(path));
            }
        }

        check("the console only starts the overworld datapack, with no duplicate summons or kills",
                commands(SMOKE.resolve("first-exosuit.commands")).equals(List.of(
                        "execute in minecraft:overworld run function grindless_exosuit:begin")));
        check("the four original expectations are unchanged",
                commands(SMOKE.resolve("first-exosuit.expect")).equals(MARKERS));
        check("the existing runner selects a 1.20.1 datapack with no timeout override",
                Files.readString(PACK.resolve("pack.mcmeta")).contains("\"pack_format\": 15")
                        && !Files.exists(SMOKE.resolve("first-exosuit.timeout")));
        check("begin resets the bounded state and requests the target chunk",
                begin.containsAll(List.of("scoreboard players set phase exosuit_check 0",
                        "scoreboard players set tries exosuit_check 0", "forceload add 0 0",
                        "schedule function grindless_exosuit:poll 1t replace")));
        check("the one spawn entry requires phase zero and an actually loaded chunk",
                all.stream().filter(line -> line.endsWith("run function grindless_exosuit:spawn")).toList()
                        .equals(List.of("execute if score phase exosuit_check matches 0 if loaded 8 100 8"
                                + " run function grindless_exosuit:spawn")));
        check("spawn leaves phase zero before attempting any item",
                spawn.get(0).equals("scoreboard players set phase exosuit_check 1"));
        check("only the one-shot spawn function summons exactly four items",
                all.stream().filter(line -> line.contains("summon minecraft:item ")).count() == 4
                        && spawn.stream().filter(line -> line.contains("summon minecraft:item ")).count() == 4);
        for (int i = 0; i < ITEMS.size(); i++) {
            String item = ITEMS.get(i);
            String score = PARTS.get(i) + "_spawn";
            List<String> summons = spawn.stream().filter(line -> line.contains(
                    "Item:{id:\"grindless:" + item + "\",Count:1b}")).toList();
            check(item + " is summoned once with test ownership and incidental hazards isolated",
                    summons.size() == 1 && summons.get(0).contains("run summon minecraft:item 8 100 8")
                            && summons.get(0).contains("Tags:[\"" + TAG + "\"]")
                            && summons.get(0).contains("NoGravity:1b,Invulnerable:1b,PickupDelay:32767s")
                            && summons.get(0).contains("Motion:[0.0d,0.0d,0.0d]"));
            check(item + " distinguishes unattempted summons from the actual command success",
                    begin.contains("scoreboard players set " + score + " exosuit_check -1")
                            && summons.size() == 1 && summons.get(0).startsWith(
                                    "execute store success score " + score + " exosuit_check run summon ")
                            && all.stream().filter(line -> line.startsWith("scoreboard players set " + score + " "))
                                    .toList().equals(List.of("scoreboard players set " + score + " exosuit_check -1"))
                            && all.stream().filter(line -> line.startsWith("execute store success score " + score + " "))
                                    .toList().equals(summons));
        }
        String observed = "execute if score phase exosuit_check matches 1 if loaded 8 100 8";
        for (String item : ITEMS) {
            observed += " if entity " + entity(item);
        }
        observed += " run scoreboard players set observed exosuit_check 1";
        check("each poll resets observation, then requires all four actual tagged IDs together",
                poll.indexOf("scoreboard players set observed exosuit_check 0") >= 0
                        && poll.indexOf(observed) > poll.indexOf("scoreboard players set observed exosuit_check 0"));
        String coherentObservation = observed;
        check("no other function can latch a successful observation",
                all.stream().filter(line -> line.endsWith("run scoreboard players set observed exosuit_check 1"))
                        .toList().equals(List.of(coherentObservation)));
        check("only that coherent observation can reach the success function",
                all.stream().filter(line -> line.endsWith("function grindless_exosuit:passed")).toList()
                        .equals(List.of("execute if score observed exosuit_check matches 1"
                                + " run function grindless_exosuit:passed")));
        check("every poll consumes the shared chunk/entity counter and stops by attempt 160",
                poll.get(0).equals("scoreboard players add tries exosuit_check 1")
                        && poll.contains("execute if score phase exosuit_check matches 0..1"
                                + " if score tries exosuit_check matches ..159"
                                + " run schedule function grindless_exosuit:poll 5t replace")
                        && poll.contains("execute if score phase exosuit_check matches 0..1"
                                + " if score tries exosuit_check matches 160.."
                                + " run function grindless_exosuit:failed"));
        check("only begin resets the counter and only poll advances it",
                all.stream().filter(line -> line.startsWith("scoreboard players set tries ")).toList()
                        .equals(List.of("scoreboard players set tries exosuit_check 0"))
                        && all.stream().filter(line -> line.startsWith("scoreboard players add tries ")).toList()
                                .equals(List.of("scoreboard players add tries exosuit_check 1")));
        check("scheduling only polls, never reruns setup or summons",
                all.stream().filter(line -> line.contains("schedule function ")).sorted().toList()
                        .equals(List.of("execute if score phase exosuit_check matches 0..1"
                                        + " if score tries exosuit_check matches ..159"
                                        + " run schedule function grindless_exosuit:poll 5t replace",
                                "schedule function grindless_exosuit:poll 1t replace")));
        List<String> success = new ArrayList<>(List.of("scoreboard players set phase exosuit_check 2"));
        MARKERS.forEach(marker -> success.add("say " + marker));
        success.add("function grindless_exosuit:cleanup");
        check("success terminates polling, emits all four markers and only then cleans up", passed.equals(success));
        for (String marker : MARKERS) {
            check(marker + " has exactly one emitter and none on failure",
                    all.stream().filter(line -> line.contains(marker)).toList().equals(List.of("say " + marker))
                            && failed.stream().noneMatch(line -> line.contains(marker)));
        }
        check("cleanup cannot kill unrelated item entities",
                cleanup.equals(List.of("kill @e[type=minecraft:item,tag=" + TAG + "]", "forceload remove 0 0"))
                        && all.stream().filter(line -> line.startsWith("kill ")).toList().equals(
                                List.of("kill @e[type=minecraft:item,tag=" + TAG + "]")));
        List<String> diagnostics = new ArrayList<>(List.of(
                "say EXOSUIT-FAIL-CHUNK-OR-ENTITY-TIMEOUT",
                "say EXOSUIT-DIAG-SOURCE-first-exosuit-pack-DIM-minecraft:overworld-CHUNK-0,0-POS-8,100,8",
                "say EXOSUIT-DIAG-BUDGET-160-POLLS-5t-INTERVAL-RUNNER-DEFAULT-120s",
                "execute if score phase exosuit_check matches 0 run say EXOSUIT-DIAG-PHASE-CHUNK-WAIT",
                "execute if score phase exosuit_check matches 1 run say EXOSUIT-DIAG-PHASE-ENTITY-WAIT",
                "execute unless score phase exosuit_check matches 0..1 run say EXOSUIT-DIAG-PHASE-UNEXPECTED",
                "execute if score tries exosuit_check matches 160 run say EXOSUIT-DIAG-COUNTER-160",
                "execute unless score tries exosuit_check matches 160 run say EXOSUIT-DIAG-COUNTER-NOT-160",
                "execute if loaded 8 100 8 run say EXOSUIT-DIAG-CHUNK-LOADED",
                "execute unless loaded 8 100 8 run say EXOSUIT-DIAG-CHUNK-NOT-LOADED",
                "execute store success score chunk_requested exosuit_check run forceload query 0 0",
                "execute if score chunk_requested exosuit_check matches 1 run say EXOSUIT-DIAG-CHUNK-REQUESTED",
                "execute unless score chunk_requested exosuit_check matches 1 run say EXOSUIT-DIAG-CHUNK-NOT-REQUESTED"));
        for (int i = 0; i < ITEMS.size(); i++) {
            String score = PARTS.get(i) + "_spawn";
            String label = LABELS.get(i);
            diagnostics.add("execute if score " + score + " exosuit_check matches -1"
                    + " run say EXOSUIT-DIAG-SUMMON-" + label + "-NOT-ATTEMPTED");
            diagnostics.add("execute if score " + score + " exosuit_check matches 0"
                    + " run say EXOSUIT-DIAG-SUMMON-" + label + "-REJECTED");
            diagnostics.add("execute if score " + score + " exosuit_check matches 1"
                    + " run say EXOSUIT-DIAG-SUMMON-" + label + "-ACCEPTED");
            diagnostics.add("execute unless score " + score + " exosuit_check matches -1..1"
                    + " run say EXOSUIT-DIAG-SUMMON-" + label + "-UNKNOWN");
            diagnostics.add("execute if entity " + entity(ITEMS.get(i))
                    + " run say EXOSUIT-DIAG-ENTITY-" + label + "-OBSERVED");
            diagnostics.add("execute unless entity " + entity(ITEMS.get(i))
                    + " run say EXOSUIT-DIAG-ENTITY-" + label + "-NOT-OBSERVED");
        }
        diagnostics.add("scoreboard players set phase exosuit_check -1");
        diagnostics.add("function grindless_exosuit:cleanup");
        check("failure requires real say emitters bound to scores/chunk/IDs before cleanup, not silent data feedback",
                failed.equals(diagnostics));
        System.out.println(failures == 0
                ? "ALL EXOSUIT FIXTURE SOURCE CHECKS PASSED (NOT A MINECRAFT SCHEDULING PROOF)"
                : failures + " EXOSUIT FIXTURE SOURCE CHECK(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static String entity(String item) {
        return "@e[type=minecraft:item,tag=" + TAG + ",nbt={Item:{id:\"grindless:" + item + "\",Count:1b}}]";
    }

    private static List<String> commands(Path path) throws IOException {
        return Files.readAllLines(path).stream().map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#")).toList();
    }

    private static void check(String message, boolean condition) {
        if (!condition) {
            failures++;
        }
        System.out.println((condition ? "  ok   " : "  FAIL ") + message);
    }
}
