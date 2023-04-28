package com.vnator.adminshop.datagen;

import com.vnator.adminshop.blocks.ModBlocks;
import net.minecraft.data.DataGenerator;

public class ModLootTables extends BaseLootTableProvider{
    public ModLootTables(DataGenerator dataGeneratorIn) {
        super(dataGeneratorIn);
    }

    @Override
    protected void addTables() {
        lootTables.put(ModBlocks.SHOP.get(), createSimpleTable("shop", ModBlocks.SHOP.get()));
    }
}
