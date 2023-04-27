package com.vnator.adminshop.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.money.BankAccount;
import com.vnator.adminshop.money.MoneyManager;
import com.vnator.adminshop.network.MojangAPI;
import com.vnator.adminshop.network.PacketSyncMoneyToClient;
import com.vnator.adminshop.setup.Messages;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.stream.Collectors;

public class ShopAccountsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        LiteralArgumentBuilder<CommandSourceStack> shopAccountsCommand = Commands.literal("shopAccounts");

        // /shopAccounts info
        LiteralArgumentBuilder<CommandSourceStack> infoCommand = Commands.literal("info")
                .executes(command -> { return info(command.getSource()); });

        // /shopAccounts listAccounts
        LiteralArgumentBuilder<CommandSourceStack> listAccountsCommand = Commands.literal("listAccounts")
                        .executes((command) -> { return listAccounts(command.getSource()); });

        // /shopAccounts createAccount [<members>]
        LiteralArgumentBuilder<CommandSourceStack> createAccountCommand = Commands.literal("createAccount")
                        .executes((command) -> { return createAccount(command.getSource()); });
        RequiredArgumentBuilder<CommandSourceStack, String> createAccountWithMembersCommand =
                Commands.argument("members", StringArgumentType.greedyString())
                        .executes((command) -> {
                            String users = StringArgumentType.getString(command, "members");
                            return createAccount(command.getSource(), users);
                        });
        createAccountCommand.then(createAccountWithMembersCommand);

        shopAccountsCommand.then(infoCommand)
                        .then(listAccountsCommand)
                        .then(createAccountCommand);
        dispatcher.register(shopAccountsCommand);
    }

    private static int info(CommandSourceStack source) {
        source.sendSuccess(new TextComponent("AdminShop, a mod originally by Vnator and forked by Ammonium_"),
                true);
        return 1;
    }
    private static int listAccounts(CommandSourceStack source) throws CommandSyntaxException {
        // returns 1 (success) or 0 (fail)
        ServerPlayer player = source.getPlayerOrException();
        String playerUUID = player.getStringUUID();
        StringBuilder returnMessage = new StringBuilder("Usable bank accounts for "+player.getName().getString()+
                ":"+"\n");

        if (source.getLevel().isClientSide) {
            AdminShop.LOGGER.error("Can't access this from client side!");
            source.sendFailure(new TextComponent("Can't access this from client side!"));
            return 0;
        }

        MoneyManager moneyManager = MoneyManager.get(source.getLevel());
        if (!moneyManager.getSharedAccounts().containsKey(playerUUID)) {
            AdminShop.LOGGER.error("No accounts found for "+player.getName().getString());
            returnMessage.append("None");
        } else {
            moneyManager.getSharedAccounts().get(playerUUID)
                    .forEach(bankAccount -> {
                returnMessage.append("$");
                returnMessage.append(bankAccount.getBalance());
                returnMessage.append(": ");
                returnMessage.append(MojangAPI.getUsernameByUUID(bankAccount.getOwner()));
                returnMessage.append(':');
                returnMessage.append(bankAccount.getId());
                returnMessage.append("\n");
            });
        }

        source.sendSuccess(new TextComponent(returnMessage.toString()), true);
        return 1;
    }

    private static int createAccount(CommandSourceStack source) throws CommandSyntaxException {
        String members = source.getPlayerOrException().getName().getString();
        return createAccount(source, members);
    }

    private static int createAccount(CommandSourceStack source, String members) throws CommandSyntaxException {
        // Create account to MoneyManager, then sync money to every members' clients
        // Split members string to list
        System.out.println(members);
        ServerPlayer player = source.getPlayerOrException();
        Set<String> memberNames = Set.of(members.split(" "));

        // Validate that all member names are online player names
        Set<String> onlinePlayerNames = new HashSet<>(source.getOnlinePlayerNames());
        boolean valid = onlinePlayerNames.containsAll(memberNames);
        if(!valid) {
            AdminShop.LOGGER.error("Member list invalid. Member list:");
            AdminShop.LOGGER.error(members);
            source.sendFailure(new TextComponent("Member list invalid, all members must be online!"));
            return 0;
        }

        // Try to convert every member to their UUID, fail if can't
        List<ServerPlayer> onlinePlayers = source.getLevel().players();
        Set<String> memberUUIDs = new HashSet<>();
        List<ServerPlayer> memberPlayers = new ArrayList<>();
        memberNames.forEach(name -> {
            Optional<ServerPlayer> searchPlayer = onlinePlayers.stream().filter(serverPlayer -> serverPlayer.getName()
                    .getString().equals(name)).findAny();
            if(searchPlayer.isEmpty()) {
                AdminShop.LOGGER.error("Couldn't find member in onlinePlayers. Member: "+name);
            } else {
                memberPlayers.add(searchPlayer.get());
                memberUUIDs.add(searchPlayer.get().getStringUUID());
            }
        });
        if (memberNames.size() != memberUUIDs.size()) {
            AdminShop.LOGGER.error("Couldn't find all members in online players");
            source.sendFailure(new TextComponent("Couldn't find all members in onlinePlayers. Member list: "
                    +members));
            return 0;
        }

        // Verify that ownerUUID is in memberUUIDs
        if (!memberUUIDs.contains(player.getStringUUID())) {
            AdminShop.LOGGER.info("Owner is not in members list, adding.");
            memberUUIDs.add(player.getStringUUID());
        }

        // Create new account
        int newId = MoneyManager.get(source.getLevel()).CreateAccount(player.getStringUUID(), memberUUIDs);

        if (newId == -1) {
            AdminShop.LOGGER.error("Error creating new account!");
            source.sendFailure(new TextComponent("Error creating new account!"));
            return 0;
        }

        // If successful, sync client data to each member and return new ID to command
        MoneyManager moneyManager = MoneyManager.get(player.getLevel());
        Set<BankAccount> accountSet = moneyManager.getAccountSet();

        // Sync client data with all members
        memberPlayers.forEach(member -> {
                Messages.sendToPlayer(new PacketSyncMoneyToClient(accountSet), member);
        });
        source.sendSuccess(new TextComponent("Created new account with ID "+newId), true);
        return 1;
    }
}
