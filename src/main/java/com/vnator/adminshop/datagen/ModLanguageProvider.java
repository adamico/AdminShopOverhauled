package com.vnator.adminshop.datagen;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.blocks.ModBlocks;
import com.vnator.adminshop.item.ModItems;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;

import static com.vnator.adminshop.setup.ModSetup.TAB_NAME;

public class ModLanguageProvider extends LanguageProvider {

    public ModLanguageProvider(DataGenerator gen, String locale) {
        super(gen, AdminShop.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + TAB_NAME, "Admin Shop");
        add(ModBlocks.SHOP.get(), "Shop");
        add(ModItems.CHECK.get(), "Check");
    }
}
