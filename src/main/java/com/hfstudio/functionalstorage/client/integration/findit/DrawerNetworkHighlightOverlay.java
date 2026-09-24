package com.hfstudio.functionalstorage.client.integration.findit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

import com.gtnh.findit.FindItConfig;
import com.gtnh.findit.fx.BlockHighlighter;
import com.gtnh.findit.fx.ParticlePosition;
import com.gtnh.findit.service.itemfinder.FindItemRequest;
import com.hfstudio.functionalstorage.api.storage.ConnectedDrawerScope;
import com.hfstudio.functionalstorage.common.integration.findit.DrawerStackFilter;
import com.hfstudio.functionalstorage.common.integration.findit.FindItQueries;
import com.hfstudio.functionalstorage.common.tile.controller.ConnectedDrawerLookup;
import com.hfstudio.functionalstorage.common.tile.controller.DrawerControllerTile;
import com.hfstudio.functionalstorage.common.tile.controller.StorageNetworkTile;
import com.hfstudio.functionalstorage.config.FunctionalStorageConfig;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

@Optional.Interface(iface = "com.gtnh.findit.fx.BlockHighlighter", modid = "findit", striprefs = true)
public class DrawerNetworkHighlightOverlay {

    private static final int REFRESH_INTERVAL_TICKS = 10;
    private static final long RESPONSE_TIMEOUT_MILLIS = 5000L;

    private static DrawerNetworkHighlightOverlay instance;

    private final BlockHighlighter highlighter = new BlockHighlighter();
    private final List<ChunkPosition> pendingControllers = new ArrayList<>();
    private final List<ChunkPosition> controllers = new ArrayList<>();
    private final List<ChunkPosition> visible = new ArrayList<>();

    private World pendingWorld;
    private World activeWorld;
    private DrawerStackFilter.Query query;
    private long pendingUntilMillis;
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
    public void stage(World world, List<ChunkPosition> positions) {
        clear();
        if (!FunctionalStorageConfig.COMPATIBILITY.highlightLinkedDrawersOnFind) {
            return;
        }
        if (positions == null || positions.isEmpty()) {
            return;
        }
        for (ChunkPosition position : positions) {
            if (isController(world, position)) {
                pendingControllers.add(position);
            }
        }
        if (!pendingControllers.isEmpty()) {
            pendingWorld = world;
            pendingUntilMillis = System.currentTimeMillis() + RESPONSE_TIMEOUT_MILLIS;
        }
    }

    @Optional.Method(modid = "findit")
    public void highlightMatches(World world, ItemStack item) {
        if (world != pendingWorld || System.currentTimeMillis() > pendingUntilMillis || item == null) {
            clear();
            return;
        }
        controllers.addAll(pendingControllers);
        pendingControllers.clear();
        pendingWorld = null;
        activeWorld = world;
        query = FindItQueries.of(new FindItemRequest(item));
        expiresAtMillis = System.currentTimeMillis() + FindItConfig.BLOCK_HIGHLIGHTING_DURATION * 1000L;
        refresh(world);
    }

    @Optional.Method(modid = "findit")
    public void render(RenderWorldLastEvent event) {
        if (controllers.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null || minecraft.thePlayer == null || minecraft.theWorld != activeWorld) {
            clear();
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
        if (visible.isEmpty()) {
            return;
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
     * Rechecks connectivity and contents while the search highlight is active.
     */
    private void refresh(World world) {
        Set<ChunkPosition> positions = new LinkedHashSet<>();
        for (ChunkPosition controller : controllers) {
            if (isController(world, controller)) {
                appendLinkedDrawers(world, controller, query, positions);
            }
        }
        if (positions.isEmpty()) {
            visible.clear();
            highlighter.highlightBlocks(visible, 0L);
            return;
        }
        List<ChunkPosition> matched = new ArrayList<>(positions);
        if (matched.equals(visible)) {
            rearm();
            return;
        }
        visible.clear();
        visible.addAll(matched);
        apply(world, visible);
    }

    @Optional.Method(modid = "findit")
    private void rearm() {
        if (!FindItConfig.USE_PARTICLE_HIGHLIGHTER) {
            highlighter.highlightBlocks(visible, expiresAtMillis);
        }
    }

    private static boolean isController(World world, ChunkPosition position) {
        return world.blockExists(position.chunkPosX, position.chunkPosY, position.chunkPosZ) && world
            .getTileEntity(position.chunkPosX, position.chunkPosY, position.chunkPosZ) instanceof DrawerControllerTile;
    }

    @Optional.Method(modid = "findit")
    private void apply(World world, List<ChunkPosition> positions) {
        if (FindItConfig.USE_PARTICLE_HIGHLIGHTER) {
            ParticlePosition.highlightBlocks(world, positions);
            return;
        }
        highlighter.highlightBlocks(positions, expiresAtMillis);
    }

    private static void appendLinkedDrawers(World world, ChunkPosition controller, DrawerStackFilter.Query query,
        Set<ChunkPosition> target) {
        ConnectedDrawerScope scope = ConnectedDrawerLookup
            .controllerScope(world, controller.chunkPosX, controller.chunkPosY, controller.chunkPosZ, -1);
        if (scope.isEmpty()) {
            return;
        }
        for (long packed : scope.getPositions()) {
            int x = ConnectedDrawerScope.unpackX(packed);
            int y = ConnectedDrawerScope.unpackY(packed);
            int z = ConnectedDrawerScope.unpackZ(packed);
            if (!world.blockExists(x, y, z)) {
                continue;
            }
            TileEntity tile = world.getTileEntity(x, y, z);
            if (tile instanceof StorageNetworkTile) {
                continue;
            }
            DrawerStackFilter filter = DrawerStackFilter.of(tile);
            if (filter != null && filter.test(query)) {
                target.add(new ChunkPosition(x, y, z));
            }
        }
    }

    public void clear() {
        pendingControllers.clear();
        controllers.clear();
        visible.clear();
        pendingWorld = null;
        activeWorld = null;
        query = null;
        pendingUntilMillis = 0L;
        expiresAtMillis = 0L;
        nextRefreshTick = Long.MIN_VALUE;
        highlighter.highlightBlocks(visible, 0L);
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (instance != this) {
            return;
        }
        render(event);
    }
}
