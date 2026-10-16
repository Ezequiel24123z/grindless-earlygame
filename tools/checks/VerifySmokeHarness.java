package io.github.ezequiel24123z.grindless.testing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/** Exercises the real platform smoke runner against three isolated fake boots, without starting Minecraft. */
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
                #!/usr/bin/env bash
                name="$(basename "$SMOKE_COMMANDS" .commands)"
                printf '%s\\n' "$name" >> "$1/calls.txt"
                [ "$name" != "beta" ]
                """ : """
                param([string]$Root)
                $name = [System.IO.Path]::GetFileNameWithoutExtension($env:SMOKE_COMMANDS)
                Add-Content -LiteralPath (Join-Path $Root 'calls.txt') -Value $name -Encoding ASCII
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
        Process process = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(output.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly().waitFor();
            throw new IOException("The fixture smoke runner did not finish within 60 seconds");
        }
        List<String> calls = Files.exists(root.resolve("calls.txt"))
                ? Files.readAllLines(root.resolve("calls.txt")) : List.of();
        boolean dispatched = calls.equals(List.of("alpha", "beta", "gamma"));
        boolean failed = process.exitValue() != 0;
        String report = Files.exists(root.resolve("forge/run/smoke-report.html"))
                ? Files.readString(root.resolve("forge/run/smoke-report.html")) : "";
        boolean reported = report.contains("1 failed") && report.contains("<b>2</b> passed");
        System.out.println((dispatched ? "PASS " : "FAIL ") + "actual scenario dispatch: " + calls);
        System.out.println((failed ? "PASS " : "FAIL ") + "a failed scenario fails the runner");
        System.out.println((reported ? "PASS " : "FAIL ") + "report preserves two passes and one failure");
        if (!dispatched || !failed || !reported) {
            System.out.println(Files.readString(output));
        }
        return dispatched && failed && reported;
    }
}
