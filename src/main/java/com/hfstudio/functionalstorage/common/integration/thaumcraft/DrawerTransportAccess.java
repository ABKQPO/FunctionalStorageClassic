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
        IEssentiaTransport transport = getEssentiaTransport();
        return transport != null && transport.isConnectable(side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default boolean canInputFrom(ForgeDirection side) {
        IEssentiaTransport transport = getEssentiaTransport();
        return transport != null && transport.canInputFrom(side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default boolean canOutputTo(ForgeDirection side) {
        IEssentiaTransport transport = getEssentiaTransport();
        return transport != null && transport.canOutputTo(side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default void setSuction(Aspect aspect, int amount) {
        IEssentiaTransport transport = getEssentiaTransport();
        if (transport != null) transport.setSuction(aspect, amount);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default Aspect getSuctionType(ForgeDirection side) {
        IEssentiaTransport transport = getEssentiaTransport();
        return transport == null ? null : transport.getSuctionType(side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default int getSuctionAmount(ForgeDirection side) {
        IEssentiaTransport transport = getEssentiaTransport();
        return transport == null ? 0 : transport.getSuctionAmount(side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default int takeEssentia(Aspect aspect, int amount, ForgeDirection side) {
        IEssentiaTransport transport = getEssentiaTransport();
        return transport == null ? 0 : transport.takeEssentia(aspect, amount, side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default int addEssentia(Aspect aspect, int amount, ForgeDirection side) {
        IEssentiaTransport transport = getEssentiaTransport();
        return transport == null ? 0 : transport.addEssentia(aspect, amount, side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default Aspect getEssentiaType(ForgeDirection side) {
        IEssentiaTransport transport = getEssentiaTransport();
        return transport == null ? null : transport.getEssentiaType(side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default int getEssentiaAmount(ForgeDirection side) {
        IEssentiaTransport transport = getEssentiaTransport();
        return transport == null ? 0 : transport.getEssentiaAmount(side);
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default int getMinimumSuction() {
        IEssentiaTransport transport = getEssentiaTransport();
        return transport == null ? Integer.MAX_VALUE : transport.getMinimumSuction();
    }

    @Optional.Method(modid = "Thaumcraft")
    @Override
    default boolean renderExtendedTube() {
        IEssentiaTransport transport = getEssentiaTransport();
        return transport != null && transport.renderExtendedTube();
    }
}
