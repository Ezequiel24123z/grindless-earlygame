package io.github.ezequiel24123z.grindless.client.inspection;

import io.github.ezequiel24123z.grindless.client.ResourceGuidanceClient;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionCodec;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.ItemRole;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.Resource;
import io.github.ezequiel24123z.grindless.inspection.MachineInspectionSnapshot.StoredItem;
import io.github.ezequiel24123z.grindless.sustainability.SustainabilityModel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Localized descriptive text; an overlay never advances or predicts a machine's execution. */
public final class MachineInspectionTooltip {

    public static final int MAX_LINES = 40;

    private MachineInspectionTooltip() {
    }

    public static List<Component> lines(MachineInspectionSnapshot snapshot, boolean expanded, boolean developer) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(snapshot.state().symbol() + " ")
                .append(Component.translatable(snapshot.state().translationKey())));
        if (!snapshot.reasonKey().isEmpty()) {
            lines.add(Component.translatable(snapshot.reasonKey()));
        }
        snapshot.process().ifPresent(process -> {
            if (process.id().equals("research/calibration")) {
                lines.add(Component.translatable("inspection.grindless.calibration"));
            } else if (process.id().equals("crude_extraction")) {
                lines.add(Component.translatable("inspection.grindless.extraction"));
            } else if (process.id().equals("fuel_burn")) {
                lines.add(Component.translatable("inspection.grindless.fuel_cycle"));
            } else if (!process.outputs().isEmpty()) {
                lines.add(Component.translatable("inspection.grindless.process",
                        resources(process.outputs(), expanded ? 4 : 2)));
            } else {
                lines.add(Component.translatable("inspection.grindless.cycle"));
            }
            process.progress().ifPresentOrElse(progress -> lines.add(Component.translatable(
                    "inspection.grindless.progress", progress.percent(), seconds(process.durationTicks() / 20.0))),
                    () -> lines.add(Component.translatable("inspection.grindless.progress_unknown")));
            if (process.fuPerTick() > 0) {
                lines.add(Component.translatable("inspection.grindless.draw", process.fuPerTick()));
            }
        });
        if (snapshot.process().isEmpty() && snapshot.state() == MachineInspectionSnapshot.State.WORKING) {
            lines.add(Component.translatable("inspection.grindless.progress_unknown"));
        }
        snapshot.energy().ifPresent(energy -> lines.add(Component.translatable(
                "inspection.grindless.energy", energy.stored(), energy.capacity())));
        if (snapshot.port().isPresent()) {
            lines.add(Component.translatable("inspection.grindless.shared_owner"));
        }
        for (var tank : snapshot.tanks()) {
            lines.add(tank.amount() == 0
                    ? Component.translatable(snapshot.port().isPresent()
                    ? "inspection.grindless.port_fluid_empty" : "inspection.grindless.tank_empty", tank.capacity())
                    : Component.translatable(snapshot.port().isPresent()
                    ? "inspection.grindless.port_fluid" : "inspection.grindless.tank",
                    ResourceGuidanceClient.name(new SustainabilityModel.Resource(SustainabilityModel.Kind.FLUID,
                            tank.id())), tank.amount(), tank.capacity()));
            tank.outputQuota().filter(quota -> quota > 0).ifPresent(quota -> lines.add(
                    Component.translatable("inspection.grindless.fluid_share", quota)));
            if (expanded && tank.amount() > 0) {
                lines.add(Component.translatable("inspection.grindless.fluid_conditions",
                        decimal(tank.temperatureC()), decimal(tank.pressureMPa())));
            }
        }
        for (ItemRole role : ItemRole.values()) {
            List<StoredItem> items = snapshot.items().stream().filter(item -> item.role() == role).toList();
            if (!items.isEmpty()) {
                lines.add(Component.translatable("inspection.grindless.stored." + lower(role),
                        stored(items, expanded ? MachineInspectionSnapshot.MAX_ITEMS : 3)));
            }
        }
        snapshot.manual().ifPresent(manual -> lines.add(Component.translatable(
                "inspection.grindless.manual." + lower(manual.kind()), manual.fuPerAction())));
        snapshot.structure().ifPresent(structure -> {
            lines.add(Component.translatable("inspection.grindless.structure",
                    Component.translatable("inspection.grindless.validity." + lower(structure.validity()))));
            if (!structure.reasonKey().isEmpty() && !structure.reasonKey().equals(snapshot.reasonKey())) {
                lines.add(Component.translatable(structure.reasonKey()));
            }
            structure.problem().ifPresent(position -> lines.add(Component.translatable(
                    "inspection.grindless.problem_position", position.x(), position.y(), position.z())));
            if (!structure.ports().isEmpty() || structure.validity() == MachineInspectionSnapshot.Validity.FORMED) {
                lines.add(Component.translatable(structure.validity() == MachineInspectionSnapshot.Validity.FORMED
                        ? "inspection.grindless.ports" : "inspection.grindless.ports_partial", structure.ports().size()));
                if (expanded) {
                    for (var port : structure.ports()) {
                        lines.add(Component.translatable("inspection.grindless.port",
                                label("port_role", port.role()), label("face", port.face())));
                        if (!port.accessFaces().isEmpty()) {
                            lines.add(access(port.accessFaces()));
                        }
                    }
                }
            }
        });
        snapshot.port().ifPresent(port -> {
            lines.add(Component.translatable("inspection.grindless.port",
                    label("port_role", port.role()), label("face", port.face())));
            lines.add(port.accessFaces().isEmpty() ? Component.translatable("inspection.grindless.access_unconfirmed")
                    : access(port.accessFaces()));
            lines.add(label("ownership", port.ownership()));
            port.owner().ifPresent(owner -> lines.add(Component.translatable("inspection.grindless.owner_position",
                    owner.x(), owner.y(), owner.z())));
        });
        if (expanded) {
            for (var requirement : snapshot.requirements()) {
                lines.add(label("site", requirement));
            }
            snapshot.process().ifPresent(process -> {
                appendResources(lines, "needs", process.inputs());
                appendResources(lines, "makes", process.outputs());
                appendResources(lines, "catalyst", process.catalysts());
                process.progress().ifPresent(progress -> lines.add(Component.translatable(
                        "inspection.grindless.remaining", seconds(progress.nominalSecondsRemaining()))));
                process.conditions().ifPresent(conditions -> {
                    lines.add(Component.translatable("inspection.grindless.conditions",
                            decimal(conditions.temperatureC()), decimal(conditions.pressureMPa()),
                            conditions.atmosphere(), conditions.agitation()));
                    conditions.requiredTemperatureC().ifPresent(temperature -> lines.add(
                            Component.translatable("inspection.grindless.required_temperature", decimal(temperature))));
                    conditions.requiredAtmosphere().ifPresent(atmosphere -> lines.add(
                            Component.translatable("inspection.grindless.required_atmosphere", atmosphere)));
                    conditions.requiredAgitation().ifPresent(agitation -> lines.add(
                            Component.translatable("inspection.grindless.required_agitation", agitation)));
                });
            });
        } else {
            lines.add(Component.translatable("inspection.grindless.more"));
        }
        if (snapshot.truncated()) {
            lines.add(Component.translatable("inspection.grindless.bounded"));
        }
        if (developer) {
            snapshot.process().ifPresent(process -> lines.add(
                    Component.translatable("inspection.grindless.debug.recipe", process.id())));
            lines.add(Component.translatable("inspection.grindless.debug.observation",
                    snapshot.target().dimension(), snapshot.target().gameTime(), MachineInspectionCodec.VERSION));
        }
        if (lines.size() > MAX_LINES) {
            List<Component> bounded = new ArrayList<>(lines.subList(0, MAX_LINES - 1));
            bounded.add(Component.translatable("inspection.grindless.bounded"));
            return List.copyOf(bounded);
        }
        return List.copyOf(lines);
    }

    private static void appendResources(List<Component> lines, String role, List<Resource> resources) {
        for (Resource resource : resources) {
            lines.add(Component.translatable("inspection.grindless." + role, resource(resource)));
        }
    }

    private static Component resources(List<Resource> resources, int maximum) {
        MutableComponent result = Component.empty();
        for (Resource resource : resources.stream().limit(maximum).toList()) {
            if (!result.getString().isEmpty()) {
                result.append(", ");
            }
            result.append(resource(resource));
        }
        appendMore(result, resources.size(), maximum);
        return result;
    }

    private static Component stored(List<StoredItem> items, int maximum) {
        MutableComponent result = Component.empty();
        for (StoredItem item : items.stream().limit(maximum).toList()) {
            if (!result.getString().isEmpty()) {
                result.append(", ");
            }
            Component name = ResourceGuidanceClient.name(new SustainabilityModel.Resource(
                    SustainabilityModel.Kind.ITEM, item.id()));
            result.append(Component.translatable("inspection.grindless.amount", item.count(),
                    rated(name, item.matrixRating())));
        }
        appendMore(result, items.size(), maximum);
        return result;
    }

    private static Component resource(Resource resource) {
        SustainabilityModel.Kind kind = switch (resource.kind()) {
            case ITEM -> SustainabilityModel.Kind.ITEM;
            case TAG -> SustainabilityModel.Kind.TAG;
            case FLUID -> SustainabilityModel.Kind.FLUID;
        };
        Component name = ResourceGuidanceClient.name(new SustainabilityModel.Resource(kind, resource.id()));
        Component result = Component.translatable(resource.kind() == MachineInspectionSnapshot.ResourceKind.FLUID
                ? "inspection.grindless.fluid_amount" : "inspection.grindless.amount",
                resource.count(), rated(name, resource.matrixRating()));
        return resource.vented() ? Component.translatable("inspection.grindless.vented", result) : result;
    }

    private static Component rated(Component name, int rating) {
        return rating < 0 ? Component.translatable("inspection.grindless.invalid_rating", name)
                : rating > 0 ? Component.translatable("inspection.grindless.rated", name, rating) : name;
    }

    private static void appendMore(MutableComponent result, int size, int maximum) {
        if (size > maximum) {
            result.append(Component.translatable("inspection.grindless.additional", size - maximum));
        }
    }

    private static Component label(String domain, Enum<?> value) {
        return Component.translatable("inspection.grindless." + domain + "." + lower(value));
    }

    private static Component access(List<MachineInspectionSnapshot.Face> faces) {
        MutableComponent joined = Component.empty();
        for (var face : faces) {
            if (!joined.getString().isEmpty()) {
                joined.append(", ");
            }
            joined.append(label("face", face));
        }
        return Component.translatable("inspection.grindless.access", joined);
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private static String seconds(double seconds) {
        return String.format(Locale.ROOT, "%.1f", seconds);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
