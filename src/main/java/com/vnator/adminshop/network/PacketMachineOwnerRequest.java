package com.vnator.adminshop.network;

import com.ibm.icu.impl.Pair;
import com.vnator.adminshop.money.MachineOwnerInfo;
import com.vnator.adminshop.setup.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketMachineOwnerRequest {
    private final BlockPos pos;

    public PacketMachineOwnerRequest(BlockPos pos) {
        this.pos = pos;
    }

    public PacketMachineOwnerRequest(FriendlyByteBuf buf) {
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
            Pair<String, Integer> ownerInfo = machineOwnerInfo.getMachineOwner(this.pos);
            assert ownerInfo != null;
            // Send owner info to player
            Messages.sendToPlayer(new PacketMachineOwner(ownerInfo.first, ownerInfo.second, this.pos), player);
        });
        return true;
    }
}
