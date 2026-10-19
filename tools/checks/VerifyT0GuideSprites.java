package io.github.ezequiel24123z.grindless.registry;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

/** JDK-only source-pixel contracts; not artistic quality or native client acceptance. */
public final class VerifyT0GuideSprites {
    private static final List<String> GUIDES = List.of("process_atlas", "quest_book", "field_guide");
    private static final Map<String, String> PROTECTED = Map.of(
            "mineral_aggregate", "1961141aeb4e06353b618e441c5f2fd45cd0874acd707b5e271f6c9f3039d753",
            "biomass", "732b4a1ec7a1bcdb1f8d69b284f44b1d935c817fd5350a985f8d4f6b344a766c",
            "slag", "0525db4efa809072fb74e7e5c4cad3b49a2d5e117fdccb3afb399a78713d720a",
            "ceramic_grog", "913a50c8b4b52b1d4c9a131d3bac3db7a15f77ed29152ebfb5bd27be08cc99e3",
            "unfired_ceramic", "b4a1b37ff600aa6bce5dddd703ea997055560f3cc7836d6432a66824b0b11810",
            "ceramic_insulator", "d662747720ae935857aa13ddd968f51cb9629e13936c25b4ccb926de4dadbd30",
            "data_core", "0ac2750b8cdcc91e0dafac833789e9589a3aeb034928629cae2a3ae531abf5ce",
            "calibrated_data_core", "ebe3995dad658ee33cc7ad634b33536723e46b72fcbee12d978d42ea3c04dfa1",
            "relay_matrix", "7bd7ac25847d49ad67a695f8fc14b964f0e07bfaaa5fd528102fa7863d348924");
    private static int checks;
    private static int failures;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            throw new IllegalArgumentException("usage: VerifyT0GuideSprites [repository-root]");
        }
        Path root = args.length == 0 ? Path.of(".") : Path.of(args[0]);
        Path items = root.resolve("common/src/main/resources/assets/grindless/textures/item");
        Map<String, BufferedImage> images = new LinkedHashMap<>();
        for (String name : GUIDES) {
            BufferedImage image = ImageIO.read(items.resolve(name + ".png").toFile());
            yes(name + " is a native 16x16 RGBA PNG",
                    nativeSprite(image) && image.getColorModel().hasAlpha());
            if (!nativeSprite(image)) {
                continue;
            }
            images.put(name, image);
            yes(name + " uses only hard transparent/opaque pixels", binaryAlpha(image));
            yes(name + " keeps a transparent one-pixel safety margin", clearBorder(image));
            yes(name + " has an exposed cream paper surface", count(image, VerifyT0GuideSprites::paper) >= 8);
            yes(name + " has a distinct warm binding", count(image, VerifyT0GuideSprites::binding) >= 6);
            yes(name + " occupies a bounded inventory silhouette",
                    count(image, pixel -> (pixel >>> 24) == 255) >= 80
                            && count(image, pixel -> (pixel >>> 24) == 255) <= 180);
            yes(name + " rejects a fully opaque frame", !clearBorder(opaqueFrame(image)));
            yes(name + " rejects semitransparent pixel filtering", !binaryAlpha(filteredPixel(image)));
            yes(name + " rejects a cover-only monochrome fill",
                    count(monochrome(image), VerifyT0GuideSprites::paper) < 8);
        }
        for (int first = 0; first < GUIDES.size(); first++) {
            for (int second = first + 1; second < GUIDES.size(); second++) {
                BufferedImage left = images.get(GUIDES.get(first));
                BufferedImage right = images.get(GUIDES.get(second));
                yes(GUIDES.get(first) + " and " + GUIDES.get(second) + " have different silhouettes",
                        left != null && right != null && !Arrays.equals(alpha(left), alpha(right)));
            }
        }
        BufferedImage atlas = images.get("process_atlas");
        BufferedImage quest = images.get("quest_book");
        BufferedImage field = images.get("field_guide");
        yes("the Atlas retains three cyan schematic nodes",
                rgb(atlas, 6, 5) == 0x26C6DA && rgb(atlas, 10, 5) == 0x26C6DA
                        && rgb(atlas, 8, 8) == 0x26C6DA);
        yes("the Quest Book retains three legible list rows",
                paper(rgb(quest, 8, 3)) && paper(rgb(quest, 8, 6)) && paper(rgb(quest, 8, 9)));
        yes("the Field Guide has a teal bookmark above its open pages",
                rgb(field, 11, 2) == 0x26A69A && paper(rgb(field, 10, 7)));
        for (Map.Entry<String, String> entry : PROTECTED.entrySet()) {
            byte[] bytes = Files.readAllBytes(items.resolve(entry.getKey() + ".png"));
            yes(entry.getKey() + " retains the exact reviewed 1ced812 PNG bytes",
                    entry.getValue().equals(sha256(bytes)));
        }
        System.out.println(checks + " T0 GUIDE SPRITE CHECKS; " + failures + " FAILURES");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static boolean nativeSprite(BufferedImage image) {
        return image != null && image.getWidth() == 16 && image.getHeight() == 16;
    }

    private static boolean binaryAlpha(BufferedImage image) {
        return count(image, pixel -> (pixel >>> 24) != 0 && (pixel >>> 24) != 255) == 0;
    }

    private static boolean clearBorder(BufferedImage image) {
        for (int position = 0; position < 16; position++) {
            if ((image.getRGB(position, 0) >>> 24) != 0 || (image.getRGB(position, 15) >>> 24) != 0
                    || (image.getRGB(0, position) >>> 24) != 0 || (image.getRGB(15, position) >>> 24) != 0) {
                return false;
            }
        }
        return true;
    }

    private static int count(BufferedImage image, IntPredicate predicate) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                if ((pixel >>> 24) != 0 && predicate.test(pixel)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static boolean paper(int pixel) {
        int red = pixel >> 16 & 255;
        int green = pixel >> 8 & 255;
        int blue = pixel & 255;
        return red >= 140 && green >= 125 && blue >= 95
                && red >= green && green >= blue && red - blue >= 20 && red - blue <= 80;
    }

    private static boolean binding(int pixel) {
        int red = pixel >> 16 & 255;
        int green = pixel >> 8 & 255;
        int blue = pixel & 255;
        return red >= 35 && red <= 175 && green <= 150 && blue >= 30 && blue <= 100
                && red > green && green > blue && red - blue >= 10 && red - blue <= 100;
    }

    private static BufferedImage copy(BufferedImage image) {
        BufferedImage copy = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        copy.setRGB(0, 0, 16, 16, image.getRGB(0, 0, 16, 16, null, 0, 16), 0, 16);
        return copy;
    }

    private static BufferedImage opaqueFrame(BufferedImage image) {
        BufferedImage mutation = copy(image);
        mutation.setRGB(0, 0, 0xFF26C6DA);
        return mutation;
    }

    private static BufferedImage filteredPixel(BufferedImage image) {
        BufferedImage mutation = copy(image);
        mutation.setRGB(7, 7, 0x8026C6DA);
        return mutation;
    }

    private static BufferedImage monochrome(BufferedImage image) {
        BufferedImage mutation = copy(image);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if ((mutation.getRGB(x, y) >>> 24) != 0) {
                    mutation.setRGB(x, y, 0xFF26C6DA);
                }
            }
        }
        return mutation;
    }

    private static int[] alpha(BufferedImage image) {
        return Arrays.stream(image.getRGB(0, 0, 16, 16, null, 0, 16)).map(pixel -> pixel >>> 24).toArray();
    }

    private static int rgb(BufferedImage image, int x, int y) {
        return image == null ? -1 : image.getRGB(x, y) & 0xFFFFFF;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("A Java 17 runtime must provide SHA-256", impossible);
        }
    }

    private static void yes(String what, boolean actual) {
        checks++;
        if (!actual) {
            failures++;
        }
        System.out.println((actual ? "  ok   " : "  FAIL ") + what);
    }
}
