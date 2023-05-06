package com.vnator.adminshop.blocks;

import net.minecraft.world.MenuProvider;
import net.minecraftforge.items.ItemStackHandler;

public interface AutoShopMachine extends MenuProvider {
    public ItemStackHandler getItemHandler();

}
