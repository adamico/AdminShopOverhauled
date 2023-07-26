package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ShopContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, AdminShop.MODID);

    public static final RegistryObject<MenuType<ShopContainer>> SHOP_MENU = MENUS.register("shop_menu",
            () -> IForgeMenuType.create(((windowId, inv, data) -> new ShopContainer(windowId, inv, inv.player))));

    public static final RegistryObject<MenuType<SellerMenu>> SELLER_MENU = MENUS.register("seller_menu",
            () -> IForgeMenuType.create((SellerMenu::new)));

    public static final RegistryObject<MenuType<BuyerMenu>> BUYER_MENU = MENUS.register("buyer_menu",
            () -> IForgeMenuType.create((BuyerMenu::new)));

    public static final RegistryObject<MenuType<Buyer2Menu>> BUYER_2_MENU = MENUS.register("buyer_2_menu",
            () -> IForgeMenuType.create((Buyer2Menu::new)));

    public static final RegistryObject<MenuType<Buyer3Menu>> BUYER_3_MENU = MENUS.register("buyer_3_menu",
            () -> IForgeMenuType.create((Buyer3Menu::new)));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
