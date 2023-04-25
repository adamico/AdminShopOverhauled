package com.vnator.adminshop.datagen;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.setup.Registration;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModels extends ItemModelProvider {

    public ModItemModels(DataGenerator generator, ExistingFileHelper helper){
        super(generator, AdminShop.MODID, helper);
    }

    @Override
    protected void registerModels() {
        withExistingParent(Registration.SHOP_ITEM.get().getRegistryName().getPath(), modLoc("block/shop"));
        withExistingParent(Registration.ATM_ITEM.get().getRegistryName().getPath(), modLoc("block/atm"));
        withExistingParent(Registration.SELLER_ITEM.get().getRegistryName().getPath(), modLoc("block/seller"));
        withExistingParent(Registration.BUYER_ITEM.get().getRegistryName().getPath(), modLoc("block/buyer"));

        singleTexture(Registration.CHECK.get().getRegistryName().getPath(),
                mcLoc("item/generated"),
                "layer0", mcLoc("item/check"));
    }
}
