package io.github.ezequiel24123z.grindless;

/**
 * Runtime configuration.
 *
 * <p>Placeholder. The real implementation reads a config file and exposes the knobs the design
 * calls for — vein richness, replication pricing multipliers, the fabrication gate toggle and the
 * auto-void safety defaults. It is loaded before any registry so registration can react to it.
 */
public final class GrindlessConfig {

    private GrindlessConfig() {
    }

    public static void load() {
        Grindless.LOG.debug("configuration loaded (defaults; no config file yet)");
    }
}
