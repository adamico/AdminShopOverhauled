package com.vnator.adminshop.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.vnator.adminshop.money.MoneyManager;
import com.vnator.adminshop.network.MojangAPI;
import com.vnator.adminshop.setup.Messages;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class ShopAccountsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        LiteralArgumentBuilder<CommandSourceStack> shopAccountsCommand =
                // /shopAccounts listAccounts
                Commands.literal("shopAccounts");

        LiteralArgumentBuilder<CommandSourceStack> listAccountsCommand = Commands.literal("listAccounts")
                        .executes((command) -> { return listAccounts(command.getSource()); });
        shopAccountsCommand.then(listAccountsCommand);
        dispatcher.register(shopAccountsCommand);
    }

    private static int listAccounts(CommandSourceStack source) throws CommandSyntaxException {
        // returns 1 (success) or 0 (fail)
        ServerPlayer player = source.getPlayerOrException();
        String playerUUID = player.getStringUUID();
        StringBuilder returnMessage = new StringBuilder("Bank accounts for "+player.getName().getString()+":"+"\n");

        MoneyManager.get(source.getLevel()).getSharedAccounts().get(playerUUID)
                .forEach(bankAccount -> {
            returnMessage.append("$");
            returnMessage.append(bankAccount.getBalance());
            returnMessage.append(": ");
            returnMessage.append(MojangAPI.getUsernameByUUID(bankAccount.getOwner()));
            returnMessage.append(':');
            returnMessage.append(bankAccount.getId());
            returnMessage.append("\n");
        });

        source.sendSuccess(new TextComponent(returnMessage.toString()), true);
        return 1;
    }
}
