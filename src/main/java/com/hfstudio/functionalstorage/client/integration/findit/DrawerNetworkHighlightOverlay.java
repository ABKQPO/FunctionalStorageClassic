package com.hfstudio.functionalstorage.client.integration.findit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import com.gtnh.findit.FindItConfig;
import com.gtnh.findit.fx.BlockHighlighter;
import com.gtnh.findit.fx.ParticlePosition;
import com.hfstudio.functionalstorage.api.storage.ConnectedDrawerScope;
import com.hfstudio.functionalstorage.common.tile.base.ControllableDrawerTile;
import com.hfstudio.functionalstorage.common.tile.controller.ConnectedDrawerLookup;
import com.hfstudio.functionalstorage.common.tile.controller.DrawerControllerTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@Optional.Interface(iface = "com.gtnh.findit.fx.BlockHighlighter", modid = "findit", striprefs = true)
public class DrawerNetworkHighlightOverlay {

    private static final int REFRESH_INTERVAL_TICKS = 10;

    private static DrawerNetworkHighlightOverlay instance;

    private final BlockHighlighter highlighter = new BlockHighlighter();
    private final List<ChunkPosition> visible = new ArrayList<>();

    private long expiresAtMillis;
    private long nextRefreshTick = Long.MIN_VALUE;

    public static void register() {
        if (instance != null) {
            return;
        }
        instance = new DrawerNetworkHighlightOverlay();
        MinecraftForge.EVENT_BUS.register(instance);
    }

    public static DrawerNetworkHighlightOverlay active() {
        return instance;
    }

    @Optional.Method(modid = "findit")
    public void extend(World world, ChunkPosition position) {
        if (!FunctionalStorageConfig.COMPATIBILITY.highlightLinkedDrawersOnFind) {
            return;
        }
        if (!(world.getTileEntity(
            position.chunkPosX,
            position.chunkPosY,
            position.chunkPosZ) instanceof DrawerControllerTile)) {
            return;
        }
        long now = System.currentTimeMillis();
        long remaining = expiresAtMillis - now;
        if (remaining <= 0L) {
            // A fresh result starts the clock; a second controller found by the
            // same search extends the set without restarting it.
            expiresAtMillis = now + FindItConfig.BLOCK_HIGHLIGHTING_DURATION * 1000L;
            visible.clear();
        }
        List<ChunkPosition> positions = new ArrayList<>();
        positions.add(position);
        appendLinkedDrawers(world, position, positions);
        for (ChunkPosition candidate : positions) {
            if (!visible.contains(candidate)) {
                visible.add(candidate);
            }
        }
        nextRefreshTick = Long.MIN_VALUE;
    }

    @Optional.Method(modid = "findit")
    public void render(RenderWorldLastEvent event) {
        if (visible.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null || minecraft.thePlayer == null) {
            return;
        }
        if (System.currentTimeMillis() > expiresAtMillis) {
            clear();
            return;
        }
        long tick = minecraft.theWorld.getTotalWorldTime();
        if (tick >= nextRefreshTick) {
            nextRefreshTick = tick + REFRESH_INTERVAL_TICKS;
            refresh(minecraft.theWorld);
        }
        // FindIt's own listener renders an instance it owns and re-arms it only
        // while its own list is populated, so this overlay re-arms its own
        // instance and renders it, which is what keeps the drawers alive past
        // FindIt's single-shot window.
        if (!FindItConfig.USE_PARTICLE_HIGHLIGHTER) {
            highlighter.renderHighlightedBlock(event);
        }
    }

    /**
     * Drops drawers that no longer exist and re-issues the surviving set.
     */
    private void refresh(World world) {
        List<ChunkPosition> positions = new ArrayList<>(visible.size());
        for (ChunkPosition position : visible) {
            if (!world.blockExists(position.chunkPosX, position.chunkPosY, position.chunkPosZ)) {
                continue;
            }
            if (!(world.getTileEntity(
                position.chunkPosX,
                position.chunkPosY,
                position.chunkPosZ) instanceof ControllableDrawerTile)) {
                continue;
            }
            if (!positions.contains(position)) {
                positions.add(position);
            }
        }
        if (positions.isEmpty()) {
            clear();
            return;
        }
        visible.clear();
        visible.addAll(positions);
        apply(world, positions);
    }

    @Optional.Method(modid = "findit")
    private void apply(World world, List<ChunkPosition> positions) {
        if (FindItConfig.USE_PARTICLE_HIGHLIGHTER) {
            ParticlePosition.highlightBlocks(world, positions);
            return;
        }
        highlighter.highlightBlocks(positions, expiresAtMillis);
    }

    private static void appendLinkedDrawers(World world, ChunkPosition controller, List<ChunkPosition> target) {
        ConnectedDrawerScope scope = ConnectedDrawerLookup
            .controllerScope(world, controller.chunkPosX, controller.chunkPosY, controller.chunkPosZ, -1);
        if (scope.isEmpty()) {
            return;
        }
        Set<Long> seen = new LinkedHashSet<>(scope.getPositions());
        for (long packed : seen) {
            int x = ConnectedDrawerScope.unpackX(packed);
            int y = ConnectedDrawerScope.unpackY(packed);
            int z = ConnectedDrawerScope.unpackZ(packed);
            if (world.blockExists(x, y, z) && world.getTileEntity(x, y, z) instanceof ControllableDrawerTile) {
                target.add(new ChunkPosition(x, y, z));
            }
        }
    }

    public void clear() {
        visible.clear();
        expiresAtMillis = 0L;
        nextRefreshTick = Long.MIN_VALUE;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (instance != this) {
            return;
        }
        render(event);
    }
}
