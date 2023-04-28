package com.vnator.adminshop.datagen;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.blocks.ModBlocks;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModBlockTags extends BlockTagsProvider {

    public ModBlockTags(DataGenerator generator, ExistingFileHelper helper) {
        super(generator, AdminShop.MODID, helper);
    }

    @Override
    protected void addTags(){
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.SHOP.get());
    }

    @Override
    public String getName(){
        return "Admin Shop Tags";
    }
}
