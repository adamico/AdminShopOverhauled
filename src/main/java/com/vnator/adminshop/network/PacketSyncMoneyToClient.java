package com.vnator.adminshop.network;

import com.ibm.icu.impl.Pair;
import com.vnator.adminshop.money.BankAccount;
import com.vnator.adminshop.money.ClientMoneyData;
import com.vnator.adminshop.money.MoneyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class PacketSyncMoneyToClient {

//    private final long money;
    private final Set<Pair<String, Integer>> sharedAccountsSet = new HashSet<>();
    private final Map<Pair<String, Integer>, Long> accountBalanceMap = new HashMap<>();
    public PacketSyncMoneyToClient(Set<Pair<String, Integer>> sharedAccountsSet, Map<Pair<String, Integer>, Long> accountBalanceMap){
        this.sharedAccountsSet.addAll(sharedAccountsSet);
        this.accountBalanceMap.putAll(accountBalanceMap);
    }

    public PacketSyncMoneyToClient(FriendlyByteBuf buf){
        // Get sep and map size
        int setSize = buf.readInt();
        int mapSize = buf.readInt();

        // Read each sharedAccountsSet Pair from the buffer
        for (int i = 0; i < setSize; i++) {
            String ownerUUID = buf.readUtf();
            int accID = buf.readInt();
            sharedAccountsSet.add(Pair.of(ownerUUID, accID));
        }

        // Read each accountBalanceMap's entry from the buffer
        for (int i = 0; i < mapSize; i++) {
            String ownerUUID = buf.readUtf();
            int accID = buf.readInt();
            long balance = buf.readLong();
            accountBalanceMap.put(Pair.of(ownerUUID, accID), balance);
        }
    }

    public void toBytes(FriendlyByteBuf buf){
        // Add set and map size to buffer
        buf.writeInt(sharedAccountsSet.size());
        buf.writeInt(accountBalanceMap.size());

        // Write each sharedAccountsSet Pair to the buffer
        sharedAccountsSet.forEach(pair -> {
            buf.writeUtf(pair.first);
            buf.writeInt(pair.second);
        });

        // Write each accountBalanceMap's entry to the buffer
        accountBalanceMap.forEach((pair, balance) -> {
            buf.writeUtf(pair.first);
            buf.writeInt(pair.second);
            buf.writeLong(balance);
        });
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            ClientMoneyData.setSharedAccounts(sharedAccountsSet, accountBalanceMap);
        });
        return true;
    }
}
