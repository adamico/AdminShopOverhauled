package com.vnator.adminshop.blocks.entity;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.blocks.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, AdminShop.MODID);

    public static final RegistryObject<BlockEntityType<SellerBE>> SELLER =
            BLOCK_ENTITIES.register("seller", () -> BlockEntityType.Builder.of(SellerBE::new,
                    ModBlocks.SELLER.get()).build(null));

    public static final RegistryObject<BlockEntityType<ShopBE>> SHOP =
            BLOCK_ENTITIES.register("shop", () -> BlockEntityType.Builder.of(ShopBE::new,
                    ModBlocks.SHOP.get()).build(null));

    public static final RegistryObject<BlockEntityType<BuyerBE>> BUYER_1 =
            BLOCK_ENTITIES.register("buyer_1", () -> BlockEntityType.Builder.of(BuyerBE::new,
                    ModBlocks.BUYER_1.get()).build(null));

    public static final RegistryObject<BlockEntityType<Buyer2BE>> BUYER_2 =
            BLOCK_ENTITIES.register("buyer_2", () -> BlockEntityType.Builder.of(Buyer2BE::new,
                    ModBlocks.BUYER_2.get()).build(null));
    public static final RegistryObject<BlockEntityType<Buyer3BE>> BUYER_3 =
            BLOCK_ENTITIES.register("buyer_3", () -> BlockEntityType.Builder.of(Buyer3BE::new,
                    ModBlocks.BUYER_3.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
