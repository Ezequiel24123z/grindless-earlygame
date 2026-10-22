package io.github.ezequiel24123z.grindless.menu;

import com.google.gson.JsonParser;
import io.github.ezequiel24123z.grindless.fluid.FluidBuffer;
import io.github.ezequiel24123z.grindless.fluid.FluidLogic;
import io.github.ezequiel24123z.grindless.fluid.FluidNbt;
import io.github.ezequiel24123z.grindless.fluid.FluidState;
import io.github.ezequiel24123z.grindless.material.MaterialSnapshot;
import io.github.ezequiel24123z.grindless.recipe.MachineFamily;
import io.github.ezequiel24123z.grindless.recipe.ProcessLookup;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.function.Consumer;

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
        assemblerGeometry();
        assemblerWire();

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
                        && !screen.substring(screen.indexOf("var lines = font.split(statusLine()"),
                                screen.indexOf("menu.primaryFluidView().ifPresent(fluid -> graphics.drawString"))
                        .contains("plainSubstrByWidth"));
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
                        && jei.contains("recipe.namesPressure()")
                        && jei.contains("recipe.namesAtmosphere()")
                        && jei.contains("recipe.namesAgitation()"));
        check("JEI exposes exact pressure and optimal width without integer or two-decimal rounding",
                jei.contains("Component.translatable(\"jei.grindless.pressure\", Double.toString(recipe.pressureMPa()),")
                        && jei.contains("Double.toString(recipe.pressureHalfWidthMPa())"));
        check("JEI labels absolute pressure, exact units and the optimal rather than hard-cutoff band",
                lang.get("jei.grindless.pressure").getAsString()
                        .equals("Pressure: %s MPa absolute (optimal +/- %s MPa)"));
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
        assemblerAuthority(menu, screen, process, lang);
        System.out.println(assertions + " MACHINE UI ASSERTIONS; " + failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void assemblerGeometry() {
        for (MachineMenuKind kind : MachineMenuKind.values()) {
            check(kind + " keeps the shared 176x204 machine body", MachineMenuLayout.WIDTH == 176
                    && MachineMenuLayout.HEIGHT == 204 && MachineMenuLayout.INVENTORY_Y == 122
                    && MachineMenuLayout.HOTBAR_Y == 180);
            check(kind + " extends only the Assembler's outer width",
                    MachineMenuLayout.screenWidth(kind) == (kind == MachineMenuKind.ASSEMBLER ? 312 : 176));
        }
        for (int width : List.of(320, 426, 640, 854)) {
            int left = (width - MachineMenuLayout.screenWidth(MachineMenuKind.ASSEMBLER)) / 2;
            check("Assembler side panel fits the native minimum and larger GUI widths " + width,
                    left >= 4 && left + MachineMenuLayout.screenWidth(MachineMenuKind.ASSEMBLER) <= width - 4);
        }
        check("the picker is separated from every original slot and the energy/status/inventory body",
                MachineMenuLayout.ASSEMBLER_PICKER_X > MachineMenuLayout.WIDTH);
        check("search, AUTO, five native rows, paging and two footer lines remain separate",
                20 + 18 < 42 && 42 + 20 < MachineMenuLayout.ASSEMBLER_PICKER_ROW_Y
                        && MachineMenuLayout.ASSEMBLER_PICKER_ROW_Y
                        + MachineMenuLayout.ASSEMBLER_PICKER_ROWS * MachineMenuLayout.ASSEMBLER_PICKER_ROW_HEIGHT <= 158
                        && 158 + 20 < 182 && 182 + 9 <= 192 && 192 + 9 < MachineMenuLayout.HEIGHT - 1);
        for (int row = 0; row < MachineMenuLayout.ASSEMBLER_PICKER_ROWS; row++) {
            int y = MachineMenuLayout.ASSEMBLER_PICKER_ROW_Y + row * MachineMenuLayout.ASSEMBLER_PICKER_ROW_HEIGHT;
            check("picker icon " + row + " fits beside its native button without touching paging",
                    y + 1 + 16 <= 158 && 7 + 16 < 26 && 26 + 100 < MachineMenuLayout.ASSEMBLER_PICKER_WIDTH);
        }
        long token = 0xFEDCBA9876543210L;
        long decoded = 0L;
        for (int part = 0; part < 4; part++) {
            int signedShort = (short) (token >>> (part * 16));
            decoded |= (signedShort & 0xFFFFL) << (part * 16);
        }
        check("four signed vanilla data shorts reconstruct the exact opening identity", decoded == token);
    }

    private static void assemblerWire() {
        AssemblerSelectionNetwork.Request request = new AssemblerSelectionNetwork.Request(
                19, 0xFEDCBA9876543210L, 7, "assemble/chemical_washer");
        FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
        try {
            AssemblerSelectionNetwork.writeRequest(data, request);
            check("the actual request codec retains the exact ID, opening and revision",
                    AssemblerSelectionNetwork.readRequest(data).equals(request) && !data.isReadable());
        } finally {
            data.release();
        }
        Map<String, Double> history = new java.util.LinkedHashMap<>();
        for (int index = 0; index < 4100; index++) {
            history.put("removed/history/" + index, 0.125);
        }
        var many = new MachineMenuHost.RecipeSelection("", "assemble/chemical_washer", 0.25, history,
                MachineMenuHost.SelectionIssue.AUTO);
        data = new FriendlyByteBuf(Unpooled.buffer());
        try {
            AssemblerSelectionNetwork.writeUpdate(data, new AssemblerSelectionNetwork.Update(19, 5L, 9, many,
                    Optional.empty(), false));
            check("payload-bounded decoding does not strand valid work behind an arbitrary job-count cap",
                    AssemblerSelectionNetwork.readUpdate(data).state().equals(many));
        } finally {
            data.release();
        }
        for (String id : List.of("", "removed/unknown", "press/plate/iron", "x".repeat(256))) {
            data = new FriendlyByteBuf(Unpooled.buffer());
            try {
                var value = new AssemblerSelectionNetwork.Request(19, 5L, 8, id);
                AssemblerSelectionNetwork.writeRequest(data, value);
                check("wire parsing never converts or resolves a client ID: " + id.length(),
                        AssemblerSelectionNetwork.readRequest(data).equals(value));
            } finally {
                data.release();
            }
        }
        rejectRequest("truncated requests refuse before mutation", buffer -> buffer.writeInt(19));
        rejectRequest("trailing request data is not ignored", buffer -> {
            AssemblerSelectionNetwork.writeRequest(buffer, request);
            buffer.writeByte(1);
        });
        rejectRequest("overlong client IDs cannot bypass the explicit ID limit", buffer -> {
            buffer.writeInt(19).writeLong(5L).writeInt(8).writeUtf("x".repeat(257));
        });

        var state = new MachineMenuHost.RecipeSelection("removed/selected", "assemble/chemical_washer", 160.34375,
                Map.of("assemble/chemical_reactor", 0.15625, "removed/paid", 5.375),
                MachineMenuHost.SelectionIssue.UNAVAILABLE);
        var update = new AssemblerSelectionNetwork.Update(19, request.token(), 9, state,
                Optional.of(List.of(new ProcessMachineMenu.RecipeChoice("assemble/chemical_washer", ItemStack.EMPTY),
                        new ProcessMachineMenu.RecipeChoice("assemble/chemical_reactor", ItemStack.EMPTY))), true);
        data = new FriendlyByteBuf(Unpooled.buffer());
        try {
            AssemblerSelectionNetwork.writeUpdate(data, update);
            var restored = AssemblerSelectionNetwork.readUpdate(data);
            check("authoritative synchronization retains unknown configuration and exact partial/fractional work",
                    restored.container() == update.container() && restored.token() == update.token()
                            && restored.revision() == 9 && restored.state().equals(state) && restored.acknowledgement());
            check("the synchronized catalogue carries full stable IDs rather than local graph ordinals",
                    restored.catalogue().orElseThrow().stream().map(ProcessMachineMenu.RecipeChoice::id).toList()
                            .equals(List.of("assemble/chemical_washer", "assemble/chemical_reactor")));
            check("an unresolved output icon remains empty rather than a fabricated item",
                    restored.catalogue().orElseThrow().stream().allMatch(choice -> choice.output().isEmpty()));
        } finally {
            data.release();
        }
        data = new FriendlyByteBuf(Unpooled.buffer());
        try {
            AssemblerSelectionNetwork.writeUpdate(data, new AssemblerSelectionNetwork.Update(19, 5L, 9, state,
                    Optional.empty(), false));
            var restored = AssemblerSelectionNetwork.readUpdate(data);
            check("ordinary work updates need not retransmit the catalogue or acknowledge another request",
                    restored.catalogue().isEmpty() && !restored.acknowledgement() && restored.state().equals(state));
        } finally {
            data.release();
        }
        data = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var invalid = new MachineMenuHost.RecipeSelection("", "", Double.NaN, Map.of(),
                    MachineMenuHost.SelectionIssue.AUTO);
            AssemblerSelectionNetwork.writeUpdate(data, new AssemblerSelectionNetwork.Update(19, 5L, 9, invalid,
                    Optional.empty(), false));
            try {
                AssemblerSelectionNetwork.readUpdate(data);
                check("NaN synchronized work must refuse", false);
            } catch (io.netty.handler.codec.DecoderException expected) {
                check("NaN synchronized work refuses explicitly", true);
            }
        } finally {
            data.release();
        }
    }

    private static void rejectRequest(String label, Consumer<FriendlyByteBuf> write) {
        FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
        try {
            write.accept(data);
            try {
                AssemblerSelectionNetwork.readRequest(data);
                check(label, false);
            } catch (io.netty.handler.codec.DecoderException | IndexOutOfBoundsException expected) {
                check(label, true);
            }
        } finally {
            data.release();
        }
    }

    private static void assemblerAuthority(String menu, String screen, String process,
                                            com.google.gson.JsonObject lang) throws IOException {
        String network = Files.readString(COMMON.resolve("menu/AssemblerSelectionNetwork.java"));
        String work = Files.readString(COMMON.resolve("machine/AssemblerWork.java"));
        check("the server checks owner, current menu, container and exact opening/revision before selection",
                menu.contains("!serverPlayer.server.isSameThread()")
                        && menu.contains("player != owner || player.containerMenu != this || container != containerId")
                        && menu.contains("token != openingToken || revision != selectionRevision")
                        && menu.contains("ProcessLookup.byId(MachineFamily.ASSEMBLER, recipeId).isEmpty()"));
        check("the actual host checks its current live native BE, distance, family and build permissions",
                process.contains("kind.family() != MachineFamily.ASSEMBLER")
                        && process.contains("menu.host() != this")
                        && process.contains("getLevel().getBlockEntity(getBlockPos()) != this")
                        && process.contains("!getLevel().hasChunkAt(getBlockPos())")
                        && process.contains("!stillValid(player)") && process.contains("player.isSpectator()")
                        && process.contains("!player.mayBuild()") && process.contains("!getLevel().mayInteract(player, getBlockPos())")
                        && process.contains("!player.mayInteract(getLevel(), getBlockPos())")
                        && process.contains("!player.mayUseItemAt(getBlockPos(), Direction.UP, ItemStack.EMPTY)"));
        check("load/work/settlement reentrancy refuses without clearing the selected target",
                process.contains("if (loadingInventory || processing || settling)")
                        && process.indexOf("return SelectionResult.BUSY;")
                        < process.indexOf("assemblerWork.select(id);")
                        && process.contains("beforeWorkRecipeId") && process.contains("progress = beforeProgress;"));
        check("explicit resolution has no AUTO fallback and revalidates live fluid supplies even for a cache hit",
                process.contains("return completed.isPresent() ? completed")
                        && process.contains("ProcessLookup.findSelected(kind.family(), assemblerWork.selected(), inputs()")
                        && process.contains("progress < cached.durationTicks() && !ProcessLookup.matchesFluid("));
        check("a completion revalidates current graph identity, exact work owner and conditions inside rollback",
                process.contains("!currentAssemblerWork(recipe)")
                        && process.contains("filter(current -> current == recipe)")
                        && process.contains("recipe.id().equals(workRecipeId) && progress >= recipe.durationTicks()")
                        && process.contains("ProcessLookup.conditionsOf(recipe).evaluate(conditions()).canRun()"));
        check("authoritative client IDs arrive through an opening-bound snapshot, not the client's ProcessLookup",
                menu.contains("openingParts == 15")
                        && menu.contains("clientSelection.token() == synchronizedOpeningToken()")
                        && menu.contains("revision < clientSelection.revision()")
                        && menu.contains("ProcessLookup.recipes(MachineFamily.ASSEMBLER)")
                        && !screen.contains("ProcessLookup"));
        check("a click carries the actually displayed revision, not a newer unobserved list",
                screen.contains("menu.requestSelection(shownSelection, id)")
                        && menu.contains("displayed.token(), displayed.revision(), recipeId"));
        check("all rejected wire/menu requests have localized visible refusal and an authoritative acknowledgement",
                network.contains("player.displayClientMessage(Component.translatable(result.translationKey()), false)")
                        && network.contains("menu.recordSelectionResult(result)")
                        && screen.contains("Component.translatable(result.translationKey()).withStyle(ChatFormatting.RED)")
                        && network.contains("menu.sendSelection(true)")
                        && network.contains("SelectionResult.MALFORMED") && network.contains("SelectionResult.CLOSED"));
        check("the server never loads client receiver registration",
                Files.readString(COMMON.resolve("registry/ModMenus.java")).contains("AssemblerSelectionNetwork.register();")
                        && Files.readString(COMMON.resolve("client/GrindlessClient.java"))
                        .contains("AssemblerSelectionNetwork.registerClient();")
                        && network.contains("NetworkManager.Side.C2S") && network.contains("NetworkManager.Side.S2C")
                        && !network.contains("net.minecraft.client"));
        check("the picker is a searchable native paged list with output icons, keyboard and focus",
                screen.contains("new EditBox(font") && screen.contains("recipeSearch.setResponder")
                        && screen.contains("entry.name().getString().toLowerCase(Locale.ROOT).contains(query)")
                        && screen.contains("entry.id().toLowerCase(Locale.ROOT).contains(query)")
                        && screen.contains("GLFW.GLFW_KEY_PAGE_UP") && screen.contains("recipeSearch.charTyped")
                        && screen.contains("recipeButtons.get(0).setFocused(true)")
                        && screen.contains("graphics.renderItem(output, x, y)")
                        && screen.contains("Button.builder") && screen.contains("hasClickedOutside"));
        check("pending/empty/unavailable/malformed/current-work states are not optimistic client selection",
                screen.contains("menu.selectionPending()") && screen.contains("shownSelection == null")
                        && screen.contains("gui.grindless.assembler.no_results")
                        && screen.contains("MachineMenuHost.SelectionIssue.INVALID_WORK")
                        && screen.contains("state.suspended().forEach") && screen.contains("gui.grindless.assembler.active_work")
                        && !screen.contains("assemblerWork.select"));
        check("saved work contains only stable IDs and doubles, never live recipes or catalogue ordinals",
                work.contains("tag.putString(TARGET, selected)") && work.contains("row.putString(\"Recipe\", id)")
                        && work.contains("row.putDouble(\"Progress\", paid)") && !work.contains(".ordinal()")
                        && !work.contains("ProcessRecipe") && work.contains("invalidState.get(key).copy()"));
        for (MachineMenuHost.SelectionIssue issue : MachineMenuHost.SelectionIssue.values()) {
            check(issue + " has an actual localized selected-target diagnosis", lang.has(issue.translationKey()));
        }
        for (MachineMenuHost.SelectionResult result : MachineMenuHost.SelectionResult.values()) {
            check(result + " has an actual localized request result", lang.has(result.translationKey()));
        }
        for (String key : List.of("title", "search", "auto", "auto_selected", "auto_help", "previous", "next",
                "waiting", "loading", "no_results", "unavailable", "unattributed", "target", "work", "active_work",
                "held_work", "work_help", "invalid_state", "retained_work", "no_active_recipe")) {
            check("the Assembler picker localizes " + key, lang.has("gui.grindless.assembler." + key));
        }
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
