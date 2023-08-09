package com.ammonium.adminshop.shop;

import com.ammonium.adminshop.AdminShop;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

/**
 * Holder class for something that is buyable or sellable in the shop.
 * Can be an Item or Fluid, or an item or fluid Tag.
 * Cannot have a tag as buyable
 */
public class ShopItem {

    private boolean isBuy;
    private boolean isItem;
    private boolean isTag;
    private long price;
    private int permitTier;
    private ItemStack item;
    private ResourceLocation resourceLocation;
    private TagKey<Item> itemTag;
//    private CompoundTag nbt; //Used with nbt + tags

    private ShopItem(){

    }

    public ItemStack getItem(){
        // Search in item registry if not found
        if (item == null || item.isEmpty()) {
            Item nitem = ForgeRegistries.ITEMS.getValue(resourceLocation);
            if (nitem == null || nitem.getDefaultInstance().isEmpty()) {
                AdminShop.LOGGER.error("No item found with resource location: " + resourceLocation);
            }
            item = nitem != null ? nitem.getDefaultInstance() : null;
        }
        return item;
    }

    public ResourceLocation getResourceLocation() {
        return resourceLocation;
    }

    public long getPrice() {
        return price;
    }
    public TagKey<Item> getTagItem(){return itemTag;}

    public int getPermitTier() {
        return permitTier;
    }

    public boolean isBuy(){return isBuy;}
    public boolean isItem(){return isItem;}
    public boolean isTag(){return isTag;}

    static class Builder{
        private final ShopItem instance;

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

        public Builder setPrice(long p) {
            instance.price = p;
            return this;
        }

        public Builder setPermitTier(int t) {
            instance.permitTier = t;
            return this;
        }

//        public Builder setData(String data, CompoundTag nbt){
//            String [] split = data.split(":");
//            ResourceLocation resource = new ResourceLocation(split[0], split[1]);
//            if(instance.isItem) {
//                if(!instance.isTag) {
//                    instance.item = new ItemStack(ForgeRegistries.ITEMS.getValue(resource), 1, nbt);
//                }else{
//                    instance.itemTag = ItemTags.create(resource);
//                    Optional<Item> item = ForgeRegistries.ITEMS.getValues().stream()
//                            .filter(i -> new ItemStack(i).is(instance.itemTag))
//                            .findFirst();
//                    instance.item = new ItemStack(item.orElse(null));
//                    instance.nbt = nbt;
//                }
//            }
//            return this;
//        }
        public Builder setResourceLocation(ResourceLocation resourceLocation) {
//            Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
//            if (item == null || item.getDefaultInstance().isEmpty()) {
//                AdminShop.LOGGER.error("No item found with registry name: " + resourceLocation.toString());
//            } else {
//                instance.item = item.getDefaultInstance();
//            }
            instance.item = Objects.requireNonNull(
                    ForgeRegistries.ITEMS.getValue(resourceLocation)).getDefaultInstance();
            instance.resourceLocation = resourceLocation;
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
        if(isItem && !isTag) {   //Item
            return item.getItem().getDefaultInstance().getDisplayName().getString();
        } else       //Item Tag
            return I18n.get("gui.item_tag") + ": " + itemTag.location();
    }
}
