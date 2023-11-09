package com.ammonium.adminshop.client.jei;

import mezz.jei.api.recipe.category.extensions.IRecipeCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class ShopSellWrapper implements IRecipeCategoryExtension {
    private final ItemStack sellItem;
    private final Fluid sellFluid;
    private final boolean isItem;
    private final long price;
    private final int requiresTier;

    public ShopSellWrapper(ItemStack nSellItem, long nPrice, int nRequiresTier) {
        this.sellItem = nSellItem;
        this.sellFluid = FluidStack.EMPTY.getFluid();
        this.isItem = true;
        this.price = nPrice;
        this.requiresTier = nRequiresTier;
    }
    public ShopSellWrapper(Fluid nSellFluid, long nPrice, int nRequiresTier) {
        this.sellItem = ItemStack.EMPTY;
        this.sellFluid = nSellFluid;
        this.isItem = false;
        this.price = nPrice;
        this.requiresTier = nRequiresTier;
    }
    public ItemStack getSellItem() {
        return sellItem;
    }

    public Fluid getSellFluid() {
        return sellFluid;
    }

    public boolean isItem() {
        return isItem;
    }

    public long getPrice() {
        return price;
    }


    public int getRequiresTier() {
        return requiresTier;
    }
}
