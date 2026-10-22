package io.github.ezequiel24123z.grindless.machine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional Assembler configuration and suspended base-work, not a second executor.
 * Active work remains in the machine's legacy Progress/WorkRecipe fields (ADR-0185).
 */
public final class AssemblerWork {
    public static final String TARGET = "SelectedRecipe";
    public static final String SUSPENDED = "SuspendedWork";
    public static final int MAX_ID_LENGTH = 256;
    private static final List<String> SAVED_KEYS = List.of(
            "Progress", "Recipe", "WorkRecipe", TARGET, SUSPENDED);

    private final Map<String, Double> suspended = new LinkedHashMap<>();
    private String selected = "";
    private CompoundTag invalidState;
    private String problem = "";

    public String selected() {
        return selected;
    }

    public Map<String, Double> suspended() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(suspended));
    }

    public boolean valid() {
        return invalidState == null;
    }

    public String problem() {
        return problem;
    }

    public void select(String id) {
        requireValid();
        requireId(id);
        selected = id;
    }

    /** Moves, never adds, an exact double balance. The empty owner retains unattributed legacy work. */
    public double activate(String previousId, double previousWork, String nextId) {
        requireValid();
        requireId(previousId);
        requireId(nextId);
        if (nextId.isEmpty() || !Double.isFinite(previousWork) || previousWork < 0.0
                || suspended.containsKey(previousId) && (!previousId.isEmpty() || previousWork > 0.0)) {
            throw new IllegalStateException("Invalid active Assembler work transition");
        }
        if (previousId.equals(nextId)) {
            return previousWork;
        }
        if (previousWork > 0.0) {
            suspended.put(previousId, previousWork);
        }
        Double restored = suspended.remove(nextId);
        return restored == null ? 0.0 : restored;
    }

    public static AssemblerWork load(CompoundTag tag, String activeId, double activeWork) {
        AssemblerWork work = new AssemblerWork();
        try {
            requireId(activeId);
            if (tag.contains("Progress") && !(tag.get("Progress") instanceof NumericTag)
                    || !Double.isFinite(activeWork) || activeWork < 0.0) {
                throw new IllegalArgumentException("Progress must be finite nonnegative base-work");
            }
            for (String key : List.of("Recipe", "WorkRecipe", TARGET)) {
                if (tag.contains(key) && !(tag.get(key) instanceof StringTag)) {
                    throw new IllegalArgumentException(key + " must be a stable recipe ID string");
                }
            }
            String target = tag.getString(TARGET);
            requireId(target);
            if (tag.contains(SUSPENDED)) {
                if (!(tag.get(SUSPENDED) instanceof ListTag rows)
                        || !rows.isEmpty() && rows.getElementType() != Tag.TAG_COMPOUND) {
                    throw new IllegalArgumentException("SuspendedWork must contain work compounds");
                }
                for (Tag row : rows) {
                    CompoundTag entry = (CompoundTag) row;
                    if (entry.getAllKeys().size() != 2 || !(entry.get("Recipe") instanceof StringTag)
                            || !(entry.get("Progress") instanceof NumericTag amount)) {
                        throw new IllegalArgumentException("Suspended work requires only Recipe and Progress");
                    }
                    String id = entry.getString("Recipe");
                    requireId(id);
                    double paid = amount.getAsDouble();
                    if (!Double.isFinite(paid) || paid <= 0.0
                            || id.equals(activeId) && (!id.isEmpty() || activeWork > 0.0)
                            || work.suspended.putIfAbsent(id, paid) != null) {
                        throw new IllegalArgumentException("Suspended work must be positive, unique and inactive");
                    }
                }
            }
            work.selected = target;
        } catch (IllegalArgumentException invalid) {
            work.problem = invalid.getMessage();
            work.invalidState = new CompoundTag();
            for (String key : SAVED_KEYS) {
                if (tag.contains(key)) {
                    work.invalidState.put(key, tag.get(key).copy());
                }
            }
            work.suspended.clear();
        }
        return work;
    }

    /** Malformed paid state is retained byte-for-value and disabled, not normalized into fresh work. */
    public void save(CompoundTag tag) {
        if (!valid()) {
            for (String key : SAVED_KEYS) {
                tag.remove(key);
                if (invalidState.contains(key)) {
                    tag.put(key, invalidState.get(key).copy());
                }
            }
            return;
        }
        tag.remove(TARGET);
        tag.remove(SUSPENDED);
        if (!selected.isEmpty()) {
            tag.putString(TARGET, selected);
        }
        if (!suspended.isEmpty()) {
            ListTag rows = new ListTag();
            suspended.forEach((id, paid) -> {
                CompoundTag row = new CompoundTag();
                row.putString("Recipe", id);
                row.putDouble("Progress", paid);
                rows.add(row);
            });
            tag.put(SUSPENDED, rows);
        }
    }

    private static void requireId(String id) {
        if (id == null || id.length() > MAX_ID_LENGTH || !id.isEmpty() && id.isBlank()) {
            throw new IllegalArgumentException("Invalid stable Assembler recipe ID");
        }
    }

    private void requireValid() {
        if (!valid()) {
            throw new IllegalStateException("Malformed Assembler work is retained but cannot execute: " + problem);
        }
    }
}
