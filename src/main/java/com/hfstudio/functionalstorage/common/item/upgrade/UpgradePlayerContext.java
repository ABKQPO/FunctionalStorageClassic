package com.hfstudio.functionalstorage.common.item.upgrade;

import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.ForgeDirection;

import com.mojang.authlib.GameProfile;

import lombok.Getter;

public class UpgradePlayerContext implements AutoCloseable {

    private static final GameProfile DEFAULT_PROFILE = new GameProfile(
        UUID.fromString("e54c991c-6040-46a0-9bc2-cb8da0508781"),
        "[FunctionalStorage]");

    @Getter
    private final FakePlayer player;
    private final ItemStack previousItem;
    private final double previousX;
    private final double previousY;
    private final double previousZ;
    private final float previousYaw;
    private final float previousPitch;

    public UpgradePlayerContext(WorldServer world, ItemStack upgrade, ItemStack held, int x, int y, int z,
        ForgeDirection direction) {
        UUID owner = ((AutomationUpgradeItem) upgrade.getItem()).getOwner(upgrade);
        player = FakePlayerFactory
            .get(world, owner == null ? DEFAULT_PROFILE : new GameProfile(owner, DEFAULT_PROFILE.getName()));
        previousItem = player.getHeldItem();
        previousX = player.posX;
        previousY = player.posY;
        previousZ = player.posZ;
        previousYaw = player.rotationYaw;
        previousPitch = player.rotationPitch;
        player.inventory.setInventorySlotContents(player.inventory.currentItem, held);
        player.setPositionAndRotation(
            x + 0.5D,
            y + 0.5D - player.getEyeHeight(),
            z + 0.5D,
            (float) Math.toDegrees(Math.atan2(-direction.offsetX, direction.offsetZ)),
            -direction.offsetY * 90F);
    }

    @Override
    public void close() {
        player.inventory.setInventorySlotContents(player.inventory.currentItem, previousItem);
        player.setPositionAndRotation(previousX, previousY, previousZ, previousYaw, previousPitch);
    }
}
