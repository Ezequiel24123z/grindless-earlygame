package io.github.ezequiel24123z.grindless.menu;

import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.fluid.FluidBuffer;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.fluid.FluidNbt;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.material.MaterialSnapshot;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

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
        for (int lines : List.of(0, 1, 4, 5, 20)) {
            int visible = MachineMenuLayout.visibleStatusLines(lines, 9);
            check(lines + " status lines fit their actual reserved region", visible == Math.min(lines, 4)
                    && MachineMenuLayout.STATUS_Y + visible * 9 <= MachineMenuLayout.INVENTORY_LABEL_Y - 4);
        }
        fluidViews();
        loadedFluidViews();
        recipeShapes();

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
        check("oversized status uses the tested bound, an indicator and the complete tooltip",
                screen.contains("MachineMenuLayout.visibleStatusLines(lines.size(), font.lineHeight)")
                        && screen.contains("index < visible") && screen.contains("gui.grindless.status_more")
                        && screen.contains("graphics.renderTooltip(font, statusLine(), mouseX, mouseY)"));
        check("only actual recipe catalysts receive the JEI catalyst role",
                jei.contains("inputs.addAll(recipe.catalysts())")
                        && jei.contains("index < inputs.size() && index >= recipe.itemInputs().size()")
                        && jei.contains("? RecipeIngredientRole.CATALYST : RecipeIngredientRole.INPUT"));
        check("the nonconsumed marker is explicitly red and localized",
                jei.contains("Component.translatable(\"jei.grindless.not_consumed\").withStyle(ChatFormatting.RED)"));
        var lang = JsonParser.parseString(Files.readString(Path.of(
                "common/src/main/resources/assets/grindless/lang/en_us.json"))).getAsJsonObject();
        check("the nonconsumed tooltip has its actual English translation",
                lang.get("jei.grindless.not_consumed").getAsString().equals("Not consumed"));
        for (String key : List.of("gui.grindless.fluid_amount", "gui.grindless.fluid_output_quota",
                "gui.grindless.fluid_unavailable", "gui.grindless.fluid_empty", "gui.grindless.fluid_unknown",
                "gui.grindless.fluid_unknown_details", "gui.grindless.status_more", "jei.grindless.inputs",
                "jei.grindless.outputs", "jei.grindless.amount", "jei.grindless.adjacent_fluid",
                "jei.grindless.shared_tank", "jei.grindless.state_fluid_input",
                "jei.grindless.state_fluid_output", "jei.grindless.fluid_unavailable",
                "jei.grindless.settling_structure")) {
            check(key + " has a committed translation", lang.has(key));
        }
        check("JEI paints the complete physical schema separately from populated ingredients",
                jei.contains("MachineMenuLayout.itemSlots(menuKind)")
                        && jei.contains("slotBackground.draw(graphics, slot.x() - 1, slot.y() - 1)"));
        check("JEI uses actual Forge fluid ingredients and capacity-aware renderers",
                jei.contains(".addIngredient(ForgeTypes.FLUID_STACK, stack)")
                        && jei.contains("slot.setFluidRenderer(capacity, showCapacity, 16, FLUID_HEIGHT)"));
        check("JEI does not freeze a possibly empty pre-tag graph in its category constructor",
                !jei.contains("private final List<ProcessRecipe> recipes")
                        && jei.contains("return ProcessLookup.recipes().stream()"));
        check("JEI progress respects the same proven physical slot positions",
                jei.contains("arrow.draw(graphics, MachineMenuLayout.progressX(menuKind), 35)"));
        check("unregistered state fluids remain explicit rather than being fabricated as water",
                jei.contains("jei.grindless.state_fluid_input")
                        && jei.contains("jei.grindless.state_fluid_output"));
        check("nonrenderable cells expose a localized binding diagnosis without a fake ingredient",
                jei.contains("public void getTooltip(ITooltipBuilder tooltip")
                        && jei.contains("addFluidDiagnostic(tooltip, spec.id(), spec.count()")
                        && jei.contains("jei.grindless.fluid_unavailable\").withStyle(ChatFormatting.RED)"));
        check("all recipe conditions are wrapped, included in height and never silently truncated",
                jei.contains("for (Component detail : details(recipe))")
                        && jei.contains("font.split(detail, WIDTH - 8)")
                        && !jei.contains("subList(")
                        && jei.contains("recipe.namesTemperature()")
                        && jei.contains("recipe.namesAtmosphere()")
                        && jei.contains("recipe.namesAgitation()"));
        check("source and formation conditions are not mislabeled as catalysts",
                jei.contains("SourceSite.Fault.NEEDS_ROCK_BED")
                        && jei.contains("SourceSite.Fault.NEEDS_OPEN_SKY")
                        && jei.contains("SourceSite.Fault.NEEDS_DAYLIGHT")
                        && jei.contains("WorkshopStructure.Fault.MISSING_BRICKS")
                        && jei.contains("jei.grindless.settling_structure"));
        check("adjacent fluid and settling's shared store are named honestly",
                jei.contains("jei.grindless.adjacent_fluid") && jei.contains("jei.grindless.shared_tank"));
        check("the primary view uses synchronized identity, not a recipe or client block-entity inference",
                menu.contains("MachineFluidView.fromMenuData(data.get(DATA_FLUID_IS_WATER), fluidAmount(),")
                        && menu.contains("FluidLogic.MACHINE_CAPACITY, fluidOutputAmount()")
                        && !menu.contains("FluidLogic.WATER"));
        String process = Files.readString(COMMON.resolve("machine/ProcessMachineBlockEntity.java"));
        check("server synchronization encodes the actual primary-store identity alongside its amount",
                process.contains("FluidState storedFluid = fluid.state();")
                        && process.contains("DATA_FLUID, storedFluid.millibuckets()")
                        && process.contains("DATA_FLUID_IS_WATER, MachineFluidView.waterFlag(storedFluid)"));
        check("client load and pre-level initialization cannot confirm water from a local block entity",
                process.replaceAll("\\s+", " ").contains(
                        "if (getLevel() != null && !getLevel().isClientSide()) { "
                                + "data.set(ProcessMachineMenu.DATA_FLUID_IS_WATER, "
                                + "MachineFluidView.waterFlag(storedFluid)); }"));
        check("unknown nonempty contents remain visible with a diagnosis and no fabricated texture",
                screen.contains("fluid.id().map(id -> FluidClientPlatform.draw(")
                        && screen.contains("fill)).orElse(false)")
                        && screen.contains("fluid.amount() > 0 && fluid.id().isEmpty()")
                        && screen.contains("gui.grindless.fluid_unknown_details")
                        && screen.contains("graphics.drawString(font, \"?\"")
                        && screen.contains("fluid.amount(), fluid.capacity()"));
        check("the new identity field appends without shifting the existing menu protocol",
                List.of(ProcessMachineMenu.DATA_ENERGY, ProcessMachineMenu.DATA_CAPACITY,
                        ProcessMachineMenu.DATA_PROGRESS, ProcessMachineMenu.DATA_CYCLE,
                        ProcessMachineMenu.DATA_STATUS, ProcessMachineMenu.DATA_KIND,
                        ProcessMachineMenu.DATA_STRUCTURE, ProcessMachineMenu.DATA_FLUID,
                        ProcessMachineMenu.DATA_FLUID_OUTPUT, ProcessMachineMenu.DATA_SITE)
                        .equals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9))
                        && ProcessMachineMenu.DATA_FLUID_IS_WATER == 10 && ProcessMachineMenu.DATA_SIZE == 11);
        check("both hosts and the empty-data fallback allocate the current shared payload size",
                process.contains("new SimpleContainerData(ProcessMachineMenu.DATA_SIZE)")
                        && Files.readString(COMMON.resolve("machine/ThermalGeneratorBlockEntity.java"))
                        .contains("new SimpleContainerData(ProcessMachineMenu.DATA_SIZE)")
                        && menu.contains("new SimpleContainerData(DATA_SIZE)"));
        check("common keeps Forge fluid classes behind the platform bridge",
                !Files.readString(COMMON.resolve("client/FluidClientPlatform.java")).contains("net.minecraftforge")
                        && !screen.contains("net.minecraftforge"));
        System.out.println(assertions + " MACHINE UI ASSERTIONS; " + failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void fluidViews() {
        for (int amount : List.of(0, 1, 250, 1000, 1875, 2000)) {
            MachineFluidView view = MachineFluidView.fromMenuData(1, amount, 2000, Math.min(amount, 750));
            int expected = amount == 0 ? 0 : (int) Math.ceil(amount * 34.0 / 2000);
            check(amount + " mB has a bounded, visible proportional fill", view.fillHeight(34) == expected);
            check(amount + " mB does not add the export quota to stored water",
                    view.fillHeight(34) == MachineFluidView.fromMenuData(1, amount, 2000, 0).fillHeight(34));
        }
        for (MachineMenuKind kind : MachineMenuKind.values()) {
            MachineMenuLayout.waterTank(kind).ifPresent(tank -> {
                check(kind + " water well fits above the status and beside energy",
                        tank.x() > 20 && tank.x() + tank.width() < MachineMenuLayout.WIDTH - 8
                                && tank.y() >= 26 && tank.y() + tank.height() <= MachineMenuLayout.STATUS_Y);
                for (var slot : MachineMenuLayout.itemSlots(kind)) {
                    check(kind + " water well does not cover physical slot " + slot.index(),
                            tank.x() + tank.width() <= slot.x() - 1 || tank.x() >= slot.x() + 17
                                    || tank.y() + tank.height() <= slot.y() - 1 || tank.y() >= slot.y() + 17);
                }
            });
        }
    }

    private static void loadedFluidViews() {
        List<FluidState> contents = List.of(FluidState.of(FluidLogic.WATER, 250),
                FluidState.of("minecraft:lava", 250, 1200, 0.25),
                FluidState.of("grindless:unbound_fluid", 250, 35, 0.2), FluidState.EMPTY);
        for (MachineMenuKind kind : MachineMenuKind.values()) {
            if (MachineMenuLayout.waterTank(kind).isEmpty()) {
                continue;
            }
            for (FluidState saved : contents) {
                var tag = FluidNbt.save(saved);
                var originalTag = tag.copy();
                FluidBuffer store = FluidBuffer.ambient(FluidLogic.MACHINE_CAPACITY);
                store.set(FluidNbt.load(tag));
                FluidState loaded = store.state();
                String context = kind + " loaded " + saved.id();
                check(context + " preserves the real saved state", loaded.equals(saved));
                SimpleContainerData data = new SimpleContainerData(ProcessMachineMenu.DATA_SIZE);
                data.set(ProcessMachineMenu.DATA_KIND, kind.ordinal());
                data.set(ProcessMachineMenu.DATA_FLUID, loaded.millibuckets());
                data.set(ProcessMachineMenu.DATA_FLUID_OUTPUT, loaded.isEmpty() ? 0 : 100);
                MachineFluidView pending = view(data);
                check(context + " never assumes water before its identity arrives",
                        pending.id().isEmpty() && pending.amount() == loaded.millibuckets()
                                && pending.capacity() == store.capacity()
                                && pending.outputQuota() == data.get(ProcessMachineMenu.DATA_FLUID_OUTPUT));
                data.set(ProcessMachineMenu.DATA_FLUID_IS_WATER, MachineFluidView.waterFlag(loaded));
                MachineFluidView confirmed = view(data);
                Optional<String> expected = loaded.is(FluidLogic.WATER)
                        ? Optional.of(FluidLogic.WATER) : Optional.empty();
                check(context + " identifies only confirmed water", confirmed.id().equals(expected));
                check(context + " retains amount, native capacity and included output quota",
                        confirmed.amount() == loaded.millibuckets() && confirmed.capacity() == store.capacity()
                                && confirmed.outputQuota() == data.get(ProcessMachineMenu.DATA_FLUID_OUTPUT));
                check(context + " keeps nonempty unknown contents distinct from an empty tank",
                        loaded.isEmpty() ? confirmed.fillHeight(34) == 0
                                : confirmed.fillHeight(34) > 0 && confirmed.amount() == 250);
                check(context + " leaves storage and saved data unchanged",
                        store.state().equals(saved) && tag.equals(originalTag)
                                && FluidNbt.save(store.state()).equals(originalTag));
            }
        }
        for (int flag : List.of(-1, 0, 2, 42, 32767)) {
            MachineFluidView unknown = MachineFluidView.fromMenuData(flag, 250, 2000, 100);
            check("unconfirmed identity flag " + flag + " keeps an explicit nonempty unknown",
                    unknown.id().isEmpty() && unknown.amount() == 250 && unknown.capacity() == 2000
                            && unknown.outputQuota() == 100 && unknown.fillHeight(34) > 0);
        }
        check("an empty amount cannot display a stale water identity",
                MachineFluidView.fromMenuData(1, 0, 2000, 0).id().isEmpty());
    }

    private static MachineFluidView view(ContainerData data) {
        return MachineFluidView.fromMenuData(data.get(ProcessMachineMenu.DATA_FLUID_IS_WATER),
                data.get(ProcessMachineMenu.DATA_FLUID), FluidLogic.MACHINE_CAPACITY,
                data.get(ProcessMachineMenu.DATA_FLUID_OUTPUT));
    }

    private static void recipeShapes() throws IOException {
        ProcessLookup.rebuild(MaterialSnapshot.EMPTY);
        for (MachineFamily family : MachineFamily.values()) {
            MachineMenuKind kind = MachineMenuKind.valueOf(family.name());
            check(family + " has a real physical menu schema", MachineMenuLayout.itemSlots(kind).size() == kind.size());
        }
        for (var recipe : ProcessLookup.recipes()) {
            MachineMenuKind kind = MachineMenuKind.valueOf(recipe.family().name());
            check(recipe.id() + " fits the actual item inputs including catalysts",
                    recipe.itemInputs().size() + recipe.catalysts().size() <= kind.inputs());
            check(recipe.id() + " fits every physical item output", recipe.itemOutputs().size() <= kind.outputs());
            check(recipe.id() + " uses at most one primary and one adjacent supply",
                    recipe.fluidInputs().size() <= 2);
            check(recipe.id() + " has at most one nonvented primary-store output",
                    recipe.fluidOutputs().stream().filter(output -> !output.vented()).count() <= 1);
            if (MachineMenuLayout.waterTank(kind).isPresent()) {
                check(recipe.id() + " genuinely consumes only primary water in the Bootstrap view",
                        recipe.fluidInputs().size() == 1
                                && recipe.fluidInputs().get(0).id().equals(FluidLogic.WATER)
                                && recipe.fluidInputs().get(0).count() <= FluidLogic.MACHINE_CAPACITY);
                check(recipe.id() + " returns only water to that same store",
                        recipe.fluidOutputs().stream().allMatch(output -> output.id().equals(FluidLogic.WATER)
                                && !output.vented() && output.count() <= FluidLogic.MACHINE_CAPACITY));
            }
        }
        check("the projected capacity is the actual native process buffer capacity",
                Files.readString(COMMON.resolve("machine/ProcessMachineBlockEntity.java"))
                        .contains("new FluidBuffer(FluidLogic.MACHINE_CAPACITY,"));
    }

    private static void check(String description, boolean passed) {
        assertions++;
        if (!passed) {
            failures++;
            System.out.println("FAIL " + description);
        }
    }
}
