package com.ammonium.adminshop.blocks;

import net.minecraft.world.MenuProvider;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;

public interface AutoShopMachine extends MenuProvider, IForgeBlockEntity {
    ItemStackHandler getItemHandler();
    void setOwnerUUID(String ownerUUID);
    String getOwnerUUID();
    void setAccount(Pair<String, Integer> account);
    Pair<String, Integer> getAccount();
    void sendUpdates();

}
