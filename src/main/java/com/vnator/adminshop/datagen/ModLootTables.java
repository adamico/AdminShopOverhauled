package com.vnator.adminshop.datagen;

import com.vnator.adminshop.setup.Registration;
import net.minecraft.data.DataGenerator;

public class ModLootTables extends BaseLootTableProvider{
    public ModLootTables(DataGenerator dataGeneratorIn) {
        super(dataGeneratorIn);
    }

    @Override
    protected void addTables() {
        lootTables.put(Registration.SHOP.get(), createSimpleTable("shop", Registration.SHOP.get()));
        lootTables.put(Registration.ATM.get(), createSimpleTable("atm", Registration.ATM.get()));
        lootTables.put(Registration.BUYER.get(), createSimpleTable("buyer", Registration.BUYER.get()));
        lootTables.put(Registration.SELLER.get(), createSimpleTable("seller", Registration.SELLER.get()));
    }
}
