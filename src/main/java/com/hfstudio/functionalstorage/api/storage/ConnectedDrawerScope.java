package com.hfstudio.functionalstorage.api.storage;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;

public class ConnectedDrawerScope {

    private static final int X_BITS = 26;
    private static final int Y_BITS = 12;
    private static final int Z_BITS = 26;
    private static final int Y_MASK = (1 << Y_BITS) - 1;
    private static final int AXIS_MASK = (1 << X_BITS) - 1;

    private final Set<Long> positions;

    private ConnectedDrawerScope(@Nonnull Set<Long> positions) {
        this.positions = positions;
    }

    @Nonnull
    public static ConnectedDrawerScope of(@Nonnull Set<Long> positions) {
        return positions.isEmpty() ? empty() : new ConnectedDrawerScope(Set.copyOf(positions));
    }

    @Nonnull
    public static ConnectedDrawerScope empty() {
        return new ConnectedDrawerScope(Collections.emptySet());
    }

    public static long pack(int x, int y, int z) {
        return ((long) (x & AXIS_MASK) << (Y_BITS + Z_BITS)) | ((long) (y & Y_MASK) << Z_BITS) | (z & AXIS_MASK);
    }

    public static int unpackX(long packed) {
        return (int) (packed >> (Y_BITS + Z_BITS));
    }

    public static int unpackY(long packed) {
        return (int) (packed >> Z_BITS & Y_MASK);
    }

    public static int unpackZ(long packed) {
        return (int) (packed << (64 - Z_BITS) >> (64 - Z_BITS));
    }

    @Nonnull
    public Set<Long> getPositions() {
        return positions;
    }

    public boolean isEmpty() {
        return positions.isEmpty();
    }

    public int size() {
        return positions.size();
    }

    public boolean contains(int x, int y, int z) {
        return positions.contains(pack(x, y, z));
    }
}
