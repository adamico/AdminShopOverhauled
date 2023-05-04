package com.vnator.adminshop.network;

import com.ibm.icu.impl.Pair;
import com.vnator.adminshop.money.MachineOwnerInfo;
import com.vnator.adminshop.setup.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSellerOwnerRequest {
    private final BlockPos pos;

    public PacketSellerOwnerRequest(BlockPos pos) {
        this.pos = pos;
    }

    public PacketSellerOwnerRequest(FriendlyByteBuf buf) {
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

            System.out.println("Requesting machine owner info for pos "+this.pos);
            ServerPlayer player = ctx.getSender();
            assert player != null;
            // Get Machine Owner Info
            MachineOwnerInfo machineOwnerInfo = MachineOwnerInfo.get(player.getLevel());
            Pair<String, Integer> accountInfo = machineOwnerInfo.getMachineAccount(this.pos);
            String machineOwner = machineOwnerInfo.getMachineOwner(pos);
            assert accountInfo != null;
            // Send owner info to player
            Messages.sendToPlayer(new PacketSellerOwner(machineOwner, accountInfo.first, accountInfo.second, this.pos), player);
        });
        return true;
    }
}
