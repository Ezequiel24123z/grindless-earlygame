import io.github.ezequiel24123z.grindless.assetgen.GenerateAssets;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

/** JDK-only contracts for approved source and normal text publication; no Minecraft runtime. */
public final class VerifyApprovedMultitool {
    private static int checks;
    private static final Method READER;

    static {
        try {
            READER = GenerateAssets.class.getDeclaredMethod("approvedMultitool", File.class);
            READER.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: VerifyApprovedMultitool <repository-root> <new-fixture-directory>");
        }
        Path root = Path.of(args[0]);
        Path fixtures = Path.of(args[1]);
        Files.createDirectory(fixtures);
        byte[] approved = Files.readAllBytes(root.resolve("tools/visual-assets/approved/multitool.png"));
        check(Arrays.equals(approved, read(root)), "actual approved PNG returns byte-identically");

        Path tamper = fixture(fixtures, "tamper", approved);
        Files.writeString(metadata(tamper), "0".repeat(64), StandardCharsets.US_ASCII);
        rejects(tamper, "SHA-256 metadata", "source/metadata mismatch refused");
        Files.writeString(metadata(tamper), "invalid", StandardCharsets.US_ASCII);
        rejects(tamper, "Invalid approved", "malformed metadata refused");

        BufferedImage wrongSize = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Path dimensions = imageFixture(fixtures, "dimensions", wrongSize, "PNG");
        rejects(dimensions, "16x16", "wrong native dimensions refused");
        BufferedImage softAlpha = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        softAlpha.setRGB(8, 8, 0x7FC9A227);
        rejects(imageFixture(fixtures, "alpha", softAlpha, "PNG"), "binary alpha", "partial alpha refused");
        rejects(imageFixture(fixtures, "format", softAlpha, "GIF"), "not a PNG", "format autodetection cannot accept GIF");

        Path missing = fixture(fixtures, "missing", approved);
        Files.delete(source(missing));
        rejects(missing, null, "missing source refused without fallback");
        try {
            GenerateAssets.main(new String[]{tamper.toString()});
            throw new AssertionError("Expected normal-generation preflight failure");
        } catch (IOException expected) {
            check(!Files.exists(tamper.resolve("common")), "invalid source fails before normal output mutation");
        }

        Path guides = fixtures.resolve("guides-only");
        Files.createDirectory(guides);
        GenerateAssets.main(new String[]{guides.toString(), "--t0-guides-only"});
        Path items = guides.resolve("common/src/main/resources/assets/grindless/textures/item");
        try (var paths = Files.list(items)) {
            check(paths.count() == 3, "guides-only still writes exactly three files without approved-source inputs");
        }
        for (String name : new String[]{"process_atlas", "quest_book", "field_guide"}) {
            check(Files.isRegularFile(items.resolve(name + ".png")), "guides-only preserves " + name);
        }
        textPublication(fixtures.resolve("text-output"));
        System.out.println("ALL " + checks + " APPROVED MULTITOOL JDK CHECKS PASSED");
    }

    private static void textPublication(Path fixtures) throws Exception {
        Method writer = GenerateAssets.class.getDeclaredMethod("writeText", File.class, String.class);
        writer.setAccessible(true);
        String lf = "say STATE-OK first\nsay STATE-OK second\n";
        for (String eol : new String[]{"\n", "\r\n"}) {
            String style = eol.length() == 1 ? "LF" : "CRLF";
            Path file = fixtures.resolve(style).resolve("states.expect");
            Files.createDirectories(file.getParent());
            Files.writeString(file, lf.replace("\n", eol), StandardCharsets.UTF_8);
            byte[] original = Files.readAllBytes(file);
            Files.setLastModifiedTime(file, FileTime.fromMillis(1_000_000));
            FileTime modified = Files.getLastModifiedTime(file);
            for (String desired : new String[]{lf, lf.replace("\n", "\r\n")}) {
                check(Integer.valueOf(1).equals(writer.invoke(null, file.toFile(), desired)),
                        style + " no-op still counts the logical output");
                check(Arrays.equals(original, Files.readAllBytes(file)),
                        style + " equivalent LF/CRLF input preserves exact bytes");
                check(modified.equals(Files.getLastModifiedTime(file)),
                        style + " equivalent LF/CRLF input never rewrites the file");
            }
            String changed = lf.replace("second", "added_state");
            check(Integer.valueOf(1).equals(writer.invoke(null, file.toFile(), changed)),
                    style + " real state change still counts the output");
            check(changed.equals(Files.readString(file, StandardCharsets.UTF_8)),
                    style + " real state change publishes the exact new content");
            check(!modified.equals(Files.getLastModifiedTime(file)),
                    style + " real state change is not suppressed");
            String whitespace = changed.replace("say STATE-OK", "say  STATE-OK");
            writer.invoke(null, file.toFile(), whitespace);
            check(whitespace.equals(Files.readString(file, StandardCharsets.UTF_8)),
                    style + " comparison does not erase non-EOL whitespace changes");
        }
        Path created = fixtures.resolve("new").resolve("nested").resolve("states.commands");
        check(Integer.valueOf(1).equals(writer.invoke(null, created.toFile(), lf)),
                "new text output still creates missing parent directories and counts once");
        check(lf.equals(Files.readString(created, StandardCharsets.UTF_8)),
                "new output publishes the requested bytes");
    }

    private static byte[] read(Path root) throws Exception {
        try {
            return (byte[]) READER.invoke(null, root.toFile());
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IOException failure) {
                throw failure;
            }
            throw e;
        }
    }

    private static void rejects(Path root, String message, String label) throws Exception {
        try {
            read(root);
            throw new AssertionError("Expected rejection: " + label);
        } catch (IOException expected) {
            check(message == null || expected.getMessage().contains(message), label);
        }
    }

    private static Path fixture(Path parent, String name, byte[] bytes) throws Exception {
        Path root = parent.resolve(name);
        Files.createDirectories(source(root).getParent());
        Files.write(source(root), bytes);
        Files.writeString(metadata(root), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),
                StandardCharsets.US_ASCII);
        return root;
    }

    private static Path imageFixture(Path parent, String name, BufferedImage image, String format) throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        if (!ImageIO.write(image, format, bytes)) {
            throw new IOException("No JDK image writer for test format " + format);
        }
        return fixture(parent, name, bytes.toByteArray());
    }

    private static Path source(Path root) {
        return root.resolve("tools/visual-assets/approved/multitool.png");
    }

    private static Path metadata(Path root) {
        return root.resolve("tools/visual-assets/approved/multitool.sha256");
    }

    private static void check(boolean result, String label) {
        if (!result) {
            throw new AssertionError(label);
        }
        checks++;
    }
}
