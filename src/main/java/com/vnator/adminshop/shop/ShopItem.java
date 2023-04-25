package com.vnator.adminshop.shop;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

/**
 * Holder class for something that is buyable or sellable in the shop.
 * Can be an Item or Fluid, or an item or fluid Tag.
 * Cannot have a tag as buyable
 */
public class ShopItem {

    private boolean isBuy;
    private boolean isItem;
    private boolean isTag;

    private ItemStack item;
    private FluidStack fluid;
    private TagKey<Item> itemTag;
    private TagKey<Fluid> fluidTag;
    private CompoundTag nbt; //Used with nbt + tags

    private ShopItem(){

    }

    public ItemStack getItem(){return item;}
    public FluidStack getFluid(){return fluid;}
    public TagKey<Item> getTagItem(){return itemTag;}
    public TagKey<Fluid> getTagFluid(){return fluidTag;}
    public CompoundTag getNbt(){return nbt;}
    public boolean isBuy(){return isBuy;}
    public boolean isItem(){return isItem;}
    public boolean isTag(){return isTag;}

    static class Builder{
        private ShopItem instance;

        public Builder(){
            instance = new ShopItem();
        }

        public Builder setIsBuy(boolean b){
            instance.isBuy = b;
            return this;
        }

        public Builder setIsItem(boolean b){
            instance.isItem = b;
            return this;
        }

        public Builder setIsTag(boolean b){
            instance.isTag = b;
            return this;
        }

        public Builder setData(String data, CompoundTag nbt){
            String [] split = data.split(":");
            ResourceLocation resource = new ResourceLocation(split[0], split[1]);
            if(instance.isItem) {
                if(!instance.isTag) {
                    instance.item = new ItemStack(ForgeRegistries.ITEMS.getValue(resource), 1, nbt);
                }else{
                    instance.itemTag = ItemTags.create(resource);
                    Optional<Item> item = ForgeRegistries.ITEMS.getValues().stream()
                            .filter(i -> new ItemStack(i).is(instance.itemTag))
                            .findFirst();
                    instance.item = new ItemStack(item.orElse(null));
                    instance.nbt = nbt;
                }
            }else{
                if(!instance.isTag){
                    instance.fluid = new FluidStack(ForgeRegistries.FLUIDS.getValue(resource), 1, nbt);
                }else{
                    instance.fluidTag = FluidTags.create(resource);
                    Optional<Fluid> fluid = ForgeRegistries.FLUIDS.getValues().stream()
                            .filter(f -> ForgeRegistries.FLUIDS.getHolder(f).get().is(instance.fluidTag))
                            .findFirst();
                    instance.fluid = new FluidStack(fluid.get(), 1);
                    instance.nbt = nbt;
                }
            }
            return this;
        }

        public ShopItem build(){
            return instance;
        }
    }

    /**
     *
     * @return Display name of the item/fluid contained in this
     */
    public String toString(){
        if(isItem && !isTag)    //Item
            return I18n.get(item.getItem().toString());
        else if(isItem)         //Item Tag
            return I18n.get("gui.item_tag") + ": " + itemTag.location();
        else if(!isTag)         //Fluid
            return fluid.getDisplayName().getString();
        else                    //Fluid Tag
            return I18n.get("gui.fluid_tag") + ": " + itemTag.registry().toString() + "/" + itemTag.location();
    }
}
