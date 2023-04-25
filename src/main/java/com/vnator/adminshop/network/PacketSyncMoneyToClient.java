package com.vnator.adminshop.network;

import com.vnator.adminshop.money.ClientMoneyData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncMoneyToClient {

    private final long money;

    public PacketSyncMoneyToClient(long money){
        this.money = money;
    }

    public PacketSyncMoneyToClient(FriendlyByteBuf buf){
        this.money = buf.readLong();
    }

    public void toBytes(FriendlyByteBuf buf){
        buf.writeLong(money);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            ClientMoneyData.setMoney(money);
        });
        return true;
    }
}
