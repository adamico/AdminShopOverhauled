package com.ammonium.adminshop.network;

import com.ammonium.adminshop.money.MachineOwnerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSetMachineInfo {
    private final String machineOwnerUUID;
    private final String accOwnerUUID;
    private final int accID;
    private final BlockPos pos;

    public PacketSetMachineInfo(String machineOwnerUUID, String accOwnerUUID, int accID, BlockPos pos) {
        this.machineOwnerUUID = machineOwnerUUID;
        this.accOwnerUUID = accOwnerUUID;
        this.accID = accID;
        this.pos = pos;
    }

    public PacketSetMachineInfo(FriendlyByteBuf buf) {
        this.machineOwnerUUID = buf.readUtf();
        this.accOwnerUUID = buf.readUtf();
        this.accID = buf.readInt();
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.machineOwnerUUID);
        buf.writeUtf(this.accOwnerUUID);
        buf.writeInt(this.accID);
        buf.writeBlockPos(this.pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too

            System.out.println("Setting machine owner info for pos "+this.pos);
            ServerPlayer player = ctx.getSender();
            assert player != null;
            // Set Machine Owner Info
            MachineOwnerInfo machineOwnerInfo = MachineOwnerInfo.get(player.getLevel());
            machineOwnerInfo.addMachineInfo(this.pos, this.machineOwnerUUID, this.accOwnerUUID, this.accID);
        });
        return true;
    }
}
