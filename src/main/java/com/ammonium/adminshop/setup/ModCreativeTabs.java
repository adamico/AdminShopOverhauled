package com.ammonium.adminshop.setup;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ModCreativeTabs {
    @SubscribeEvent
    public static void buildContents(CreativeModeTabEvent.Register event) {
        event.registerCreativeModeTab(new ResourceLocation(AdminShop.MODID, "creativetab"), builder ->
                // Set name of tab to display
                builder.title(Component.translatable("item_group." + AdminShop.MODID + ".creativetab"))
                        // Set icon of creative tab
                        .icon(() -> new ItemStack(ModBlocks.SHOP.get()))
                        // Add default items to tab
                        .displayItems((params, output) -> {
                            output.accept(ModBlocks.SHOP.get());
                            output.accept(ModBlocks.BUYER_1.get());
                            output.accept(ModBlocks.BUYER_2.get());
                            output.accept(ModBlocks.BUYER_3.get());
                            output.accept(ModBlocks.SELLER.get());

                        })
        );
    }
}