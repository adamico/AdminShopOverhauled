package com.vnator.adminshop.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MojangAPI {
    public static String getUsernameByUUID(String uuid) {
        try {
            URL url = new URL("https://api.mojang.com/user/profile/" + uuid.replace("-", ""));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Extract the username from the response JSON
                String jsonResponse = response.toString();
                int nameStartIndex = jsonResponse.lastIndexOf("\"name\":\"") + 8;
                int nameEndIndex = jsonResponse.lastIndexOf("\"}");

                return jsonResponse.substring(nameStartIndex, nameEndIndex);
            } else {
                System.out.println("GET request failed: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return uuid;
    }
}

