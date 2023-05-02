package com.vnator.adminshop.network;

import com.vnator.adminshop.blocks.entity.SellerBE;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class PacketOpenMenu {
    private BlockPos pos;

    public PacketOpenMenu(BlockPos pos) {
        this.pos = pos;
    }
    public PacketOpenMenu(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                System.out.println("Opening menu at pos " + this.pos + ", player "+player.getName().getString());
                // Open the menu
                Level level = player.level;
                BlockEntity blockEntity = level.getBlockEntity(this.pos);
                if (blockEntity instanceof SellerBE) {
                    NetworkHooks.openGui(player, (SellerBE)blockEntity, this.pos);
                }
            }
        });
        return true;
    }
}
