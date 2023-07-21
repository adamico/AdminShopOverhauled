package com.ammonium.adminshop;

import com.ammonium.adminshop.blocks.ModBlocks;
import com.ammonium.adminshop.blocks.entity.ModBlockEntities;
import com.ammonium.adminshop.client.events.ServerEventListeners;
import com.ammonium.adminshop.item.ModItems;
import com.ammonium.adminshop.screen.ModMenuTypes;
import com.ammonium.adminshop.setup.ClientSetup;
import com.ammonium.adminshop.setup.Config;
import com.ammonium.adminshop.setup.ModSetup;
import com.ammonium.adminshop.shop.Shop;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(AdminShop.MODID)
public class AdminShop {
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "adminshop";

    public AdminShop() {

        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        Config.register();

        eventBus.addListener(ModSetup::init);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> eventBus.addListener(ClientSetup::init));
        MinecraftForge.EVENT_BUS.register(ServerEventListeners.class);

        ModItems.register(eventBus);
        ModBlocks.register(eventBus);
        ModBlockEntities.register(eventBus);
        ModMenuTypes.register(eventBus);

        // Register ourselves for server and other game events we are interested in

    }

    private void setup(final FMLCommonSetupEvent event) {
        // some preinit code
//        LOGGER.info("HELLO FROM PREINIT");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("Loading Shop");
        Shop.get();
    }

    // You can use EventBusSubscriber to automatically subscribe events on the contained class (this is subscribing to the FORGE
    // Event bus for receiving Forge Events)
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {

    }
}
