package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.AutoShopMachine;
import com.ammonium.adminshop.money.BuyerTargetInfo;
import com.ammonium.adminshop.money.MachineOwnerInfo;
import com.ammonium.adminshop.shop.Shop;
import com.ammonium.adminshop.shop.ShopItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class PacketSetBuyerTarget {
    private BlockPos pos;
    private ResourceLocation itemName;

    public PacketSetBuyerTarget(BlockPos pos, ResourceLocation itemName) {
        this.pos = pos;
        this.itemName = itemName;
    }

    public PacketSetBuyerTarget(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.itemName = buf.readResourceLocation();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeResourceLocation(this.itemName);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too

            // Change machine's account
            ServerPlayer player = ctx.getSender();

            if (player != null) {
                System.out.println("Setting buyer target for "+this.pos+" to "+this.itemName.getNamespace());
                // Get IBuyerBE
                Level level = player.level;
                BlockEntity blockEntity = level.getBlockEntity(this.pos);
                if (!(blockEntity instanceof AutoShopMachine)) {
                    AdminShop.LOGGER.error("BlockEntity at pos is not BuyerBE");
                    return;
                }
                // Check machine's owner is the same as player
                MachineOwnerInfo machineOwnerInfo = MachineOwnerInfo.get(player.getLevel());
                if (!machineOwnerInfo.getMachineOwner(this.pos).equals(player.getStringUUID())) {
                    AdminShop.LOGGER.error("Player is not the machine's owner");
                    return;
                }
                // Get item from itemName
                Item item = ForgeRegistries.ITEMS.getValue(this.itemName);
                // Check if item is in buyMap;
                if (!Shop.get().getShopBuyMap().containsKey(item)) {
                    AdminShop.LOGGER.error("Item is not in BuyMap");
                    return;
                }
                System.out.println("Saving machine account information.");
                ShopItem shopItem = Shop.get().getShopBuyMap().get(item);
                // Apply changes to BuyerTargetInfo
                BuyerTargetInfo buyerTargetInfo = BuyerTargetInfo.get(player.getLevel());
                buyerTargetInfo.addBuyerTarget(this.pos, shopItem);
            }
        });
        return true;
    }
}
