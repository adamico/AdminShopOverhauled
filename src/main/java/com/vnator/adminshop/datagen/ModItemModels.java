package com.vnator.adminshop.datagen;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.blocks.ModBlocks;
import com.vnator.adminshop.item.ModItems;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.Objects;

public class ModItemModels extends ItemModelProvider {

    public ModItemModels(DataGenerator generator, ExistingFileHelper helper){
        super(generator, AdminShop.MODID, helper);
    }

    @Override
    protected void registerModels() {
        withExistingParent(Objects.requireNonNull(ModBlocks.SHOP.get().getRegistryName()).getPath(), modLoc("block/shop"));

        singleTexture(Objects.requireNonNull(ModItems.CHECK.get().getRegistryName()).getPath(),
                mcLoc("item/generated"),
                "layer0", mcLoc("item/check"));
    }
}
