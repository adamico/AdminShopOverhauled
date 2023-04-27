package com.vnator.adminshop.shop;

import com.mojang.brigadier.CommandDispatcher;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.commands.ReloadShopCommand;
import com.vnator.adminshop.commands.ShopAccountsCommand;
import com.vnator.adminshop.money.MoneyManager;
import com.vnator.adminshop.network.PacketSyncMoneyToClient;
import com.vnator.adminshop.network.PacketSyncShopToClient;
import com.vnator.adminshop.setup.Messages;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ServerEventListeners {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event){
        if(Shop.get().errors.size() > 0)
            Shop.get().printErrors(event.getPlayer());
        Messages.sendToPlayer(new PacketSyncShopToClient(Shop.get().shopTextRaw), (ServerPlayer) event.getPlayer());
        Messages.sendToPlayer(new PacketSyncMoneyToClient(MoneyManager.get(event.getPlayer().getLevel()).getAccountSet()),
                (ServerPlayer) event.getPlayer());
    }

    @SubscribeEvent
    public static void onCommandRegistration(RegisterCommandsEvent event){
        CommandDispatcher<CommandSourceStack> commandDispatcher = event.getDispatcher();
        ReloadShopCommand.register(commandDispatcher);
        ShopAccountsCommand.register(commandDispatcher);
    }
}
