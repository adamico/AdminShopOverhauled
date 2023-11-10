package com.ammonium.adminshop.client.jei;

import mezz.jei.api.recipe.category.extensions.IRecipeCategoryExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class ShopBuyWrapper implements IRecipeCategoryExtension {
    private final ItemStack buyItem;
    private final Fluid buyFluid;
    private final boolean isItem;
    private final long price;
    private final int requiresTier;

    public ShopBuyWrapper(ItemStack nBuyItem, long nPrice, int nRequiresTier) {
        this.buyItem = nBuyItem;
        this.buyFluid = FluidStack.EMPTY.getFluid();
        this.isItem = true;
        this.price = nPrice;
        this.requiresTier = nRequiresTier;
    }

    public ShopBuyWrapper(Fluid nBuyFluid, long nPrice, int nRequiresTier) {
        this.buyItem = ItemStack.EMPTY;
        this.buyFluid = nBuyFluid;
        this.isItem = false;
        this.price = nPrice;
        this.requiresTier = nRequiresTier;
    }
    public ItemStack getBuyItem() {
        return buyItem;
    }

    public Fluid getBuyFluid() {
        return buyFluid;
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
