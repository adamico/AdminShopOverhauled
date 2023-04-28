package com.vnator.adminshop.datagen;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.blocks.ModBlocks;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModBlockStates extends BlockStateProvider {

    public ModBlockStates(DataGenerator gen, ExistingFileHelper helper){
        super(gen, AdminShop.MODID, helper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlock(ModBlocks.SHOP.get());
    }
}
