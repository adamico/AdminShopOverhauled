package com.vnator.adminshop.screen.slot;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.shop.Shop;
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
            AdminShop.LOGGER.error("Cannot place this item into the seller slot");
            System.out.println("Cannot place this item into the seller slot");
            System.out.println(stack.getItem());
        }
        return result;
    }

    @Override
    public boolean mayPickup(Player playerIn) {
        return false;
    }

    @Override
    public int getMaxStackSize(ItemStack pStack) {
        return super.getMaxStackSize(pStack);
    }

}
