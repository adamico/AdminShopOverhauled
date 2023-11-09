package com.ammonium.adminshop.network;

import com.ammonium.adminshop.shop.Shop;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncShopToClient {
    private final String shopData;

    public PacketSyncShopToClient(String shopData){
        this.shopData = shopData;
    }

    public PacketSyncShopToClient(FriendlyByteBuf buf){
        shopData = buf.readUtf(32767);
    }

    public void toBytes(FriendlyByteBuf buf){
        buf.writeUtf(shopData, 32767);
    }


    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            // Sync shop
            Shop.get().loadFromFile(Minecraft.getInstance().player);

            // Refresh JEI
//            refreshJEIRecipes();
        });
        return true;
    }

//    private void refreshJEIRecipes() {
//        // Check runtime is available
//        if (!JEIRuntime.isAvailable()) {
//            AdminShop.LOGGER.debug("JEI runtime not available yet");
//            return;
//        }
//        IJeiRuntime runtime = JEIRuntime.getRuntime();
//        if (runtime != null) {
//            IRecipeManager recipeManager = runtime.getRecipeManager();
//            RecipeType<ShopBuyWrapper> buyRecipeType = ShopBuyCategory.SHOP_RECIPE_TYPE;
//            Stream<ShopBuyWrapper> oldBuyRecipes = recipeManager.createRecipeLookup(buyRecipeType).get();
//            // recipeManager.removeRecipes() <- not a thing?
//            recipeManager.hideRecipes(buyRecipeType, oldBuyRecipes.collect(Collectors.toSet()));
//            recipeManager.addRecipes(buyRecipeType, Shop.get().getBuyRecipes());
//
//            RecipeType<ShopSellWrapper> sellRecipeType = ShopSellCategory.SHOP_RECIPE_TYPE;
//            Stream<ShopSellWrapper> oldSellRecipes = recipeManager.createRecipeLookup(sellRecipeType).get();
//            recipeManager.hideRecipes(sellRecipeType, oldSellRecipes.collect(Collectors.toSet()));
//            recipeManager.addRecipes(sellRecipeType, Shop.get().getSellRecipes());
//        }
//    }
}
