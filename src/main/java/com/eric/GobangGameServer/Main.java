// src/main/java/com/eric/GobangGameServer/Main.java
package com.eric.GobangGameServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Scanner;

/**
 * Main entry point for the Gobang Game Server
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int DEFAULT_PORT = 12345;
    private static final int DEFAULT_MAX_PLAYERS = 100;

    private static GameServer server;

    public static void main(String[] args) {
        try {
            int port = DEFAULT_PORT;
            int maxPlayers = DEFAULT_MAX_PLAYERS;

            // Parse command line arguments
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-p":
                    case "--port":
                        if (i + 1 < args.length) {
                            port = Integer.parseInt(args[++i]);
                        }
                        break;
                    case "-m":
                    case "--max-players":
                        if (i + 1 < args.length) {
                            maxPlayers = Integer.parseInt(args[++i]);
                        }
                        break;
                    case "-h":
                    case "--help":
                        printUsage();
                        return;
                }
            }

            // Validate port
            if (port < 1024 || port > 65535) {
                logger.error("Port must be between 1024 and 65535");
                System.exit(1);
            }

            // Validate max players
            if (maxPlayers < 2 || maxPlayers > 1000) {
                logger.error("Max players must be between 2 and 1000");
                System.exit(1);
            }

            // Start server
            server = new GameServer(port, maxPlayers);

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutdown signal received");
                if (server != null) {
                    server.stop();
                }
            }));

            // Start server in a separate thread
            Thread serverThread = new Thread(() -> server.start());
            serverThread.setDaemon(false);
            serverThread.start();

            // Console command loop
            handleConsoleCommands();

        } catch (Exception e) {
            logger.error("Failed to start server: {}", e.getMessage());
            System.exit(1);
        }
    }

    private static void handleConsoleCommands() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("========================================");
        System.out.println("    Gobang Game Server Console");
        System.out.println("========================================");
        System.out.println("Commands:");
        System.out.println("  status    - Show server status");
        System.out.println("  players   - List connected players");
        System.out.println("  sessions  - List active sessions");
        System.out.println("  stop      - Stop the server");
        System.out.println("  help      - Show this help");
        System.out.println("  exit      - Exit console (server continues)");
        System.out.println("========================================");

        while (true) {
            System.out.print("> ");
            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "status":
                    printServerStatus();
                    break;

                case "players":
                    printConnectedPlayers();
                    break;

                case "sessions":
                    printActiveSessions();
                    break;

                case "stop":
                    System.out.println("Stopping server...");
                    if (server != null) {
                        server.stop();
                    }
                    scanner.close();
                    System.exit(0);
                    break;

                case "help":
                    printConsoleHelp();
                    break;

                case "exit":
                    System.out.println("Exiting console. Server continues to run.");
                    System.out.println("Use Ctrl+C to stop the server.");
                    scanner.close();
                    return;

                case "":
                    break;

                default:
                    System.out.println("Unknown command. Type 'help' for available commands.");
            }
        }
    }

    private static void printServerStatus() {
        if (server != null) {
            System.out.println("\n=== Server Status ===");
            System.out.println("Connected Players: " + server.getConnectedPlayerCount());
            System.out.println("Active Sessions: " + server.getActiveSessionCount());
            System.out.println("Waiting Sessions: " + server.getWaitingSessionCount());
            System.out.println("===================\n");
        } else {
            System.out.println("Server not initialized");
        }
    }

    private static void printConnectedPlayers() {
        System.out.println("\n=== Connected Players ===");
        // Note: In a real implementation, you would access the server's player list
        System.out.println("(Player list display not implemented in console)");
        System.out.println("=========================\n");
    }

    private static void printActiveSessions() {
        System.out.println("\n=== Active Sessions ===");
        // Note: In a real implementation, you would access the server's session list
        System.out.println("(Session list display not implemented in console)");
        System.out.println("======================\n");
    }

    private static void printConsoleHelp() {
        System.out.println("\nAvailable Console Commands:");
        System.out.println("  status    - Display server status information");
        System.out.println("  players   - List all connected players");
        System.out.println("  sessions  - List all active game sessions");
        System.out.println("  stop      - Gracefully shutdown the server");
        System.out.println("  help      - Display this help message");
        System.out.println("  exit      - Exit the console interface");
        System.out.println();
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar GobangGameServer.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -p, --port PORT        Server port (default: 12345)");
        System.out.println("  -m, --max-players NUM  Maximum players (default: 100)");
        System.out.println("  -h, --help             Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar GobangGameServer.jar");
        System.out.println("  java -jar GobangGameServer.jar -p 8080 -m 50");
        System.out.println();
    }
}