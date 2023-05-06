package com.vnator.adminshop.network;

import com.vnator.adminshop.money.MachineOwnerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

public class PacketRemoveSellerInfo {
    private BlockPos pos;
    public PacketRemoveSellerInfo(BlockPos pos) {
        this.pos = pos;
    }
    public PacketRemoveSellerInfo(FriendlyByteBuf buf) {
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
            System.out.println("Removing seller info for "+this.pos);
            ServerLevel level = Objects.requireNonNull(ctx.getSender()).getLevel();
            MachineOwnerInfo.get(level).removeMachineInfo(this.pos);

        });
        return true;
    }
}
