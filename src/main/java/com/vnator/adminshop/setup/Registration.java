package com.vnator.adminshop.setup;

import com.vnator.adminshop.blocks.ShopBE;
import com.vnator.adminshop.blocks.ShopBlock;
import com.vnator.adminshop.blocks.ShopContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.vnator.adminshop.AdminShop.MODID;

public class Registration {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, MODID);
    private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, MODID);

    public static void init(){
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        CONTAINERS.register(bus);
    }

    //Common Properties
    public static final BlockBehaviour.Properties BLOCK_PROPERTIES = BlockBehaviour.Properties.of(Material.METAL).strength(1f)
            .dynamicShape().noOcclusion();
    public static final Item.Properties ITEM_PROPERTIES = new Item.Properties().tab(ModSetup.ITEM_GROUP);

    //Blocks
    public static final RegistryObject<Block> SHOP = BLOCKS.register("shop", ShopBlock::new);
    public static final RegistryObject<Item> SHOP_ITEM = fromBlock(SHOP);
    public static final RegistryObject<BlockEntityType<ShopBE>> SHOP_BE = BLOCK_ENTITIES.register("shop",
            () -> BlockEntityType.Builder.of(ShopBE::new, SHOP.get()).build(null));
    public static final RegistryObject<MenuType<ShopContainer>> SHOP_CONTAINER = CONTAINERS.register("shop",
            () -> IForgeMenuType.create(((windowId, inv, data) -> new ShopContainer(windowId, inv, inv.player))));

    public static final RegistryObject<Block> ATM = BLOCKS.register("atm", () -> new Block(BLOCK_PROPERTIES));
    public static final RegistryObject<Item> ATM_ITEM = fromBlock(ATM);

    public static final RegistryObject<Block> SELLER = BLOCKS.register("seller", () -> new Block(BLOCK_PROPERTIES));
    public static final RegistryObject<Item> SELLER_ITEM = fromBlock(SELLER);

    public static final RegistryObject<Block> BUYER = BLOCKS.register("buyer", () -> new Block(BLOCK_PROPERTIES));
    public static final RegistryObject<Item> BUYER_ITEM = fromBlock(BUYER);

    //Items
    public static final RegistryObject<Item> CHECK = ITEMS.register("check", () -> new Item(ITEM_PROPERTIES));

    // Convenience function: Take a RegistryObject<Block> and make a corresponding RegistryObject<Item> from it
    public static <B extends Block> RegistryObject<Item> fromBlock(RegistryObject<B> block) {
        return ITEMS.register(block.getId().getPath(), () -> new BlockItem(block.get(), ITEM_PROPERTIES));
    }

}
