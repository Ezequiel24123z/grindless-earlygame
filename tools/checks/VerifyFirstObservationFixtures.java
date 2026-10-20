package io.github.ezequiel24123z.grindless.testing;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs the shared source-only contracts, not Minecraft entity or fluid scheduling. */
public final class VerifyFirstObservationFixtures {

    public static void main(String[] args) throws Exception {
        boolean windows = System.getProperty("os.name").startsWith("Windows");
        Path script = Path.of("tools", "checks", "verify-first-observation-fixtures"
                + (windows ? ".ps1" : ".sh")).toAbsolutePath();
        List<String> command = windows
                ? List.of("powershell", "-NoLogo", "-NoProfile", "-NonInteractive",
                        "-ExecutionPolicy", "Bypass", "-File", script.toString())
                : List.of("bash", script.toString());
        Process process = new ProcessBuilder(command).inheritIO().start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Source-only fixture checks exceeded 60 seconds");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException("Source-only fixture checks failed: " + process.exitValue());
        }
    }
}
