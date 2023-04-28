package com.vnator.adminshop.setup;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.blocks.ModBlocks;
import com.vnator.adminshop.screen.ModMenuTypes;
import com.vnator.adminshop.screen.ShopScreen;
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
            MenuScreens.register(ModMenuTypes.SHOP_MENU.get(), ShopScreen::new);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.SHOP.get(), RenderType.translucent());
            KeyInit.init();
        });
    }
}
