package io.github.ezequiel24123z.grindless.menu;

import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;

/** Structural UI contracts; these checks do not claim a rendered client pass. */
public final class VerifyMachineUi {

    private static final Path COMMON = Path.of("common/src/main/java/io/github/ezequiel24123z/grindless");
    private static int assertions;
    private static int failures;

    public static void main(String[] args) throws IOException {
        for (MachineMenuKind kind : MachineMenuKind.values()) {
            List<MachineMenuLayout.ItemSlot> slots = MachineMenuLayout.itemSlots(kind);
            check(kind + " preserves its complete physical inventory", slots.size() == kind.size());
            check(kind + " preserves output-only slots",
                    slots.stream().filter(MachineMenuLayout.ItemSlot::output).count() == kind.outputs());
            HashSet<String> positions = new HashSet<>();
            for (int index = 0; index < slots.size(); index++) {
                var slot = slots.get(index);
                check(kind + " preserves storage index " + index, slot.index() == index);
                check(kind + " preserves input/output boundary " + index,
                        slot.output() == (index >= kind.inputs()));
                check(kind + " keeps slot " + index + " above status",
                        slot.x() >= 8 && slot.x() + 16 < MachineMenuLayout.WIDTH - 7
                                && slot.y() >= 26 && slot.y() + 16 < MachineMenuLayout.STATUS_Y);
                check(kind + " has a distinct position for slot " + index,
                        positions.add(slot.x() + ":" + slot.y()));
                int arrowX = MachineMenuLayout.progressX(kind);
                check(kind + " keeps its progress decoration off slot " + index,
                        slot.x() + 16 <= arrowX || slot.x() >= arrowX + 24
                                || slot.y() + 16 <= 35 || slot.y() >= 52);
            }
        }
        check("settling keeps all five output positions, not a two-output projection",
                MachineMenuLayout.itemSlots(MachineMenuKind.SETTLING_TROUGH).stream()
                        .filter(MachineMenuLayout.ItemSlot::output)
                        .map(slot -> slot.x() + ":" + slot.y()).toList()
                        .equals(List.of("107:26", "125:26", "143:26", "107:44", "125:44")));
        check("the status has room for four native text lines before the inventory label",
                MachineMenuLayout.STATUS_Y + 4 * 9 <= MachineMenuLayout.INVENTORY_LABEL_Y - 4);
        check("the inventory label does not intersect the first row",
                MachineMenuLayout.INVENTORY_LABEL_Y + 9 < MachineMenuLayout.INVENTORY_Y - 1);
        check("three inventory rows and the hotbar remain distinct",
                MachineMenuLayout.INVENTORY_Y + 2 * 18 + 16 < MachineMenuLayout.HOTBAR_Y - 1);
        check("the full hotbar stays inside the screen",
                MachineMenuLayout.HOTBAR_Y + 16 < MachineMenuLayout.HEIGHT - 3);

        String menu = Files.readString(COMMON.resolve("menu/ProcessMachineMenu.java"));
        String screen = Files.readString(COMMON.resolve("client/ProcessMachineScreen.java"));
        String jei = Files.readString(Path.of(
                "forge/src/main/java/io/github/ezequiel24123z/grindless/forge/jei/GrindlessProcessCategory.java"));
        check("the menu uses the shared physical slot schema",
                menu.contains("MachineMenuLayout.itemSlots(kind)"));
        check("player slots still use 27 inventory slots followed by nine hotbar slots",
                menu.contains("col + row * 9 + 9") && menu.contains("row < 3") && menu.contains("col < 9"));
        check("every real slot receives a background even when empty",
                screen.contains("for (Slot slot : menu.slots)") && !screen.contains("slot.hasItem()"));
        check("site faults wrap instead of being cut to one line",
                screen.contains("font.split(statusLine(), imageWidth - 16)")
                        && !screen.contains("plainSubstrByWidth"));
        check("only actual recipe catalysts receive the JEI catalyst role",
                jei.contains("for (IngredientSpec catalyst : recipe.catalysts())")
                        && jei.contains("input++, RecipeIngredientRole.CATALYST")
                        && jei.contains("input++, RecipeIngredientRole.INPUT"));
        check("the nonconsumed marker is explicitly red and localized",
                jei.contains("Component.translatable(\"jei.grindless.not_consumed\").withStyle(ChatFormatting.RED)"));
        var lang = JsonParser.parseString(Files.readString(Path.of(
                "common/src/main/resources/assets/grindless/lang/en_us.json"))).getAsJsonObject();
        check("the nonconsumed tooltip has its actual English translation",
                lang.get("jei.grindless.not_consumed").getAsString().equals("Not consumed"));
        System.out.println(assertions + " MACHINE UI ASSERTIONS; " + failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String description, boolean passed) {
        assertions++;
        if (!passed) {
            failures++;
            System.out.println("FAIL " + description);
        }
    }
}
