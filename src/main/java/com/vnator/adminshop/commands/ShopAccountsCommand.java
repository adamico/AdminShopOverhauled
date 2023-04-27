package com.vnator.adminshop.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.money.BankAccount;
import com.vnator.adminshop.money.MoneyManager;
import com.vnator.adminshop.network.MojangAPI;
import com.vnator.adminshop.network.PacketSyncMoneyToClient;
import com.vnator.adminshop.setup.Messages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class ShopAccountsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        LiteralArgumentBuilder<CommandSourceStack> shopAccountsCommand = Commands.literal("shopAccounts");

        // /shopAccounts info
        LiteralArgumentBuilder<CommandSourceStack> infoCommand = Commands.literal("info")
                .executes(command -> info(command.getSource()));

        // /shopAccounts listAccounts
        LiteralArgumentBuilder<CommandSourceStack> listAccountsCommand = Commands.literal("listAccounts")
                        .executes(command -> listAccounts(command.getSource()));

        // /shopAccounts createAccount [<members>]
        LiteralArgumentBuilder<CommandSourceStack> createAccountCommand = Commands.literal("createAccount")
                        .executes(command -> createAccount(command.getSource()));
        RequiredArgumentBuilder<CommandSourceStack, String> createAccountWithMembersCommand =
                Commands.argument("members", StringArgumentType.greedyString())
                        .executes((command) -> {
                            String users = StringArgumentType.getString(command, "members");
                            return createAccount(command.getSource(), users);
                        });
        createAccountCommand.then(createAccountWithMembersCommand);

        // /shopAccounts deleteAccount [id]
        LiteralArgumentBuilder<CommandSourceStack> deleteAccountCommand = Commands.literal("deleteAccount");
        RequiredArgumentBuilder<CommandSourceStack, Integer> deleteAccountWithIDCommand =
                Commands.argument("id", IntegerArgumentType.integer())
                        .executes(command -> {
                            int id = IntegerArgumentType.getInteger(command, "id");
                            return deleteAccount(command.getSource(), id);
                        });
        deleteAccountCommand.then(deleteAccountWithIDCommand);

        // /shopAccounts addMember [id] [member]
        LiteralArgumentBuilder<CommandSourceStack> addMemberCommand = Commands.literal("addMember");
        RequiredArgumentBuilder<CommandSourceStack, Integer> addMemberCommandID = Commands.argument("id",
                IntegerArgumentType.integer());
        RequiredArgumentBuilder<CommandSourceStack, String> addMemberCommandMember = Commands.argument("member",
                StringArgumentType.string())
                        .executes(command -> {
                            int id = IntegerArgumentType.getInteger(command, "id");
                            String member = StringArgumentType.getString(command, "member");
                            return addMember(command.getSource(), id, member);
                        });
        addMemberCommand.then(addMemberCommandID.then(addMemberCommandMember));

        shopAccountsCommand.then(infoCommand)
                    .then(listAccountsCommand)
                    .then(createAccountCommand)
                    .then(deleteAccountCommand)
                    .then(addMemberCommand);
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
            List<BankAccount> sharedAccountsSorted = moneyManager.getSharedAccounts().get(playerUUID);
            sharedAccountsSorted.sort((o1, o2) -> {
                if (o1.getOwner().equals(playerUUID) && !o2.getOwner().equals(playerUUID)) {
                    return -1;
                } else if (!o1.getOwner().equals(playerUUID) && o2.getOwner().equals(playerUUID)) {
                    return 1;
                } else if (o1.getOwner().equals(o2.getOwner())) {
                    return Integer.compare(o1.getId(), o2.getId());
                } else {
                    return o1.getOwner().compareTo(o2.getOwner());
                }}
            );
            sharedAccountsSorted.forEach(bankAccount -> {
            returnMessage.append("$");
            returnMessage.append(bankAccount.getBalance());
            returnMessage.append(": ");
            returnMessage.append(MojangAPI.getUsernameByUUID(bankAccount.getOwner()));
            returnMessage.append(":");
            returnMessage.append(bankAccount.getId());
            returnMessage.append("\nMembers: ");
            bankAccount.getMembers().forEach(memberUUID -> {
                returnMessage.append(MojangAPI.getUsernameByUUID(memberUUID));
                returnMessage.append(" ");
            });
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
        MoneyManager moneyManager = MoneyManager.get(source.getLevel());
        Set<BankAccount> accountSet = moneyManager.getAccountSet();

        // Sync client data with all members
        memberPlayers.forEach(member -> Messages.sendToPlayer(new PacketSyncMoneyToClient(accountSet), member));
        source.sendSuccess(new TextComponent("Created new account with ID "+newId), true);
        return 1;
    }

    private static int deleteAccount(CommandSourceStack source, int id) throws CommandSyntaxException {
        // Check if trying to delete personal account
        if (id == 1) {
            AdminShop.LOGGER.error("Can't delete personal account!");
            source.sendFailure(new TextComponent("Can't delete personal (id 1) account!"));
            return 0;
        }
        // Get player and moneyManager
        ServerPlayer player = source.getPlayerOrException();
        MoneyManager moneyManager = MoneyManager.get(source.getLevel());
        // Check if player has account with said ID
        if (!moneyManager.existsBankAccount(player.getStringUUID(), id)) {
            source.sendFailure(new TextComponent("There are no accounts you own with said ID!"));
            return 0;
        }
        // Get list of to-be-deleted account's memberUUIDs
        Set<String> memberUUIDs = moneyManager.getBankAccount(player.getStringUUID(), id).getMembers();
        // Delete bank account
        boolean success = moneyManager.deleteBankAccount(player.getStringUUID(), id);
        if(!success) {
            AdminShop.LOGGER.error("Error deleting bank account!");
            source.sendFailure(new TextComponent("Error deleting bank account!"));
        }

        // Get list of online deleted account members
        List<ServerPlayer> onlinePlayers = source.getLevel().players();
        List<ServerPlayer> onlineMembers = new ArrayList<>();
        memberUUIDs.forEach(name -> {
            Optional<ServerPlayer> searchPlayer = onlinePlayers.stream().filter(serverPlayer ->
                    serverPlayer.getStringUUID().equals(name)).findAny();
            searchPlayer.ifPresent(onlineMembers::add);
        });

        // Sync client data with all onlineMembers
        Set<BankAccount> accountSet = moneyManager.getAccountSet();
        onlineMembers.forEach(memberPlayer -> Messages.sendToPlayer(
                new PacketSyncMoneyToClient(accountSet), memberPlayer));

        source.sendSuccess(new TextComponent("Successfully deleted account "+id), true);
        return 1;
    }

    private static int addMember(CommandSourceStack source, int id, String member) throws CommandSyntaxException {
        // Check if trying to add member to personal account
        if (id == 1) {
            AdminShop.LOGGER.error("Can't add member to personal account.");
            source.sendFailure(new TextComponent("Can't add member to personal account (id 1)! Make a shared " +
                    "account with /shopAccounts createAccount [<members>]"));
            return 0;
        }
        // Get player and moneyManager
        ServerPlayer player = source.getPlayerOrException();
        MoneyManager moneyManager = MoneyManager.get(source.getLevel());
        // Check if bank account exists
        if (!moneyManager.existsBankAccount(player.getStringUUID(), id)) {
            AdminShop.LOGGER.error("Can't add member to bank account that doesn't exist.");
            source.sendFailure(new TextComponent("That account ID doesn't exist! Use an existing " +
                    "ID from /shopAccounts listAccounts"));
            return 0;
        }
        // Get memberUUID, fail if can't
        List<ServerPlayer> onlinePlayers = source.getLevel().players();
        Optional<ServerPlayer> searchPlayer = onlinePlayers.stream().filter(serverPlayer -> serverPlayer.getName()
                .getString().equals(member)).findAny();
        if(searchPlayer.isEmpty()) {
            AdminShop.LOGGER.error("Couldn't find member in onlinePlayers.");
            source.sendFailure(new TextComponent("Couldn't find member "+member+"! Member must be online"));
            return 0;
        }
        String memberUUID = searchPlayer.get().getStringUUID();
        // Check if bank account already has member
        if (moneyManager.getBankAccount(player.getStringUUID(), id).getMembers().contains(memberUUID)) {
            AdminShop.LOGGER.error("BankAccount already has member.");
            source.sendFailure(new TextComponent("Account already has member."));
            return 0;
        }
        // Add member, return if failed
        boolean success = moneyManager.addMember(player.getStringUUID(), id, memberUUID);
        if (!success) {
            AdminShop.LOGGER.error("Error adding member to bank account.");
            source.sendFailure(new TextComponent("Error adding member to bank account."));
            return 0;
        }
        // Get list of online members
        List<ServerPlayer> onlineMembers = new ArrayList<>();
        BankAccount newBankAccount = moneyManager.getBankAccount(player.getStringUUID(), id);
        newBankAccount.getMembers().forEach(accountMember -> {
            Optional<ServerPlayer> searchMember = onlinePlayers.stream().filter(serverPlayer ->
                    serverPlayer.getStringUUID().equals(accountMember)).findAny();
            searchMember.ifPresent(onlineMembers::add);
        });
        // Sync client data with all onlineMembers
        Set<BankAccount> accountSet = moneyManager.getAccountSet();
        onlineMembers.forEach(memberPlayer -> Messages.sendToPlayer(
                new PacketSyncMoneyToClient(accountSet), memberPlayer));

        source.sendSuccess(new TextComponent("Successfully added "+member+" to account."), true);
        return 1;
    }
}
