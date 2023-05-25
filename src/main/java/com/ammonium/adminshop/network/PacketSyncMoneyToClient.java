package com.ammonium.adminshop.network;

import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.ClientLocalData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class PacketSyncMoneyToClient {

    private final List<BankAccount> usableAccounts = new ArrayList<>();

    public PacketSyncMoneyToClient(List<BankAccount> usableAccounts){
        this.usableAccounts.addAll(usableAccounts);
    }

    public PacketSyncMoneyToClient(FriendlyByteBuf buf){
        // Get list size
        int listSize = buf.readInt();

        // Get each BankAccount from the buffer
        for (int i = 0; i < listSize; i++) {
            // Owner UUID
            String owner = buf.readUtf();
            // Account ID
            int id = buf.readInt();
            // Balance
            long bal = buf.readLong();

            // Read Members
            Set<String> members = new HashSet<>();
            // Member set size
            int memberSize = buf.readInt();
            // Member UUIDs
            for (int j = 0; j < memberSize; j++) {
                members.add(buf.readUtf());
            }

            // Add to usableAccounts
            usableAccounts.add(new BankAccount(owner, members, id, bal));
        }
    }

    public void toBytes(FriendlyByteBuf buf){
        // Add set and map size to buffer
        buf.writeInt(usableAccounts.size());

        // Write each BankAccount to the buffer
        usableAccounts.forEach(account -> {
            // Owner UUID
            buf.writeUtf(account.getOwner());
            // Account ID
            buf.writeInt(account.getId());
            // Balance
            buf.writeLong(account.getBalance());

            // Write Members
            // Member set size
            buf.writeInt(account.getMembers().size());
            // Member UUIDs
            account.getMembers().forEach(buf::writeUtf);

        });

    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            ClientLocalData.setUsableAccounts(usableAccounts);
        });
        return true;
    }
}
