package com.ammonium.adminshop.setup;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.network.*;
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
        net.messageBuilder(PacketAccountAddPermit.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketAccountAddPermit::new)
                .encoder(PacketAccountAddPermit::toBytes)
                .consumer(PacketAccountAddPermit::handle)
                .add();
        net.messageBuilder(PacketUpdateRequest.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketUpdateRequest::new)
                .encoder(PacketUpdateRequest::toBytes)
                .consumer(PacketUpdateRequest::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
