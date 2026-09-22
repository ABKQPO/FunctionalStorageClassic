package com.hfstudio.functionalstorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.Item;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hfstudio.functionalstorage.api.storage.BigItemStack;
import com.hfstudio.functionalstorage.api.storage.IBigItemHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.inventory.AggregatedStorage;
import com.hfstudio.functionalstorage.common.inventory.adapter.DrawerItemInventory;
import com.hfstudio.functionalstorage.common.inventory.base.BigItemHandler;
import com.hfstudio.functionalstorage.support.StorageFixtures;
import com.hfstudio.functionalstorage.support.VanillaBootstrap;

/**
 * Asserts that a bridge built over a network follows the network as it grows.
 * <p>
 * A controller's index count changes when a drawer is linked, unlinked, or changes
 * layout, and the bridge that exposes it sizes its slot array once. Reusing a stale
 * array would leave newly linked drawers unreachable and could address indices that no
 * longer exist, so the tests here grow and shrink the membership and then read through
 * a freshly built bridge each time.
 */
public class BridgeResizeTest {

    @BeforeAll
    static void installVanillaState() {
        VanillaBootstrap.install();
    }

    @Test
    @DisplayName("a bridge built after growth spans the new index count")
    void bridgeFollowsGrowth() {
        BigItemHandler first = new BigItemHandler(2);
        List<IBigItemHandler> children = new ArrayList<>();
        children.add(first);

        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(children);

        DrawerItemInventory small = new DrawerItemInventory(network, "network", () -> {});
        assertEquals(2, small.getSizeInventory(), "the bridge must span the initial drawers");

        // Link a second drawer, which the aggregate learns about on rebuild.
        BigItemHandler second = new BigItemHandler(3);
        Item item = StorageFixtures.newItem();
        second.insert(2, new BigItemStack(StorageFixtures.one(item), 42L), StorageAction.EXECUTE);
        children.add(second);
        assertTrue(network.rebuild(children), "linking a drawer must rebuild");
        assertEquals(5, network.getStorageCount(), "the aggregate must span the new drawer");

        DrawerItemInventory rebuilt = new DrawerItemInventory(network, "network", () -> {});
        assertEquals(5, rebuilt.getSizeInventory(), "a bridge built after growth must span every index");

        // The newly linked drawer must be reachable through the aggregate index.
        assertTrue(rebuilt.getStackInSlot(4) != null, "the last index must address the newly linked drawer");
        assertEquals(42, rebuilt.getStackInSlot(4).stackSize, "the new drawer's contents must be reported");
    }

    @Test
    @DisplayName("an index beyond the current count is refused, not read from a stale array")
    void staleIndicesAreRefused() {
        BigItemHandler first = new BigItemHandler(2);
        BigItemHandler second = new BigItemHandler(2);
        List<IBigItemHandler> children = new ArrayList<>();
        children.add(first);
        children.add(second);

        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(children);
        DrawerItemInventory before = new DrawerItemInventory(network, "network", () -> {});
        assertEquals(4, before.getSizeInventory(), "the bridge must span both drawers");

        // Unlink the second drawer.
        children.remove(second);
        assertTrue(network.rebuild(children), "unlinking must rebuild");
        assertEquals(2, network.getStorageCount(), "the aggregate must shrink");

        DrawerItemInventory after = new DrawerItemInventory(network, "network", () -> {});
        assertEquals(2, after.getSizeInventory(), "a rebuilt bridge must span only the remaining drawer");
        assertNull(after.getStackInSlot(3), "an index that no longer exists must hand out nothing");
        assertNull(after.getStackInSlot(9), "a never-existing index must hand out nothing");
    }

    @Test
    @DisplayName("a drawer whose layout grows shifts later indices but keeps its own")
    void growingADrawerShiftsLaterIndices() {
        BigItemHandler first = new BigItemHandler(1);
        BigItemHandler second = new BigItemHandler(1);
        Item firstItem = StorageFixtures.newItem();
        Item secondItem = StorageFixtures.newItem();

        first.insert(0, new BigItemStack(StorageFixtures.one(firstItem), 5L), StorageAction.EXECUTE);
        second.insert(0, new BigItemStack(StorageFixtures.one(secondItem), 7L), StorageAction.EXECUTE);

        List<IBigItemHandler> children = new ArrayList<>();
        children.add(first);
        children.add(second);
        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(children);

        DrawerItemInventory twoSlot = new DrawerItemInventory(network, "network", () -> {});
        assertNotNull(twoSlot.getStackInSlot(1), "index one must address the second drawer");

        // Resize the first drawer, which pushes the second drawer's index outward.
        BigItemHandler widened = new BigItemHandler(3);
        widened.insert(0, new BigItemStack(StorageFixtures.one(firstItem), 5L), StorageAction.EXECUTE);
        List<IBigItemHandler> replaced = new ArrayList<>();
        replaced.add(widened);
        replaced.add(second);
        network.rebuild(replaced);

        DrawerItemInventory resized = new DrawerItemInventory(network, "network", () -> {});
        assertEquals(4, resized.getSizeInventory(), "the bridge must span the widened layout");
        assertNotNull(resized.getStackInSlot(3), "the second drawer's index must have moved outward");
        assertEquals(7, resized.getStackInSlot(3).stackSize, "the moved index must report the same contents");
    }

    @Test
    @DisplayName("an empty network yields a bridge that spans nothing")
    void emptyNetworkBridgeIsHarmless() {
        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(List.of());

        DrawerItemInventory bridge = new DrawerItemInventory(network, "network", () -> {});
        assertEquals(0, bridge.getSizeInventory(), "an empty network must expose no slots");
        assertNull(bridge.getStackInSlot(0), "an empty network must hand out nothing");
        assertNull(bridge.getStackInSlot(-1), "a negative index must hand out nothing");
    }

    @Test
    @DisplayName("a rebuilt bridge still commits edits made through it")
    void rebuiltBridgeStillCommits() {
        BigItemHandler first = new BigItemHandler(2);
        BigItemHandler second = new BigItemHandler(2);
        List<IBigItemHandler> children = new ArrayList<>();
        children.add(first);
        children.add(second);

        AggregatedStorage.Items network = new AggregatedStorage.Items();
        network.rebuild(children);

        Item item = StorageFixtures.newItem();
        DrawerItemInventory bridge = new DrawerItemInventory(network, "network", () -> {});
        bridge.setInventorySlotContents(3, StorageFixtures.stack(item, 20));
        bridge.markDirty();

        assertEquals(
            20L,
            StorageFixtures.total(second, item),
            "a write through the bridge must reach the drawer behind that index");
        assertEquals(0L, StorageFixtures.total(first, item), "an unrelated drawer must be untouched");
    }
}
