package com.ammonium.adminshop.network;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.ClientLocalData;
import com.ammonium.adminshop.setup.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.function.Supplier;

public class PacketMachineOwner {

    private final String machineOwnerUUID;
    private final String accOwnerUUID;
    private final int accID;
    private final BlockPos pos;

    public PacketMachineOwner(String machineOwnerUUID, String accOwnerUUID, int accID, BlockPos pos) {
        this.machineOwnerUUID = machineOwnerUUID;
        this.accOwnerUUID = accOwnerUUID;
        this.accID = accID;
        this.pos = pos;
    }

    public PacketMachineOwner(FriendlyByteBuf buf) {
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
//            Player player = Minecraft.getInstance().player; <-- Not allowed
            AdminShop.LOGGER.error("Receiving seller info for " + this.pos);
            // Update ClientLocalData with received data
            ClientLocalData.addMachineAccount(this.pos, Pair.of(this.accOwnerUUID, this.accID));
            ClientLocalData.addMachineOwner(this.pos, this.machineOwnerUUID);
            // Send open menu packet
            // Don't open menu if player does not have access
            Pair<String, Integer> bankAccount = ClientLocalData.getMachineAccount(this.pos);
            Optional<BankAccount> search = ClientLocalData.getUsableAccounts().stream().filter(account ->
                    bankAccount.equals(Pair.of(this.accOwnerUUID, this.accID))).findAny();
            if (search.isEmpty()) {
                AdminShop.LOGGER.warn("Player does not have access to this machine!");
                ctx.getSender().sendMessage(new TextComponent("You do not have access to this machine!"),
                        ctx.getSender().getUUID());
            } else {
                // Request to open menu
                Messages.sendToServer(new PacketOpenMenu(this.pos));
            }
        });
        return true;
    }

}
