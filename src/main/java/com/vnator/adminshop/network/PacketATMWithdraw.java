package com.vnator.adminshop.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketATMWithdraw {
    private final long money;
    private final boolean isCurrency;

    public PacketATMWithdraw(long money, boolean isCurrency){
        this.money = money;
        this.isCurrency = isCurrency;
    }

    public PacketATMWithdraw(FriendlyByteBuf buf){
        money = buf.readInt();
        isCurrency = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf){
        buf.writeLong(money);
        buf.writeBoolean(isCurrency);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            //TODO perform withdrawl for player
        });
        return true;
    }
}
