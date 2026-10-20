package io.github.ezequiel24123z.grindless.assetgen;

import io.github.ezequiel24123z.grindless.registry.BlockCatalogue.Geometry;
import io.github.ezequiel24123z.grindless.structure.WorkshopGeometry;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A deterministic assembly preview from the same boxes and texture functions as the shipped models. */
final class WorkshopPreview {

    private record Point(double x, double y, double z) {
    }

    private record Face(Point a, Point b, Point c, Point d, BufferedImage texture, int shade) {
        double depth() {
            return depth(a) + depth(b) + depth(c) + depth(d);
        }

        private static double depth(Point point) {
            return point.x() - point.z() + point.y();
        }
    }

    private WorkshopPreview() {
    }

    static int write(File root) throws IOException {
        BufferedImage preview = new BufferedImage(720, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = preview.createGraphics();
        try {
            graphics.setColor(new Color(0x141820));
            graphics.fillRect(0, 0, preview.getWidth(), preview.getHeight());
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            drawAssembly(graphics, 36, "idle");
            drawAssembly(graphics, 396, "running");
        } finally {
            graphics.dispose();
        }
        File output = new File(root, "docs/assets/t0-settling-preview.png");
        Files.createDirectories(output.toPath().getParent());
        if (!ImageIO.write(preview, "png", output)) {
            throw new IOException("No PNG writer for the settling preview");
        }
        return 1;
    }

    private static void drawAssembly(Graphics2D graphics, int originX, String status) {
        List<Face> faces = new ArrayList<>();
        Geometry[] segments = {Geometry.SETTLER, Geometry.CHANNEL, Geometry.SCREEN};
        for (int index = 0; index < segments.length; index++) {
            Geometry geometry = segments[index];
            for (WorkshopGeometry.Box box : WorkshopGeometry.boxes(geometry)) {
                double z0 = box.z0() + index * 16;
                double z1 = box.z1() + index * 16;
                Point topFrontLeft = new Point(box.x0(), box.y1(), z0);
                Point topFrontRight = new Point(box.x1(), box.y1(), z0);
                Point topBackRight = new Point(box.x1(), box.y1(), z1);
                Point topBackLeft = new Point(box.x0(), box.y1(), z1);
                faces.add(new Face(topFrontLeft, topFrontRight, topBackRight, topBackLeft,
                        texture(geometry, box.top(), status), 0));
                faces.add(new Face(topFrontLeft, topFrontRight,
                        new Point(box.x1(), box.y0(), z0), new Point(box.x0(), box.y0(), z0),
                        texture(geometry, box.front() == null ? box.side() : box.front(), status), 24));
                faces.add(new Face(topFrontRight, topBackRight,
                        new Point(box.x1(), box.y0(), z1), new Point(box.x1(), box.y0(), z0),
                        texture(geometry, box.side(), status), 64));
            }
        }
        faces.sort(Comparator.comparingDouble(Face::depth));
        for (Face face : faces) {
            drawFace(graphics, originX, face);
        }
    }

    private static BufferedImage texture(Geometry geometry, String slot, String status) {
        return switch (slot) {
            case "#front" -> MachineArt.front(geometry, status);
            case "#top" -> MachineArt.top(geometry);
            case "#cap" -> MachineTextures.top(MachineTextures.CASING);
            case "#side", "#base" -> MachineTextures.side(MachineTextures.CASING);
            default -> throw new IllegalArgumentException("Unknown workshop texture slot: " + slot);
        };
    }

    private static void drawFace(Graphics2D graphics, int originX, Face face) {
        Point[] points = {face.a(), face.b(), face.c(), face.d()};
        int[] x = new int[4];
        int[] y = new int[4];
        for (int index = 0; index < points.length; index++) {
            x[index] = originX + (int) Math.round(4 * (points[index].x() + points[index].z()));
            y[index] = 184 + (int) Math.round(2 * (points[index].x() - points[index].z()) - 4 * points[index].y());
        }
        Polygon polygon = new Polygon(x, y, 4);
        Shape clip = graphics.getClip();
        graphics.clip(polygon);
        AffineTransform transform = new AffineTransform(
                (x[1] - x[0]) / 16.0, (y[1] - y[0]) / 16.0,
                (x[3] - x[0]) / 16.0, (y[3] - y[0]) / 16.0, x[0], y[0]);
        graphics.drawImage(face.texture(), transform, null);
        if (face.shade() > 0) {
            graphics.setColor(new Color(0, 0, 0, face.shade()));
            graphics.fill(polygon);
        }
        graphics.setClip(clip);
    }
}
