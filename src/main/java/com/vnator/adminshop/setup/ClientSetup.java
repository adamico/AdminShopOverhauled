package com.vnator.adminshop.setup;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.blocks.ShopScreen;
import com.vnator.adminshop.client.KeyInit;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = AdminShop.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    public static void init(FMLClientSetupEvent event){
        event.enqueueWork(() -> {
            MenuScreens.register(Registration.SHOP_CONTAINER.get(), ShopScreen::new);
            ItemBlockRenderTypes.setRenderLayer(Registration.SHOP.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(Registration.ATM.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(Registration.SELLER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(Registration.BUYER.get(), RenderType.translucent());
            KeyInit.init();
        });
    }
}
