package com.ammonium.adminshop.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ammonium.adminshop.shop.Shop;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ReloadShopCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        LiteralArgumentBuilder<CommandSourceStack> reloadShopCommand =
                Commands.literal("reloadshop")
                        .requires((source) -> source.hasPermission(3))
                        .executes(ReloadShopCommand::reloadShop);
        dispatcher.register(reloadShopCommand);
    }

    static int reloadShop(CommandContext<CommandSourceStack> commandContext){
        try {
            Shop.get().loadFromFile(commandContext.getSource().getPlayerOrException());
        }catch (CommandSyntaxException e){
            return 0;
        }
        return 1;
    }
}
