package com.ammonium.adminshop.setup;

import com.ammonium.adminshop.shop.Shop;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class Config {

    public static ForgeConfigSpec.LongValue STARTING_MONEY;
    public static ForgeConfigSpec.DoubleValue PRICE_PER_ENERGY;
    public static ForgeConfigSpec.IntValue LIQUID_SELL_PACKET_SIZE;
    public static ForgeConfigSpec.IntValue POWER_SELL_PACKET_SIZE;
    public static String SHOP_CONTENTS;

    public static void register(){
        ForgeConfigSpec.Builder config = new ForgeConfigSpec.Builder();
        registerConfigs(config);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, config.build());
        Shop.get(); //Load initial shop stock
    }

    private static void registerConfigs(ForgeConfigSpec.Builder config){
        config.comment("General configurations. Shop contents stored in \"adminshop.csv\"")
                .push("general_config");

        STARTING_MONEY = config
                .comment("Amount of money each player starts with. Must be a whole number.")
                .defineInRange("starting_money", 100, 0, Long.MAX_VALUE);
        PRICE_PER_ENERGY = config
                .comment("Sell price for each unit of Forge Energy.",
                        "Keep this low or else a simple 100 FE/t generator will make 2000x this price per second!")
                .defineInRange("power_price", 0.0005, 0, Double.MAX_VALUE);
        LIQUID_SELL_PACKET_SIZE = config
                .comment("How much fluid needs to be inside the auto seller before it converts it to money.",
                        "Higher values might reduce lag, lower values help with slow fluid production (eg. UU matter)",
                        "Does NOT affect prices, modify only if you know what you're doing.")
                .defineInRange("liquid_sell_packet_size", 10, 1, Integer.MAX_VALUE);
        POWER_SELL_PACKET_SIZE = config
                .comment("How much power needs to be inside the auto seller before it converts it to money.",
                        "Higher values might reduce lag, lower values help with low power production (eg. low tech)",
                        "DOES NOT affect prices, use power_price to set that instead!")
                .defineInRange("power_sell_packet_size", 100, 1, Integer.MAX_VALUE);
        config.pop();
    }
}
