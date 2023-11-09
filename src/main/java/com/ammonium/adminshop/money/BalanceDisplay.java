package com.ammonium.adminshop.money;

import com.ammonium.adminshop.AdminShop;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = AdminShop.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BalanceDisplay {
    private static long balance = 0;
    private static final long[] history = new long[]{0, 0};
    private static long lastBalance = 0;
    private static int tick = 0;

    private static @Nullable
    LocalPlayer getPlayer() {
        return Minecraft.getInstance().player;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        //if (!Config.emcDisplay.get()) return;
        LocalPlayer player = getPlayer();
        tick++;
        if (event.phase == TickEvent.Phase.END && player != null && tick >= 20) {
            tick = 0;
            //BankAccount selectedAccount = getBankAccount();
            //Pair<String, Integer> selectedAccountInfo = Pair.of(selectedAccount.getOwner(), selectedAccount.getId());
            //For testing purposes
            Pair<String, Integer> selectedAccountInfo = Pair.of("", 1);
            balance = ClientLocalData.getMoney(selectedAccountInfo);
            history[1] = history[0];
            history[0] = balance - lastBalance;
            lastBalance = balance;
        }
    }

    private static void reset() {
        balance = lastBalance = 0;
        tick = 0;
    }

    @SubscribeEvent
    public static void clientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        //if (!Config.emcDisplay.get()) return;
        reset();
    }

    @SubscribeEvent
    public static void onRenderGUI(CustomizeGuiOverlayEvent.DebugText  event) {
        //if (!Config.emcDisplay.get()) return;
        long avg = history[0] + history[1];
        String str = String.valueOf(balance);
        if (avg != 0) str += " " + (avg > 0 ? (ChatFormatting.GREEN + "+") : (ChatFormatting.RED)) + avg + "/s";
        event.getLeft().add(String.format("Balance: %s", str));
    }

}