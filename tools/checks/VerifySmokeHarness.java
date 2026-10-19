package io.github.ezequiel24123z.grindless.testing;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/** Exercises real smoke dispatch, selection and assertion budgets with isolated fake boots, not Minecraft. */
public final class VerifySmokeHarness {

    public static void main(String[] args) throws Exception {
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        if (args.length != 0 && (args.length != 2 || !args[0].equals("--bash"))) {
            throw new IllegalArgumentException("usage: VerifySmokeHarness [--bash executable]");
        }
        boolean bash = !windows || args.length == 2;
        String executable = args.length == 2 ? args[1] : bash ? "bash" : "powershell";
        Files.createDirectories(Path.of("build"));
        Path fixture = Files.createTempDirectory(Path.of("build").toAbsolutePath(), "smoke runner ");
        boolean passed;
        try {
            passed = exercise(fixture, executable, bash);
        } finally {
            try (Stream<Path> paths = Files.walk(fixture)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        System.out.println(passed ? "ALL SMOKE HARNESS CHECKS PASSED" : "SMOKE HARNESS CHECKS FAILED");
        System.exit(passed ? 0 : 1);
    }

    private static boolean exercise(Path root, String executable, boolean bash) throws Exception {
        Path tools = Files.createDirectories(root.resolve("tools"));
        Path scenarios = Files.createDirectories(tools.resolve("smoke"));
        Files.writeString(scenarios.resolve("scenarios.txt"), "alpha first\nbeta failing\ngamma last\n");
        for (String name : List.of("alpha", "beta", "gamma")) {
            Files.writeString(scenarios.resolve(name + ".commands"), "say " + name + "\n");
            Files.writeString(scenarios.resolve(name + ".expect"), name + "\n");
        }
        String suffix = bash ? ".sh" : ".ps1";
        Path runner = tools.resolve("run-smokes" + suffix);
        Files.writeString(runner, Files.readString(Path.of("tools", "run-smokes" + suffix)).replace("\r\n", "\n"));
        Path boot = tools.resolve("smoke-boot" + suffix);
        Files.writeString(boot, bash ? """
                #!/usr/bin/env sh
                name="$(basename "$SMOKE_COMMANDS" .commands)"
                printf '%s\\n' "$name" >> "$1/calls.txt"
                printf '%s|%s|%s|%s\\n' "$name" "${SMOKE_EXPECT_TIMEOUT-<unset>}" \
                  "${SMOKE_EXPECT_TIMEOUT:-120}" "${SMOKE_TIMEOUT-<unset>}" >> "$1/timeouts.txt"
                [ "$name" != "beta" ]
                """ : """
                param([string]$Root)
                $name = [System.IO.Path]::GetFileNameWithoutExtension($env:SMOKE_COMMANDS)
                Add-Content -LiteralPath (Join-Path $Root 'calls.txt') -Value $name -Encoding ASCII
                $raw = if ($null -eq $env:SMOKE_EXPECT_TIMEOUT) { '<unset>' } else { $env:SMOKE_EXPECT_TIMEOUT }
                $effective = if ($env:SMOKE_EXPECT_TIMEOUT) { $env:SMOKE_EXPECT_TIMEOUT } else { '120' }
                Add-Content -LiteralPath (Join-Path $Root 'timeouts.txt') -Encoding ASCII \
                    -Value "$name|$raw|$effective|$($env:SMOKE_TIMEOUT)"
                if ($name -eq 'beta') { exit 1 }
                exit 0
                """);
        if (bash && !boot.toFile().setExecutable(true)) {
            throw new IOException("Could not make the fixture boot executable");
        }
        List<String> command = new ArrayList<>(List.of(executable));
        if (!bash) {
            command.addAll(List.of("-NoProfile", "-ExecutionPolicy", "Bypass", "-File"));
        }
        command.add(runner.toString());
        if (!bash) {
            command.add("-Root");
        }
        command.add(root.toString());
        Path output = root.resolve("runner.log");
        int code = run(command, output);
        List<String> calls = readLines(root.resolve("calls.txt"));
        boolean dispatched = calls.equals(List.of("alpha", "beta", "gamma"));
        boolean failed = code != 0;
        Path reportPath = root.resolve("forge").resolve("run").resolve("smoke-report.html");
        String report = Files.exists(reportPath) ? Files.readString(reportPath) : "";
        boolean reported = report.contains("1 failed") && report.contains("<b>2</b> passed");
        boolean passed = check(dispatched, "actual scenario dispatch: " + calls, output);
        passed &= check(failed, "a failed scenario fails the runner", output);
        passed &= check(reported, "report preserves two passes and one failure", output);
        passed &= check(readLines(root.resolve("timeouts.txt")).equals(List.of(
                budget("alpha", "<unset>"), budget("beta", "<unset>"), budget("gamma", "<unset>"))),
                "absent sidecars preserve the boot's 120-second default and startup budget", output);

        clearBoots(root);
        List<String> selected = new ArrayList<>(command);
        selected.addAll(List.of(bash ? "--only" : "-Only", "alpha,gamma"));
        int selectedCode = run(selected, root.resolve("selected.log"));
        List<String> selectedCalls = readLines(root.resolve("calls.txt"));
        boolean exactSelection = selectedCode == 0 && selectedCalls.equals(List.of("alpha", "gamma"));
        passed &= check(exactSelection, "comma-separated CLI selection: " + selectedCalls, root.resolve("selected.log"));

        clearBoots(root);
        selected.set(selected.size() - 1, "alpha,missing");
        boolean unknownRejected = run(selected, root.resolve("unknown.log")) != 0
                && !Files.exists(root.resolve("calls.txt"));
        passed &= check(unknownRejected, "unknown selected names fail before any boot", root.resolve("unknown.log"));
        selected.set(selected.size() - 1, " ");
        boolean emptyRejected = run(selected, root.resolve("empty.log")) != 0
                && !Files.exists(root.resolve("calls.txt"));
        passed &= check(emptyRejected, "empty selection never expands to every scenario", root.resolve("empty.log"));
        selected.set(selected.size() - 1, "alpha,gamma");

        List<String> listing = new ArrayList<>(command);
        listing.add(bash ? "--list" : "-List");
        boolean listed = run(listing, root.resolve("list.log")) == 0
                && !Files.exists(root.resolve("calls.txt"))
                && readLines(root.resolve("list.log")).equals(List.of("alpha", "beta", "gamma"));
        passed &= check(listed, "listing selects names without booting", root.resolve("list.log"));
        List<String> shard = new ArrayList<>(command);
        shard.addAll(List.of(bash ? "--shard" : "-Shard", "1", bash ? "--of" : "-Of", "2"));
        passed &= checkTimeouts(root, shard, "shard", Map.of(), 0,
                List.of(budget("alpha", "<unset>"), budget("gamma", "<unset>")), null);

        Files.writeString(scenarios.resolve("beta.timeout"), "invalid but unselected\n");
        Files.writeString(scenarios.resolve("alpha.timeout"), "600\n");
        Files.writeString(scenarios.resolve("gamma.timeout"), "900\r\n");
        passed &= checkTimeouts(root, selected, "lf-crlf-sidecars", Map.of(), 0,
                List.of(budget("alpha", "600"), budget("gamma", "900")), null);
        Files.writeString(scenarios.resolve("alpha.timeout"), " \t000600\r\n\r\n");
        Files.writeString(scenarios.resolve("gamma.timeout"), "2147483647\n");
        passed &= checkTimeouts(root, selected, "normalized-and-maximum", Map.of(), 0,
                List.of(budget("alpha", "600"), budget("gamma", "2147483647")), null);

        Files.writeString(scenarios.resolve("alpha.timeout"), "900\r\n");
        Files.delete(scenarios.resolve("gamma.timeout"));
        List<String> caller = callerCommand(executable, bash, runner, root, "alpha,gamma");
        passed &= checkTimeouts(root, caller, "nonleak-and-caller-restoration", Map.of(), 0,
                List.of(budget("alpha", "900"), budget("gamma", "<unset>")), "<unset>");
        passed &= checkTimeouts(root, caller, "explicit-override-and-restoration",
                Map.of("SMOKE_EXPECT_TIMEOUT", "45"), 0,
                List.of(budget("alpha", "45"), budget("gamma", "45")), "45");
        Files.writeString(scenarios.resolve("beta.timeout"), "700\n");
        passed &= checkTimeouts(root, callerCommand(executable, bash, runner, root, "alpha,beta"),
                "failed-boot-restoration", Map.of(), 1,
                List.of(budget("alpha", "900"), budget("beta", "700")), "<unset>");

        String[][] invalidTimeouts = {
                {"blank", ""},
                {"whitespace", " \t\r\n"},
                {"zero", "0\n"},
                {"negative", "-1\r\n"},
                {"fraction", "1.5\n"},
                {"multiple-space", "900 600\n"},
                {"multiple-lines", "900\r\n600\r\n"},
                {"overflow", "2147483648\n"},
                {"huge", "999999999999999999999999999999\n"},
                {"shell-content", "$(echo 900)\n"},
                {"utf8-bom", "\ufeff900\n"},
                {"nul", "900\0ignored\n"}
        };
        for (String[] invalid : invalidTimeouts) {
            Files.writeString(scenarios.resolve("gamma.timeout"), invalid[1]);
            passed &= checkInvalidTimeout(root, selected, invalid[0], Map.of());
        }
        Files.writeString(scenarios.resolve("gamma.timeout"), "2147483648\n");
        passed &= checkInvalidTimeout(root, selected, "invalid-despite-override",
                Map.of("SMOKE_EXPECT_TIMEOUT", "45"));
        List<String> optionalChecks = new ArrayList<>(List.of(executable));
        if (!bash) {
            optionalChecks.addAll(List.of("-NoProfile", "-ExecutionPolicy", "Bypass", "-File"));
        }
        optionalChecks.add(Path.of("tools", "checks", "verify-optional-datapack" + suffix).toAbsolutePath().toString());
        if (!bash) {
            optionalChecks.add("-Root");
        }
        optionalChecks.add(Path.of("").toAbsolutePath().toString());
        Path optionalOutput = root.resolve("optional-packs.log");
        passed &= check(run(optionalChecks, optionalOutput) == 0,
                "optional pack preflight, initial disablement and isolated dispatch", optionalOutput);
        List<String> batchChecks = new ArrayList<>(List.of(executable));
        if (!bash) {
            batchChecks.addAll(List.of("-NoProfile", "-ExecutionPolicy", "Bypass", "-File"));
        }
        batchChecks.add(Path.of("tools", "checks", "Test-SmokesBatch" + suffix).toAbsolutePath().toString());
        if (!bash) {
            batchChecks.add("-RepositoryRoot");
        }
        batchChecks.add(Path.of("").toAbsolutePath().toString());
        Path batchOutput = root.resolve("shared-batch.log");
        passed &= check(run(batchChecks, batchOutput) == 0,
                "shared-world batch selection, fresh-world exclusions and one-boot dispatch", batchOutput);
        return passed;
    }

    private static List<String> callerCommand(String executable, boolean bash, Path runner, Path root, String only)
            throws IOException {
        if (bash) {
            Path caller = root.resolve("caller.sh");
            Files.writeString(caller, """
                    trap 'printf "CALLER_EXPECT_TIMEOUT=%s\\n" "${SMOKE_EXPECT_TIMEOUT-<unset>}"' EXIT
                    source "$1" "$2" --only "$3"
                    """);
            return List.of(executable, caller.toString(), runner.toString(), root.toString(), only);
        }
        String script = "& '" + runner.toString().replace("'", "''") + "' -Root '"
                + root.toString().replace("'", "''") + "' -Only '" + only + "'; "
                + "$code = $LASTEXITCODE; "
                + "$value = if ($null -eq $env:SMOKE_EXPECT_TIMEOUT) { '<unset>' } else { $env:SMOKE_EXPECT_TIMEOUT }; "
                + "Write-Output ('CALLER_EXPECT_TIMEOUT=' + $value); exit $code";
        return List.of(executable, "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script);
    }

    private static boolean checkTimeouts(Path root, List<String> command, String name,
                                         Map<String, String> environment, int expectedCode,
                                         List<String> expected, String caller) throws Exception {
        clearBoots(root);
        Path output = root.resolve(name + ".log");
        int code = run(command, output, environment);
        List<String> observed = readLines(root.resolve("timeouts.txt"));
        boolean passed = code == expectedCode && observed.equals(expected)
                && (caller == null || readOutput(output).contains("CALLER_EXPECT_TIMEOUT=" + caller));
        return check(passed, "assertion budgets " + name + ": " + observed, output);
    }

    private static boolean checkInvalidTimeout(Path root, List<String> command, String name,
                                               Map<String, String> environment) throws Exception {
        clearBoots(root);
        Path output = root.resolve("invalid-" + name + ".log");
        int code = run(command, output, environment);
        String diagnostic = readOutput(output);
        boolean rejected = code != 0 && !Files.exists(root.resolve("calls.txt"))
                && !Files.exists(root.resolve("timeouts.txt"))
                && diagnostic.contains("Invalid smoke assertion timeout")
                && diagnostic.contains("gamma.timeout") && diagnostic.contains("positive integer");
        return check(rejected, "invalid " + name + " fails before any boot with a sidecar diagnostic", output);
    }

    private static String budget(String name, String raw) {
        return name + "|" + raw + "|" + (raw.equals("<unset>") ? "120" : raw) + "|37";
    }

    private static void clearBoots(Path root) throws IOException {
        Files.deleteIfExists(root.resolve("calls.txt"));
        Files.deleteIfExists(root.resolve("timeouts.txt"));
    }

    private static List<String> readLines(Path path) throws IOException {
        return Files.exists(path) ? Files.readAllLines(path) : List.of();
    }

    private static String readOutput(Path path) throws IOException {
        return new String(Files.readAllBytes(path), Charset.defaultCharset());
    }

    private static boolean check(boolean passed, String description, Path output) throws IOException {
        System.out.println((passed ? "PASS " : "FAIL ") + description);
        if (!passed) {
            System.out.println(readOutput(output));
        }
        return passed;
    }

    private static int run(List<String> command, Path output) throws Exception {
        return run(command, output, Map.of());
    }

    private static int run(List<String> command, Path output, Map<String, String> environment) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(output.toFile());
        builder.environment().remove("SMOKE_EXPECT_TIMEOUT");
        builder.environment().put("SMOKE_TIMEOUT", "37");
        builder.environment().putAll(environment);
        Process process = builder.start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly().waitFor();
            throw new IOException("The fixture smoke runner did not finish within 60 seconds");
        }
        return process.exitValue();
    }
}
