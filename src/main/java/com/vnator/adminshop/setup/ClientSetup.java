package com.vnator.adminshop.setup;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.blocks.ModBlocks;
import com.vnator.adminshop.client.KeyInit;
import com.vnator.adminshop.screen.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = AdminShop.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    public static void init(FMLClientSetupEvent event){
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.SHOP_MENU.get(), ShopScreen::new);
            MenuScreens.<SellerMenu, SellerScreen>register(ModMenuTypes.SELLER_MENU.get(), (SellerMenu menu,
            Inventory playerInventory, Component title) -> new SellerScreen(menu, playerInventory, title,
                    menu.getBlockEntity().getBlockPos()));
            MenuScreens.<BuyerMenu, BuyerScreen>register(ModMenuTypes.BUYER_MENU.get(), (BuyerMenu menu,
            Inventory playerInventory, Component title) -> new BuyerScreen(menu, playerInventory, title,
                    menu.getBlockEntity().getBlockPos()));
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SHOP.get(), RenderType.translucent());
            KeyInit.init();
        });
    }
}
