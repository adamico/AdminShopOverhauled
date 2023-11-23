package com.ammonium.adminshop.client.events;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.client.jei.PreparableReloadListener;
import com.ammonium.adminshop.commands.AdminShopCommand;
import com.ammonium.adminshop.commands.ShopAccountsCommand;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.network.PacketSyncMoneyToClient;
import com.ammonium.adminshop.network.PacketSyncShopToClient;
import com.ammonium.adminshop.setup.Messages;
import com.ammonium.adminshop.shop.Shop;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = AdminShop.MODID)
public class ServerEventListeners {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event){
        if(Shop.get().errors.size() > 0)
            Shop.get().printErrors(event.getEntity());
        ServerPlayer player = (ServerPlayer) event.getEntity();
        AdminShop.LOGGER.debug("Calling SyncShop from onPlayerLogin");
        Messages.sendToPlayer(new PacketSyncShopToClient(Shop.get().shopTextRaw), player);
        MoneyManager moneyManager = MoneyManager.get(event.getEntity().level());
        Map<String, List<BankAccount>> sharedAccounts = moneyManager.getSharedAccounts();
        List<BankAccount> usableAccounts;
        if (!sharedAccounts.containsKey(event.getEntity().getStringUUID())) {
            // Create personal account if first login
            int success = moneyManager.CreateAccount(event.getEntity().getStringUUID(), 1);
            if (success == -1) {
                AdminShop.LOGGER.error("Could not create personal account on first login!");
            }
        }
        usableAccounts = moneyManager.getSharedAccounts().get(event.getEntity().getStringUUID());
        if (usableAccounts == null) {
            AdminShop.LOGGER.error("Could not get usableAccounts for player on login.");
            usableAccounts = new ArrayList<>();
        }
        Messages.sendToPlayer(new PacketSyncMoneyToClient(usableAccounts), player);
    }

    @SubscribeEvent
    public static void onCommandRegistration(RegisterCommandsEvent event){
        CommandDispatcher<CommandSourceStack> commandDispatcher = event.getDispatcher();
        AdminShopCommand.register(commandDispatcher);
        ShopAccountsCommand.register(commandDispatcher);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        AdminShop.LOGGER.info("Loading Shop");
        Shop.get();
    }

    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(new PreparableReloadListener());
    }
}
