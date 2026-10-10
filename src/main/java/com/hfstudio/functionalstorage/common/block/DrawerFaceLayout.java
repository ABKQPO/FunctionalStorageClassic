package com.hfstudio.functionalstorage.common.block;

import java.util.Objects;

public class DrawerFaceLayout {

    @FunctionalInterface
    public interface SlotResolver {

        int slotAt(double horizontal, double vertical);
    }

    public static final DrawerFaceLayout X_1 = new DrawerFaceLayout(
        new float[][] { { 0.5F, 0.5F } },
        (horizontal, vertical) -> 0);
    public static final DrawerFaceLayout X_2 = new DrawerFaceLayout(
        new float[][] { { 0.5F, 0.25F }, { 0.5F, 0.75F } },
        (horizontal, vertical) -> vertical < 0.5D ? 0 : 1);
    public static final DrawerFaceLayout X_3 = new DrawerFaceLayout(
        new float[][] { { 0.5F, 0.25F }, { 0.25F, 0.75F }, { 0.75F, 0.75F } },
        (horizontal, vertical) -> vertical < 0.5D ? 0 : (horizontal < 0.5D ? 1 : 2));
    public static final DrawerFaceLayout X_4 = new DrawerFaceLayout(
        new float[][] { { 0.25F, 0.25F }, { 0.75F, 0.25F }, { 0.25F, 0.75F }, { 0.75F, 0.75F } },
        (horizontal, vertical) -> (vertical < 0.5D ? 0 : 2) + (horizontal < 0.5D ? 0 : 1));

    private final float[][] centers;
    private final SlotResolver resolver;

    public DrawerFaceLayout(float[][] centers, SlotResolver resolver) {
        if (centers.length == 0) throw new IllegalArgumentException("A drawer face must contain a slot");
        this.centers = new float[centers.length][2];
        for (int slot = 0; slot < centers.length; slot++) {
            if (centers[slot].length != 2) throw new IllegalArgumentException("Slot centers require two coordinates");
            for (int axis = 0; axis < 2; axis++) {
                float value = centers[slot][axis];
                if (!Float.isFinite(value) || value < 0F || value > 1F) {
                    throw new IllegalArgumentException("Slot centers must be within the drawer face");
                }
                this.centers[slot][axis] = value;
            }
        }
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    public int getSlotCount() {
        return centers.length;
    }

    public float getSlotX(int slot) {
        return centers[slot][0];
    }

    public float getSlotY(int slot) {
        return centers[slot][1];
    }

    public int slotAt(double horizontal, double vertical) {
        if (!Double.isFinite(horizontal) || !Double.isFinite(vertical)
            || horizontal < 0D
            || horizontal > 1D
            || vertical < 0D
            || vertical > 1D) return -1;
        int slot = resolver.slotAt(horizontal, vertical);
        return slot < 0 || slot >= centers.length ? -1 : slot;
    }
}
