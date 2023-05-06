package com.vnator.adminshop.setup;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.network.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class Messages {

    private static SimpleChannel INSTANCE;
    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register(){
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(AdminShop.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();
        INSTANCE = net;
        net.messageBuilder(PacketSyncMoneyToClient.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncMoneyToClient::new)
                .encoder(PacketSyncMoneyToClient::toBytes)
                .consumer(PacketSyncMoneyToClient::handle)
                .add();
        net.messageBuilder(PacketPurchaseRequest.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketPurchaseRequest::new)
                .encoder(PacketPurchaseRequest::toBytes)
                .consumer(PacketPurchaseRequest::handle)
                .add();
        net.messageBuilder(PacketSyncShopToClient.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncShopToClient::new)
                .encoder(PacketSyncShopToClient::toBytes)
                .consumer(PacketSyncShopToClient::handle)
                .add();
        net.messageBuilder(PacketMachineOwner.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketMachineOwner::new)
                .encoder(PacketMachineOwner::toBytes)
                .consumer(PacketMachineOwner::handle)
                .add();
        net.messageBuilder(PacketMachineOwnerRequest.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketMachineOwnerRequest::new)
                .encoder(PacketMachineOwnerRequest::toBytes)
                .consumer(PacketMachineOwnerRequest::handle)
                .add();
        net.messageBuilder(PacketOpenMenu.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketOpenMenu::new)
                .encoder(PacketOpenMenu::toBytes)
                .consumer(PacketOpenMenu::handle)
                .add();
        net.messageBuilder(PacketMachineAccountChange.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketMachineAccountChange::new)
                .encoder(PacketMachineAccountChange::toBytes)
                .consumer(PacketMachineAccountChange::handle)
                .add();
        net.messageBuilder(PacketSetBuyerTarget.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketSetBuyerTarget::new)
                .encoder(PacketSetBuyerTarget::toBytes)
                .consumer(PacketSetBuyerTarget::handle)
                .add();
        net.messageBuilder(PacketBuyerInfoRequest.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketBuyerInfoRequest::new)
                .encoder(PacketBuyerInfoRequest::toBytes)
                .consumer(PacketBuyerInfoRequest::handle)
                .add();
        net.messageBuilder(PacketBuyerInfo.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketBuyerInfo::new)
                .encoder(PacketBuyerInfo::toBytes)
                .consumer(PacketBuyerInfo::handle)
                .add();
        net.messageBuilder(PacketSetMachineInfo.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketSetMachineInfo::new)
                .encoder(PacketSetMachineInfo::toBytes)
                .consumer(PacketSetMachineInfo::handle)
                .add();
        net.messageBuilder(PacketSellerTransaction.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketSellerTransaction::new)
                .encoder(PacketSellerTransaction::toBytes)
                .consumer(PacketSellerTransaction::handle)
                .add();
        net.messageBuilder(PacketBuyerTransaction.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketBuyerTransaction::new)
                .encoder(PacketBuyerTransaction::toBytes)
                .consumer(PacketBuyerTransaction::handle)
                .add();
        net.messageBuilder(PacketRemoveSellerInfo.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketRemoveSellerInfo::new)
                .encoder(PacketRemoveSellerInfo::toBytes)
                .consumer(PacketRemoveSellerInfo::handle)
                .add();
        net.messageBuilder(PacketRemoveBuyerInfo.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketRemoveBuyerInfo::new)
                .encoder(PacketRemoveBuyerInfo::toBytes)
                .consumer(PacketRemoveBuyerInfo::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
