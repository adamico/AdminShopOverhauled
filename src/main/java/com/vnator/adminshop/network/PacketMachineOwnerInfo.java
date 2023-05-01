package com.vnator.adminshop.network;

import com.vnator.adminshop.money.ClientMoneyData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketMachineOwnerInfo {
    private final String ownerUUID;
    private final int accID;
    private final BlockPos pos;

    public PacketMachineOwnerInfo(String ownerUUID, int accID, BlockPos pos) {
        this.ownerUUID = ownerUUID;
        this.accID = accID;
        this.pos = pos;
    }

    public PacketMachineOwnerInfo(FriendlyByteBuf buf) {
        this.ownerUUID = buf.readUtf();
        this.accID = buf.readInt();
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.ownerUUID);
        buf.writeInt(this.accID);
        buf.writeBlockPos(this.pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            ClientMoneyData.addMachineOwnership(this.pos, this.ownerUUID, this.accID);
        });
        return true;
    }

}
