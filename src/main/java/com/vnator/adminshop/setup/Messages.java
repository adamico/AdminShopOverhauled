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

        net.messageBuilder(PacketATMWithdraw.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketATMWithdraw::new)
                .encoder(PacketATMWithdraw::toBytes)
                .consumer(PacketATMWithdraw::handle)
                .add();

        net.messageBuilder(PacketSyncShopToClient.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncShopToClient::new)
                .encoder(PacketSyncShopToClient::toBytes)
                .consumer(PacketSyncShopToClient::handle)
                .add();

    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
