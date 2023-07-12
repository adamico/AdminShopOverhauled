package com.ammonium.adminshop.screen.slot;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.shop.Shop;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ModShopInputSlot extends SlotItemHandler {
    public ModShopInputSlot(IItemHandler itemHandler, int index, int x, int y) {
        super(itemHandler, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        boolean result = Shop.get().getShopSellMap().containsKey(stack.getItem());
        if (!result) {
            AdminShop.LOGGER.error("Cannot place item into seller: "+stack.getItem().getRegistryName());
        }
        return result;
    }

    @Override
    public boolean mayPickup(Player playerIn) {
        return true;
    }

    @Override
    public int getMaxStackSize(ItemStack pStack) {
        return super.getMaxStackSize(pStack);
    }

}
