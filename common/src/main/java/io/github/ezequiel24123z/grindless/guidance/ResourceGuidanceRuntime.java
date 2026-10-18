package io.github.ezequiel24123z.grindless.guidance;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** Loads one prepared classpath asset at client setup, never a repository or source auditor. */
public final class ResourceGuidanceRuntime {
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static volatile Loaded current = new Loaded(null, "not_loaded");

    private ResourceGuidanceRuntime() {
    }

    public record Loaded(ResourceGuidance.Document document, String diagnostic) {
        public boolean available() {
            return document != null;
        }
    }

    public static void initialize(ClassLoader loader, Consumer<String> diagnostic) {
        current = read(loader);
        if (!current.available()) {
            diagnostic.accept("Prepared resource guidance unavailable (" + current.diagnostic()
                    + "); classifications are unknown. Regenerate and check " + ResourceGuidance.ASSET + ".");
        }
    }

    public static Loaded current() {
        return current;
    }

    /** Package resource I/O happens only here; presenters use the immutable cached document. */
    public static Loaded read(ClassLoader loader) {
        try (InputStream stream = loader.getResourceAsStream(ResourceGuidance.ASSET)) {
            if (stream == null) {
                return new Loaded(null, "missing_asset");
            }
            byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) {
                return new Loaded(null, "oversized_asset");
            }
            ResourceGuidance.Document document = ResourceGuidanceJson.read(new String(bytes, StandardCharsets.UTF_8));
            if (document.resources().isEmpty()) {
                return new Loaded(null, "empty_declarations");
            }
            return new Loaded(document, null);
        } catch (IOException | RuntimeException invalid) {
            return new Loaded(null, "invalid_asset");
        }
    }
}
