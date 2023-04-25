package com.vnator.adminshop.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketPurchaseRequest {

    private final boolean isBuy;
    private final int category;
    private final int itemIndex;
    private final int quantity;

    public PacketPurchaseRequest(boolean isBuy, int category, int itemIndex, int quantity){
        this.isBuy = isBuy;
        this.category = category;
        this.itemIndex = itemIndex;
        this.quantity = quantity;
    }

    public PacketPurchaseRequest(FriendlyByteBuf buf){
        isBuy = buf.readBoolean();
        category = buf.readInt();
        itemIndex = buf.readInt();
        quantity = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf){
        buf.writeBoolean(isBuy);
        buf.writeInt(category);
        buf.writeInt(itemIndex);
        buf.writeInt(quantity);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            //TODO perform purchase for player
            System.out.println("Perform purchase on: "+isBuy+" , "+category+" , "+itemIndex);
        });
        return true;
    }
}
