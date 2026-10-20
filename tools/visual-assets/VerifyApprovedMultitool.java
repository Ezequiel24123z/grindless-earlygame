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
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

/** JDK-only contract for the one approved-source seam; no Minecraft runtime. */
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
        System.out.println("ALL " + checks + " APPROVED MULTITOOL JDK CHECKS PASSED");
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
