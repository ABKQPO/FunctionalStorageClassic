package com.hfstudio.functionalstorage.common.integration.thaumcraft;

import net.minecraftforge.common.util.ForgeDirection;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;

public interface DrawerTransportAccess extends IEssentiaTransport {

    IEssentiaTransport getEssentiaTransport();

    @Override
    default boolean isConnectable(ForgeDirection side) {
        return getEssentiaTransport().isConnectable(side);
    }

    @Override
    default boolean canInputFrom(ForgeDirection side) {
        return getEssentiaTransport().canInputFrom(side);
    }

    @Override
    default boolean canOutputTo(ForgeDirection side) {
        return getEssentiaTransport().canOutputTo(side);
    }

    @Override
    default void setSuction(Aspect aspect, int amount) {
        getEssentiaTransport().setSuction(aspect, amount);
    }

    @Override
    default Aspect getSuctionType(ForgeDirection side) {
        return getEssentiaTransport().getSuctionType(side);
    }

    @Override
    default int getSuctionAmount(ForgeDirection side) {
        return getEssentiaTransport().getSuctionAmount(side);
    }

    @Override
    default int takeEssentia(Aspect aspect, int amount, ForgeDirection side) {
        return getEssentiaTransport().takeEssentia(aspect, amount, side);
    }

    @Override
    default int addEssentia(Aspect aspect, int amount, ForgeDirection side) {
        return getEssentiaTransport().addEssentia(aspect, amount, side);
    }

    @Override
    default Aspect getEssentiaType(ForgeDirection side) {
        return getEssentiaTransport().getEssentiaType(side);
    }

    @Override
    default int getEssentiaAmount(ForgeDirection side) {
        return getEssentiaTransport().getEssentiaAmount(side);
    }

    @Override
    default int getMinimumSuction() {
        return getEssentiaTransport().getMinimumSuction();
    }

    @Override
    default boolean renderExtendedTube() {
        return getEssentiaTransport().renderExtendedTube();
    }
}
