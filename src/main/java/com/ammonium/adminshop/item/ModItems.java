package com.ammonium.adminshop.item;

import com.ammonium.adminshop.AdminShop;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AdminShop.MODID);

//    public static final RegistryObject<Item> CHECK = ITEMS.register("check",
//            () -> new Item(new Item.Properties().tab(ModSetup.ITEM_GROUP)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}