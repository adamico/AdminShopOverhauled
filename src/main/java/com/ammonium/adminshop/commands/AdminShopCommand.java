package com.ammonium.adminshop.commands;

import com.ammonium.adminshop.item.ModItems;
import com.ammonium.adminshop.shop.Shop;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class AdminShopCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        LiteralArgumentBuilder<CommandSourceStack> adminShopCommand = Commands.literal("adminshop");

        // adminshop reload
        LiteralArgumentBuilder<CommandSourceStack> reloadShopCommand = Commands.literal("reload")
                        .requires(source -> source.hasPermission(3))
                        .executes(command -> reloadShop(command.getSource()));

        // adminshop getPermit [tier]
        LiteralArgumentBuilder<CommandSourceStack> getPermitCommand = Commands.literal("getPermit").requires(source -> source.hasPermission(3));
        RequiredArgumentBuilder<CommandSourceStack, Integer> getPermitCommandTier = Commands.argument("tier", IntegerArgumentType.integer())
                        .executes(command -> {
                            int tier = IntegerArgumentType.getInteger(command, "tier");
                            return getPermit(command.getSource(), tier);
                        });
        getPermitCommand.then(getPermitCommandTier);

        adminShopCommand.then(reloadShopCommand)
                        .then(getPermitCommand);
        dispatcher.register(adminShopCommand);
    }

    static int reloadShop(CommandSourceStack source){
        try {
            Shop.get().loadFromFile(source.getPlayerOrException());
        }catch (CommandSyntaxException e){
            return 0;
        }
        return 1;
    }

    static int getPermit(CommandSourceStack source, int tier) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (tier == 0) {
            source.sendFailure(Component.literal("Permit tier 0 is the default, all accounts have it!"));
            return 0;
        }

        // Give item
        ItemStack permit = new ItemStack(ModItems.PERMIT.get());
        CompoundTag key = permit.getOrCreateTag();
        key.putInt("key", tier);
        permit.setTag(key);

        boolean success = player.getInventory().add(permit);
        if (!success) {
            source.sendFailure(Component.literal("Unable to give permit to player"));
            return 0;
        }
        source.sendSuccess(Component.literal("Obtained trade permit"), true);
        return 1;
    }
}
