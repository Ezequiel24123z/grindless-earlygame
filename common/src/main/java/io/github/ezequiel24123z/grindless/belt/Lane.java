package io.github.ezequiel24123z.grindless.belt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * One lane of a belt tile: items and the positions they occupy.
 *
 * <p>The tile ticks this once. Items never become entities (ADR-0008).
 */
public final class Lane {

    private final List<LaneItem> items = new ArrayList<>();

    public List<LaneItem> items() {
        return Collections.unmodifiableList(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    /** Whether a new item can enter at position 0. */
    public boolean canAccept() {
        for (LaneItem item : items) {
            if (item.position() < BeltLogic.ITEM_LENGTH) {
                return false;
            }
        }
        return items.size() < BeltLogic.SLOTS_PER_LANE;
    }

    /**
     * Places {@code item} at the input end. Returns whether it fitted.
     */
    public boolean insert(LaneItem item) {
        if (!canAccept()) {
            return false;
        }
        item.setPosition(0.0);
        items.add(item);
        return true;
    }

    /** Moves every item toward the output without overlapping. */
    public void advance(double delta) {
        if (items.isEmpty() || delta <= 0.0) {
            return;
        }
        items.sort(Comparator.comparingDouble(LaneItem::position).reversed());
        double front = Double.POSITIVE_INFINITY;
        for (LaneItem item : items) {
            double next = BeltLogic.clamp(item.position(), delta, front);
            item.setPosition(next);
            front = item.position();
        }
    }

    /** The item sitting at the output end, or {@code null}. */
    public LaneItem peekFront() {
        LaneItem front = null;
        for (LaneItem item : items) {
            if (front == null || item.position() > front.position()) {
                front = item;
            }
        }
        return front;
    }

    /** Removes and returns the item ready to leave, or {@code null}. */
    public LaneItem takeReady() {
        LaneItem front = peekFront();
        if (front == null || !BeltLogic.readyToLeave(front.position())) {
            return null;
        }
        items.remove(front);
        return front;
    }

    /**
     * Removes the item closest to {@code target} and returns it, or {@code null} if the lane
     * is empty.
     */
    public LaneItem takeNearest(double target) {
        if (items.isEmpty()) {
            return null;
        }
        LaneItem best = items.get(0);
        double bestDistance = Math.abs(best.position() - target);
        for (int i = 1; i < items.size(); i++) {
            LaneItem item = items.get(i);
            double distance = Math.abs(item.position() - target);
            if (distance < bestDistance) {
                best = item;
                bestDistance = distance;
            }
        }
        items.remove(best);
        return best;
    }

    /**
     * Restores a saved item at its recorded position. Does not enforce spacing: a save is
     * already a legal lane.
     */
    public void restore(LaneItem item) {
        items.add(item);
    }

    public void clear() {
        items.clear();
    }
}
