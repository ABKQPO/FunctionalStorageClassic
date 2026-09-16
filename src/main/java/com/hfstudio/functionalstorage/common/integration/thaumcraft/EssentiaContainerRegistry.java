package com.hfstudio.functionalstorage.common.integration.thaumcraft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.hfstudio.functionalstorage.api.storage.BigAspectStack;
import com.hfstudio.functionalstorage.api.storage.IBigAspectHandler;
import com.hfstudio.functionalstorage.api.storage.StorageAction;
import com.hfstudio.functionalstorage.common.interaction.HeldContainerExchange;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;

/** Registers explicit container capacities because Thaumcraft's item interface does not expose them. */
public class EssentiaContainerRegistry {

    private static final Map<Item, ContainerDefinition> CONTAINERS = new ConcurrentHashMap<>();

    public record ContainerDefinition(int capacity, int emptyMetadata, int filledMetadata) {

        public ContainerDefinition {
            if (capacity <= 0 || emptyMetadata < 0 || filledMetadata < 0) {
                throw new IllegalArgumentException("Invalid essentia container definition");
            }
        }
    }

    private EssentiaContainerRegistry() {}

    public static void register(Item item, ContainerDefinition definition) {
        if (!(item instanceof IEssentiaContainerItem)) {
            throw new IllegalArgumentException("An essentia container must implement IEssentiaContainerItem");
        }
        CONTAINERS.put(item, definition);
    }

    public static boolean activate(EntityPlayer player, IBigAspectHandler handler, int slot) {
        ItemStack held = player.getHeldItem();
        if (held == null || slot < 0 || slot >= handler.getStorageCount()) {
            return false;
        }
        ContainerDefinition definition = CONTAINERS.get(held.getItem());
        if (definition == null || !(held.getItem() instanceof IEssentiaContainerItem container)) {
            return false;
        }
        ItemStack result = held.copy();
        result.stackSize = 1;
        AspectList content = container.getAspects(result);
        if (content != null && content.size() == 1) {
            Aspect aspect = content.getAspects()[0];
            BigAspectStack request = new BigAspectStack(aspect, content.getAmount(aspect));
            if (!request.isEmpty() && handler.insert(slot, request, StorageAction.SIMULATE)
                .isComplete()) {
                container.setAspects(result, new AspectList());
                result.setItemDamage(definition.emptyMetadata());
                handler.insert(slot, request, StorageAction.EXECUTE);
                HeldContainerExchange.complete(player, result);
            }
        } else if (content == null || content.size() == 0) {
            BigAspectStack available = handler.getSnapshot(slot);
            if (available.getAspect() != null && handler.extract(slot, definition.capacity(), StorageAction.SIMULATE)
                .isComplete()) {
                result.setItemDamage(definition.filledMetadata());
                container.setAspects(result, new AspectList().add(available.getAspect(), definition.capacity()));
                handler.extract(slot, definition.capacity(), StorageAction.EXECUTE);
                HeldContainerExchange.complete(player, result);
            }
        }
        return true;
    }
}
