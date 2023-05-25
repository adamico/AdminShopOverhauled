package com.ammonium.adminshop.money;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.shop.Shop;
import com.ammonium.adminshop.shop.ShopItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class BuyerTargetInfo extends SavedData {

    private final String COMPOUND_TAG_NAME = "adminshop_buyertargets";
    private final Map<BlockPos, ShopItem> buyerTargetMap = new HashMap<>();

    public void removeBuyerTarget(BlockPos pos) {
        System.out.println("Removing buyer target");
        buyerTargetMap.remove(pos);
        setDirty();
    }
    public void addBuyerTarget(BlockPos pos, ShopItem target) {
        System.out.println("Setting buyer target");
        buyerTargetMap.put(pos, target);
        setDirty();
    }
    public boolean hasTarget(BlockPos pos) {
        return buyerTargetMap.containsKey(pos);
    }
    public ShopItem getBuyerTarget(BlockPos pos) {
        return buyerTargetMap.get(pos);
    }

    public static BuyerTargetInfo get(Level checkLevel){
        if(checkLevel.isClientSide()){
            throw new RuntimeException("Don't access this client-side!");
        }
        MinecraftServer serv = ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = serv.getLevel(Level.OVERWORLD);
        assert level != null;
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(BuyerTargetInfo::new, BuyerTargetInfo::new, "buyertargets");
    }

    //Constructors
    public BuyerTargetInfo(){}

    public BuyerTargetInfo(CompoundTag tag){
        if (tag.contains(COMPOUND_TAG_NAME)) {
            ListTag ledger = tag.getList(COMPOUND_TAG_NAME, 10);
            buyerTargetMap.clear();
            ledger.forEach(ownerTag -> {
                CompoundTag ctag = ((CompoundTag) ownerTag);
                int posx = ctag.getInt("posx");
                int posy = ctag.getInt("posy");
                int posz = ctag.getInt("posz");
                String itemName = ctag.getString("itemname");
                BlockPos pos = new BlockPos(posx, posy, posz);
                AdminShop.LOGGER.info("LOADING BUYER POS:"+pos.toShortString()+", TARGET:"+itemName);
                // Get ShopItem from itemName
                ResourceLocation itemLocation = new ResourceLocation(itemName);
                Item item = ForgeRegistries.ITEMS.getValue(itemLocation);
                ShopItem shopItem = Shop.get().getShopBuyMap().get(item);
                buyerTargetMap.put(pos, shopItem);
            });
        }
    }
    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag ledger = new ListTag();
        buyerTargetMap.forEach((pos, shopItem) -> {
            ResourceLocation itemName = shopItem.getItem().getItem().getRegistryName();
            assert itemName != null;
            AdminShop.LOGGER.info("SAVING BUYER POS:"+pos.toShortString()+", TARGET:"+ itemName);
            CompoundTag ownertag = new CompoundTag();
            ownertag.putInt("posx", pos.getX());
            ownertag.putInt("posy", pos.getY());
            ownertag.putInt("posz", pos.getZ());
            ownertag.putString("itemname", itemName.toString());
            ledger.add(ownertag);
        });
        tag.put(COMPOUND_TAG_NAME, ledger);
        return tag;
    }
}
