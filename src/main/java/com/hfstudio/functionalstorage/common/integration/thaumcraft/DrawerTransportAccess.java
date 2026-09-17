package com.hfstudio.functionalstorage.common.integration.thaumcraft;

import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.common.Optional;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;

@Optional.Interface(iface = "thaumcraft.api.aspects.IEssentiaTransport", modid = "Thaumcraft", striprefs = true)
public interface DrawerTransportAccess extends IEssentiaTransport {

    @Optional.Method(modid = "Thaumcraft")
    IEssentiaTransport getEssentiaTransport();

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default boolean isConnectable(ForgeDirection side) {
        return getEssentiaTransport().isConnectable(side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default boolean canInputFrom(ForgeDirection side) {
        return getEssentiaTransport().canInputFrom(side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default boolean canOutputTo(ForgeDirection side) {
        return getEssentiaTransport().canOutputTo(side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default void setSuction(Aspect aspect, int amount) {
        getEssentiaTransport().setSuction(aspect, amount);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default Aspect getSuctionType(ForgeDirection side) {
        return getEssentiaTransport().getSuctionType(side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default int getSuctionAmount(ForgeDirection side) {
        return getEssentiaTransport().getSuctionAmount(side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default int takeEssentia(Aspect aspect, int amount, ForgeDirection side) {
        return getEssentiaTransport().takeEssentia(aspect, amount, side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default int addEssentia(Aspect aspect, int amount, ForgeDirection side) {
        return getEssentiaTransport().addEssentia(aspect, amount, side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default Aspect getEssentiaType(ForgeDirection side) {
        return getEssentiaTransport().getEssentiaType(side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default int getEssentiaAmount(ForgeDirection side) {
        return getEssentiaTransport().getEssentiaAmount(side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default int getMinimumSuction() {
        return getEssentiaTransport().getMinimumSuction();
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default boolean renderExtendedTube() {
        return getEssentiaTransport().renderExtendedTube();
    }
}
