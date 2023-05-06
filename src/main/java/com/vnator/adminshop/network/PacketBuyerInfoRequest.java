package com.vnator.adminshop.network;

import org.apache.commons.lang3.tuple.Pair;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.money.BuyerTargetInfo;
import com.vnator.adminshop.money.MachineOwnerInfo;
import com.vnator.adminshop.setup.Messages;
import com.vnator.adminshop.shop.ShopItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketBuyerInfoRequest {
    private final BlockPos pos;

    public PacketBuyerInfoRequest(BlockPos pos) {
        this.pos = pos;
    }

    public PacketBuyerInfoRequest(FriendlyByteBuf buf) {
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
            AdminShop.LOGGER.info("Requesting buyer info for pos "+this.pos);
            ServerPlayer player = ctx.getSender();
            assert player != null;
            // Get Machine Owner Info
            MachineOwnerInfo machineOwnerInfo = MachineOwnerInfo.get(player.getLevel());
            Pair<String, Integer> accountInfo = machineOwnerInfo.getMachineAccount(this.pos);
            String machineOwner = machineOwnerInfo.getMachineOwner(pos);
            assert accountInfo != null;
            // Get BuyerTargetInfo
            BuyerTargetInfo buyerTargetInfo = BuyerTargetInfo.get(player.getLevel());
            // Check if buyer has target
            boolean hasTarget = buyerTargetInfo.hasTarget(pos);
            if (hasTarget) {
                System.out.println("Target found");
                // Get target
                ShopItem target = buyerTargetInfo.getBuyerTarget(pos);
                // Send owner info plus target to player
                Messages.sendToPlayer(new PacketBuyerInfo(machineOwner, accountInfo.getKey(), accountInfo.getValue(), this.pos,
                        target.getItem().getItem().getRegistryName()), player);
            } else {
                // Send owner info to player
                System.out.println("No target found");
                Messages.sendToPlayer(new PacketBuyerInfo(machineOwner, accountInfo.getKey(), accountInfo.getValue(), this.pos), player);
            }
        });
        return true;
    }
}
