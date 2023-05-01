package com.vnator.adminshop.client.events;

import com.mojang.brigadier.CommandDispatcher;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.commands.ReloadShopCommand;
import com.vnator.adminshop.commands.ShopAccountsCommand;
import com.vnator.adminshop.money.BankAccount;
import com.vnator.adminshop.money.MoneyManager;
import com.vnator.adminshop.network.PacketSyncMoneyToClient;
import com.vnator.adminshop.network.PacketSyncShopToClient;
import com.vnator.adminshop.setup.Messages;
import com.vnator.adminshop.shop.Shop;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//@Mod.EventBusSubscriber(modid = AdminShop.MODID)
public class ServerEventListeners {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event){
        if(Shop.get().errors.size() > 0)
            Shop.get().printErrors(event.getPlayer());
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        Messages.sendToPlayer(new PacketSyncShopToClient(Shop.get().shopTextRaw), player);
        MoneyManager moneyManager = MoneyManager.get(event.getPlayer().getLevel());
        Map<String, List<BankAccount>> sharedAccounts = moneyManager.getSharedAccounts();
        List<BankAccount> usableAccounts;
        if (!sharedAccounts.containsKey(event.getPlayer().getStringUUID())) {
            // Create personal account if first login
            int success = moneyManager.CreateAccount(event.getPlayer().getStringUUID(), 1);
            if (success == -1) {
                AdminShop.LOGGER.error("Could not create personal account on first login!");
            }
        }
        usableAccounts = moneyManager.getSharedAccounts().get(event.getPlayer().getStringUUID());
        if (usableAccounts == null) {
            AdminShop.LOGGER.error("Could not get usableAccounts for player on login.");
            usableAccounts = new ArrayList<>();
        }
        Messages.sendToPlayer(new PacketSyncMoneyToClient(usableAccounts), player);
    }

    @SubscribeEvent
    public static void onCommandRegistration(RegisterCommandsEvent event){
        CommandDispatcher<CommandSourceStack> commandDispatcher = event.getDispatcher();
        ReloadShopCommand.register(commandDispatcher);
        ShopAccountsCommand.register(commandDispatcher);
    }
}
