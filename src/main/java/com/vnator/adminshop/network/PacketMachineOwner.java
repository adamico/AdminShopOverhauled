package com.vnator.adminshop.network;

import com.ibm.icu.impl.Pair;
import com.vnator.adminshop.blocks.entity.SellerBE;
import com.vnator.adminshop.money.ClientLocalData;
import com.vnator.adminshop.setup.Messages;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketMachineOwner {
    private final String ownerUUID;
    private final int accID;
    private final BlockPos pos;

    public PacketMachineOwner(String ownerUUID, int accID, BlockPos pos) {
        this.ownerUUID = ownerUUID;
        this.accID = accID;
        this.pos = pos;
    }

    public PacketMachineOwner(FriendlyByteBuf buf) {
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
            Player player = Minecraft.getInstance().player;

            if (player != null) {
                System.out.println("Sending machine ownership info for "+this.pos+" to "+player.getName().getString());
                // Update ClientLocalData with received data
                ClientLocalData.addMachineOwner(this.pos, Pair.of(this.ownerUUID, this.accID));
                ClientLocalData.addUuidToNameMap(this.ownerUUID, MojangAPI.getUsernameByUUID(this.ownerUUID));

                // Send open menu packet
                Level level = player.level;
                BlockEntity blockEntity = level.getBlockEntity(this.pos);
                if (blockEntity instanceof SellerBE) {
                    Messages.sendToServer(new PacketOpenMenu(this.pos));
                }
            }
        });
        return true;
    }

}
