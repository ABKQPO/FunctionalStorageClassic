package com.hfstudio.functionalstorage.common.block;

/**
 * Number and arrangement of interactive regions on a drawer's front face.
 */
public enum DrawerFaceLayout {

    X_1(new float[][] { { 0.5F, 0.5F } }),
    X_2(new float[][] { { 0.5F, 0.25F }, { 0.5F, 0.75F } }),
    X_3(new float[][] { { 0.5F, 0.25F }, { 0.25F, 0.75F }, { 0.75F, 0.75F } }),
    X_4(new float[][] { { 0.25F, 0.25F }, { 0.75F, 0.25F }, { 0.25F, 0.75F }, { 0.75F, 0.75F } });

    private final float[][] centers;

    DrawerFaceLayout(float[][] centers) {
        this.centers = centers;
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
        if (horizontal < 0D || horizontal > 1D || vertical < 0D || vertical > 1D) {
            return -1;
        }
        return switch (this) {
            case X_1 -> 0;
            case X_2 -> vertical < 0.5D ? 0 : 1;
            case X_3 -> vertical < 0.5D ? 0 : (horizontal < 0.5D ? 1 : 2);
            case X_4 -> (vertical < 0.5D ? 0 : 2) + (horizontal < 0.5D ? 0 : 1);
        };
    }
}
