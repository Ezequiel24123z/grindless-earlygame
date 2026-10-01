package io.github.ezequiel24123z.grindless.material;

import io.github.ezequiel24123z.grindless.Grindless;

/**
 * The runtime, tag-driven material registry.
 *
 * <p>Grindless ships no hardcoded material list. At runtime it reads the tags actually present in
 * the loaded pack — {@code forge:ores/*}, {@code forge:ingots/*}, {@code forge:raw_materials/*},
 * {@code forge:dusts/*}, {@code forge:gems/*} and the Fabric {@code c:} conventions — and builds
 * both the material set and the whole processing chain from what it finds. It is rebuilt on
 * datapack reload so pack changes apply without a restart.
 *
 * <p>Placeholder. {@link #bootstrap()} currently only reserves the call site: tag contents are not
 * available at registration time, so the real scan runs on datapack load.
 */
public final class MaterialRegistry {

    private MaterialRegistry() {
    }

    public static void bootstrap() {
        Grindless.LOG.debug("material registry bootstrapped (tag scan runs on datapack load)");
    }
}
