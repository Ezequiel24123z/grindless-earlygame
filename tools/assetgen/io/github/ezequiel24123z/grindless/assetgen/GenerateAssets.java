package io.github.ezequiel24123z.grindless.assetgen;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Writes every generated asset into the mod's resource tree.
 *
 * <pre>
 *     powershell -ExecutionPolicy Bypass -File .\tools\generate-assets.ps1 -Root .
 * </pre>
 *
 * <p>Deterministic: running it twice produces byte-identical files, so regenerating never shows up
 * as a spurious diff and a review only ever sees real changes.
 *
 * <p>This covers the matrix and the casings — the bulk by count, and the part where consistency is
 * the quality. It deliberately does <b>not</b> produce hero item sprites, complex models, entity
 * animation or music; ADR-0048 lists those as known gaps rather than leaving a future session to
 * assume art is handled.
 */
public final class GenerateAssets {

    /**
     * Starter palettes.
     *
     * <p>Only the colour is declared per material — the point of the matrix is that adding one is
     * a single value, not an art task. The real set comes from the runtime tag scan (ADR-0004);
     * these cover the bootstrap materials and prove the approach across warm, cool and neutral
     * metals, which is where a shared template is most likely to fall apart.
     */
    private static final List<Palette> PALETTES = List.of(
            Palette.of("copper", 0xB87333),
            Palette.of("iron", 0xD8D8D8),
            Palette.of("gold", 0xFFD700),
            Palette.of("tin", 0xDCE4E8),
            Palette.of("lead", 0x6E7A8A),
            Palette.of("silver", 0xE8E8F0),
            Palette.of("nickel", 0xC9C9A8),
            Palette.of("zinc", 0xB8C4C8),
            Palette.of("steel", 0x4A6E8A),
            Palette.of("aluminium", 0xCFD8DC),
            Palette.of("titanium", 0x9FA8AE),
            Palette.of("tungsten", 0x5A5A66));

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("usage: GenerateAssets <repository-root>");
            System.exit(2);
        }
        File root = new File(args[0]);
        File items = resourceDir(root, "textures/item");
        File blocks = resourceDir(root, "textures/block");
        File sounds = resourceDir(root, "sounds");

        int written = 0;

        // ---- the form x material matrix ----
        for (Palette palette : PALETTES) {
            String m = palette.name();
            written += write(items, m + "_ingot", FormTextures.ingot(palette));
            written += write(items, m + "_plate", FormTextures.plate(palette));
            written += write(items, m + "_gear", FormTextures.gear(palette));
            written += write(items, m + "_rod", FormTextures.rod(palette));
            written += write(items, m + "_bolt", FormTextures.bolt(palette));
            written += write(items, m + "_ring", FormTextures.ring(palette));
            written += write(items, m + "_dust", FormTextures.dust(palette));
            written += write(items, m + "_crushed", FormTextures.crushed(palette));
            written += write(items, m + "_nugget", FormTextures.nugget(palette));
        }

        // ---- machine casings ----
        // Names are kept deliberately short. Minecraft's asset layout is already deep
        // (assets/grindless/textures/block/...), and on Windows a long checkout path plus a long
        // asset path exceeds MAX_PATH — the hazard ADR-0024 documents for source files applies
        // just as much here. "face_vent_on" rather than "machine_face_vent_active" buys a dozen
        // characters for nothing.
        written += write(blocks, "casing_side",
                MachineTextures.side(MachineTextures.CASING));
        written += write(blocks, "casing_top",
                MachineTextures.top(MachineTextures.CASING));
        for (MachineTextures.Motif motif : MachineTextures.Motif.values()) {
            String name = "face_" + motif.name().toLowerCase();
            written += write(blocks, name, MachineTextures.face(
                    MachineTextures.CASING, MachineTextures.ACCENT, motif, false));
            written += write(blocks, name + "_on", MachineTextures.face(
                    MachineTextures.CASING, MachineTextures.ACCENT, motif, true));
        }

        // ---- sounds ----
        // Loops are built from harmonics of their own loop frequency and must not be faded, or
        // the fade reintroduces the discontinuity the construction exists to avoid.
        SoundSynth.write(new File(sounds, "machine_hum.wav"),
                SoundSynth.hum(1.0, 60, 0.4), false);
        SoundSynth.write(new File(sounds, "machine_hum_heavy.wav"),
                SoundSynth.hum(1.0, 42, 0.6), false);
        SoundSynth.write(new File(sounds, "pylon_link.wav"),
                SoundSynth.chirp(0.45, 420, 1320), true);
        SoundSynth.write(new File(sounds, "relay_click.wav"),
                SoundSynth.click(0.08, 0.5), true);
        SoundSynth.write(new File(sounds, "brownout_alarm.wav"),
                SoundSynth.alarm(0.6, 330, 247), true);
        written += 5;

        writeProvenance(root, written);
        System.out.println("Generated " + written + " assets under "
                + root.toPath().resolve("common/src/main/resources/assets/grindless"));
    }

    /**
     * Records that everything here is generated, and by what.
     *
     * <p>The README's provenance claim — that no Minecraft asset is redistributed and no
     * third-party art is vendored — is a promise, and a promise is worth more with a marker beside
     * the files it covers.
     */
    private static void writeProvenance(File root, int count) throws IOException {
        File readme = new File(assetRoot(root), "GENERATED.md");
        String text = """
                # Generated assets

                Every file under this directory is produced by `tools/assetgen` and written by
                `tools/generate-assets.ps1`. **Do not edit them by hand** — the next run overwrites
                them. Change the generator instead.

                Generation is deterministic: the same generator always produces byte-identical
                output, so regenerating never shows up as a spurious diff.

                Nothing here is copied from Minecraft or from any third-party mod, which is what
                lets the README state its provenance without qualification.

                What is **not** generated, and is therefore missing rather than merely plain, is
                listed in ADR-0048: hero item sprites, complex models, entity animation and music.
                A future session should treat those as known gaps with no owner, not as oversights.

                Current output: %d files.
                """.formatted(count);
        Files.writeString(readme.toPath(), text, StandardCharsets.UTF_8);
    }

    private static int write(File dir, String name, BufferedImage image) throws IOException {
        ImageIO.write(image, "PNG", new File(dir, name + ".png"));
        return 1;
    }

    private static File assetRoot(File root) {
        return new File(root, "common/src/main/resources/assets/grindless");
    }

    private static File resourceDir(File root, String path) {
        File dir = new File(assetRoot(root), path);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("could not create " + dir);
        }
        return dir;
    }

    private GenerateAssets() {
    }
}
