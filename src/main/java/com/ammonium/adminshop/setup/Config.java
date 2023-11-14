package com.ammonium.adminshop.setup;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class Config {

    public static ForgeConfigSpec.LongValue STARTING_MONEY;
    public static String SHOP_CONTENTS;
    public static ForgeConfigSpec.BooleanValue balanceDisplay;
    public static ForgeConfigSpec.BooleanValue displayFormat;

    public static void register(){
        ForgeConfigSpec.Builder config = new ForgeConfigSpec.Builder();
        registerConfigs(config);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, config.build());
//        Shop.get(); //Load initial shop stock
    }

    private static void registerConfigs(ForgeConfigSpec.Builder config){
        config.comment("Display configurations. Options for changing client view")
                .push("display_config");

        balanceDisplay = config
                .comment("Displays your current balance and gained balance per second in the top left corner")
                .define("Balance Display", true);

        displayFormat = config
                .comment("If monetary values should be formatted as M/B/T/etc (Short) instead of Million/Billion/Trillion/etc (Full)")
                .define("Short mode ", true );
        config.pop();

        config.comment("General configurations. Shop contents stored in \"adminshop.csv\"")
                .push("general_config");

        STARTING_MONEY = config
                .comment("Amount of money each player starts with. Must be a whole number.")
                .defineInRange("starting_money", 100, 0, Long.MAX_VALUE);
        config.pop();
    }

}
