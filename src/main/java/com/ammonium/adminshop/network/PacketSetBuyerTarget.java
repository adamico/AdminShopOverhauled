package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.BuyerMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSetBuyerTarget {
    private final BlockPos pos;
    private final int buyIndex;

    public PacketSetBuyerTarget(BlockPos pos, int buyIndex) {
        this.pos = pos;
        this.buyIndex = buyIndex;
    }

    public PacketSetBuyerTarget(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.buyIndex = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeInt(this.buyIndex);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too

            // Change machine's account
            ServerPlayer player = ctx.getSender();

            if (player != null) {
                System.out.println("Setting buyer target for "+this.pos+" to "+this.buyIndex);
                // Get IBuyerBE
                Level level = player.level;
                BlockEntity blockEntity = level.getBlockEntity(this.pos);
                if (!(blockEntity instanceof BuyerMachine buyerEntity)) {
                    AdminShop.LOGGER.error("BlockEntity at pos is not BuyerMachine");
                    return;
                }
                // Check machine's owner is the same as player
                if (!buyerEntity.getOwnerUUID().equals(player.getStringUUID())) {
                    AdminShop.LOGGER.error("Player is not the machine's owner");
                    return;
                }
                System.out.println("Saving machine account information.");
                // Apply changes to buyerEntity
                buyerEntity.setShopBuyIndex(this.buyIndex);
                blockEntity.setChanged();
                buyerEntity.sendUpdates();
            }
        });
        return true;
    }
}
