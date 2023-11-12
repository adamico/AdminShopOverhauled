package com.ammonium.adminshop.setup;

import com.ammonium.adminshop.AdminShop;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

// Config for default account. Is world-specific and should *only* be accessed from the client side
public class ClientConfig {
    private static final String CLIENT_CONFIG_FOLDER = FMLPaths.CONFIGDIR.get().resolve("adminshop").toString();
    private static final Gson GSON = new Gson();

    private static String getServerName() {
        String name;
        try {
            // Attempt to get the save directory name
            MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
            if (server != null) {
                // Integrated server (singleplayer)
                name = server.getWorldData().getLevelName();
            } else {
                // Dedicated server or multiplayer
                name = Minecraft.getInstance().getCurrentServer().ip;
            }
        } catch (Exception e) {
            AdminShop.LOGGER.error("Error getting server name", e);
            name = "default";
        }
        return name;
    }

    public static JsonObject loadClientData() {
        assert Minecraft.getInstance().level != null && Minecraft.getInstance().level.isClientSide;
        File clientDataFile = new File(CLIENT_CONFIG_FOLDER, getServerName() + "_client.json");
        AdminShop.LOGGER.debug("Loading client data from "+clientDataFile);
        JsonObject readObject = new JsonObject();
        if (clientDataFile.exists()) {
            try (FileReader reader = new FileReader(clientDataFile)) {
                readObject = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                AdminShop.LOGGER.debug("Exception while loading client data");
                e.printStackTrace();
            }
        }

        return readObject;
    }

    public static void saveClientData(JsonObject data) {
        assert Minecraft.getInstance().level != null && Minecraft.getInstance().level.isClientSide;
        File clientDataFile = new File(CLIENT_CONFIG_FOLDER, getServerName() + "_client.json");
        AdminShop.LOGGER.debug("Saving client config data to "+clientDataFile);
        // Make sure the directories exist
        clientDataFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(clientDataFile)) {
            GSON.toJson(data, writer);
        } catch (Exception e) {
            AdminShop.LOGGER.debug("Exception while saving client data");
            e.printStackTrace();
        }
    }
    // Gets the default account of the player
    public static Pair<String, Integer> getDefaultAccount() {
        JsonObject clientData = ClientConfig.loadClientData();
        assert Minecraft.getInstance().player != null;
        Pair<String, Integer> defaultAccount = Pair.of(Minecraft.getInstance().player.getStringUUID(), 1);
        if (clientData.has("accOwner") && clientData.has("accId")) {
            defaultAccount = Pair.of(clientData.get("accOwner").getAsString(), clientData.get("accId").getAsInt());
        }
        return defaultAccount;
    }

}
