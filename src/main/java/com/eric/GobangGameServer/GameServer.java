// src/main/java/com/eric/GobangGameServer/GameServer.java
package com.eric.GobangGameServer;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main server class that handles connections and game sessions
 */
public class GameServer {
    private static final Logger logger = LoggerFactory.getLogger(GameServer.class);

    private final int port;
    private final int maxPlayers;
    private volatile boolean isRunning = false;
    private ServerSocket serverSocket;
    private final ExecutorService clientExecutor;
    private final ExecutorService sessionExecutor;

    // Thread-safe collections for managing players and sessions
    private final ConcurrentHashMap<String, Player> connectedPlayers;
    private final ConcurrentHashMap<String, GameSession> activeSessions;
    private final ConcurrentHashMap<String, Player> waitingPlayers;

    // For generating unique IDs
    private final AtomicInteger playerIdCounter = new AtomicInteger(1);
    private final AtomicInteger sessionIdCounter = new AtomicInteger(1);

    public GameServer(int port, int maxPlayers) {
        this.port = port;
        this.maxPlayers = maxPlayers;
        this.clientExecutor = Executors.newCachedThreadPool();
        this.sessionExecutor = Executors.newFixedThreadPool(10);
        this.connectedPlayers = new ConcurrentHashMap<>();
        this.activeSessions = new ConcurrentHashMap<>();
        this.waitingPlayers = new ConcurrentHashMap<>();
    }

    public void start() {
        logger.info("Starting Gobang Game Server on port {}...", port);

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            isRunning = true;

            logger.info("Server started successfully. Waiting for connections...");

            // Start session monitor thread
            startSessionMonitor();

            // Main connection loop
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    if (connectedPlayers.size() >= maxPlayers) {
                        logger.warn("Max players reached. Rejecting connection from {}",
                                clientSocket.getInetAddress());
                        clientSocket.close();
                        continue;
                    }

                    // Handle client in a separate thread
                    clientExecutor.submit(() -> handleClientConnection(clientSocket));

                } catch (IOException e) {
                    if (isRunning) {
                        logger.error("Error accepting client connection: {}", e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            logger.error("Failed to start server on port {}: {}", port, e.getMessage());
            System.exit(1);
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        String playerId = "P" + playerIdCounter.getAndIncrement();
        Player player = null;

        try {
            logger.info("New connection from {}:{} assigned ID: {}",
                    clientSocket.getInetAddress(), clientSocket.getPort(), playerId);

            // First message should be player info
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));

            String initialMessage = input.readLine();
            if (initialMessage == null) {
                logger.warn("Client disconnected before sending initial message");
                clientSocket.close();
                return;
            }

            // Parse initial message: PLAYER_INFO:name:isHost
            String[] parts = initialMessage.split(":", 3);
            if (!parts[0].equals("PLAYER_INFO") || parts.length < 3) {
                logger.warn("Invalid initial message from client: {}", initialMessage);
                sendErrorMessage(clientSocket, "Invalid initial handshake");
                clientSocket.close();
                return;
            }

            String playerName = parts[1];
            boolean isHost = Boolean.parseBoolean(parts[2]);

            // Create player object
            player = new Player(playerId, playerName, clientSocket, isHost);
            connectedPlayers.put(playerId, player);

            // Send connection confirmation
            player.sendMessage("CONNECTED:" + playerId);

            logger.info("Player connected: {} (ID: {}, Host: {})",
                    playerName, playerId, isHost);

            // Handle player messages
            handlePlayerMessages(player);

        } catch (Exception e) {
            logger.error("Error handling client connection: {}", e.getMessage());
            if (player != null) {
                cleanupPlayer(player);
            } else {
                try {
                    clientSocket.close();
                } catch (IOException ex) {
                    // Ignore
                }
            }
        }
    }

    private void handlePlayerMessages(Player player) {
        try {
            BufferedReader input = player.getInputStream();
            String message;

            while (player.isActive() && (message = input.readLine()) != null) {
                logger.debug("Received from {}: {}", player.getPlayerId(), message);
                processClientMessage(player, message);
            }

        } catch (IOException e) {
            if (player.isActive()) {
                logger.error("Error reading from player {}: {}", player.getPlayerId(), e.getMessage());
            }
        } finally {
            cleanupPlayer(player);
        }
    }

    private void processClientMessage(Player player, String message) {
        String[] parts = message.split(":", 2);
        String command = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "CREATE_SESSION":
                handleCreateSession(player, data);
                break;

            case "JOIN_SESSION":
                handleJoinSession(player, data);
                break;

            case "MOVE":
                handleMove(player, data);
                break;

            case "CHAT":
                handleChat(player, data);
                break;

            case "RESTART":
                handleRestart(player);
                break;

            case "DISCONNECT":
                handleDisconnect(player);
                break;

            case "LIST_SESSIONS":
                handleListSessions(player);
                break;

            case "GET_PLAYER_INFO":
                handleGetPlayerInfo(player);
                break;

            default:
                logger.warn("Unknown command from player {}: {}", player.getPlayerId(), command);
                player.sendMessage("ERROR:Unknown command");
        }
    }

    private void handleCreateSession(Player player, String sessionName) {
        if (!player.isHost()) {
            player.sendMessage("ERROR:Only hosts can create sessions");
            return;
        }

        if (player.getCurrentSessionId() != null) {
            player.sendMessage("ERROR:Already in a session");
            return;
        }

        String sessionId = "S" + sessionIdCounter.getAndIncrement();
        GameSession session = new GameSession(sessionId, player, null);
        activeSessions.put(sessionId, session);

        player.setCurrentSessionId(sessionId);
        waitingPlayers.put(sessionId, player);

        player.sendMessage("SESSION_CREATED:" + sessionId + ":" + sessionName);
        logger.info("Session created: {} by player {}", sessionId, player.getPlayerId());

        // Broadcast session list update
        broadcastSessionList();
    }

    private void handleJoinSession(Player player, String sessionId) {
        if (player.getCurrentSessionId() != null) {
            player.sendMessage("ERROR:Already in a session");
            return;
        }

        if (!activeSessions.containsKey(sessionId)) {
            player.sendMessage("ERROR:Session not found");
            return;
        }

        GameSession session = activeSessions.get(sessionId);

        if (session.getPlayer2() != null) {
            player.sendMessage("ERROR:Session is full");
            return;
        }

        // Add player to session
        session.sendToPlayer(session.getPlayer1(), "PLAYER_JOINED:" + player.getPlayerName());
        player.setCurrentSessionId(sessionId);

        // Remove from waiting players
        waitingPlayers.remove(sessionId);

        // Update session with player2
        // Note: We need to modify GameSession to set player2
        // For now, we'll create a new session with both players
        GameSession newSession = new GameSession(sessionId, session.getPlayer1(), player);
        newSession.setGameStarted(true);
        activeSessions.put(sessionId, newSession);

        // Send start game messages to both players
        newSession.sendToPlayer(newSession.getPlayer1(),
                "GAME_START:black:" + player.getPlayerName() + ":white");
        newSession.sendToPlayer(newSession.getPlayer2(),
                "GAME_START:white:" + newSession.getPlayer1().getPlayerName() + ":black");

        logger.info("Player {} joined session {}", player.getPlayerId(), sessionId);

        // Broadcast session list update
        broadcastSessionList();
    }

    private void handleMove(Player player, String moveData) {
        String sessionId = player.getCurrentSessionId();
        if (sessionId == null) {
            player.sendMessage("ERROR:Not in a session");
            return;
        }

        GameSession session = activeSessions.get(sessionId);
        if (session == null) {
            player.sendMessage("ERROR:Session not found");
            return;
        }

        if (session.getCurrentTurn() != player) {
            player.sendMessage("ERROR:Not your turn");
            return;
        }

        // Forward move to opponent
        session.sendToOpponent(player, "MOVE:" + moveData);

        // Switch turn
        session.switchTurn();

        // Notify both players of turn change
        session.broadcast("TURN_CHANGE:" +
                (session.getCurrentTurn() == session.getPlayer1() ? "black" : "white"));

        logger.debug("Move processed in session {} by player {}", sessionId, player.getPlayerId());
    }

    private void handleChat(Player player, String chatMessage) {
        String sessionId = player.getCurrentSessionId();
        if (sessionId == null) {
            player.sendMessage("ERROR:Not in a session");
            return;
        }

        GameSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.sendToOpponent(player, "CHAT:" + player.getPlayerName() + ":" + chatMessage);
        }
    }

    private void handleRestart(Player player) {
        String sessionId = player.getCurrentSessionId();
        if (sessionId == null) {
            player.sendMessage("ERROR:Not in a session");
            return;
        }

        GameSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.sendToOpponent(player, "RESTART_REQUEST:" + player.getPlayerName());
        }
    }

    private void handleDisconnect(Player player) {
        cleanupPlayer(player);
    }

    private void handleListSessions(Player player) {
        StringBuilder sessionList = new StringBuilder("SESSION_LIST:");

        for (String sessionId : waitingPlayers.keySet()) {
            Player host = waitingPlayers.get(sessionId);
            sessionList.append(sessionId).append(",")
                    .append(host.getPlayerName()).append(";");
        }

        if (sessionList.length() > "SESSION_LIST:".length()) {
            sessionList.setLength(sessionList.length() - 1); // Remove last semicolon
        }

        player.sendMessage(sessionList.toString());
    }

    private void handleGetPlayerInfo(Player player) {
        player.sendMessage("PLAYER_INFO:" + player.getPlayerId() + ":" +
                player.getPlayerName() + ":" + player.isHost());
    }

    private void broadcastSessionList() {
        StringBuilder sessionList = new StringBuilder("SESSION_LIST_UPDATE:");

        for (String sessionId : waitingPlayers.keySet()) {
            Player host = waitingPlayers.get(sessionId);
            sessionList.append(sessionId).append(",")
                    .append(host.getPlayerName()).append(";");
        }

        if (sessionList.length() > "SESSION_LIST_UPDATE:".length()) {
            sessionList.setLength(sessionList.length() - 1);

            // Send to all connected players
            for (Player p : connectedPlayers.values()) {
                if (p.getCurrentSessionId() == null) { // Only players not in a game
                    p.sendMessage(sessionList.toString());
                }
            }
        }
    }

    private void cleanupPlayer(Player player) {
        String playerId = player.getPlayerId();
        String sessionId = player.getCurrentSessionId();

        // Remove from connected players
        connectedPlayers.remove(playerId);

        // Handle session cleanup if player was in a session
        if (sessionId != null) {
            GameSession session = activeSessions.get(sessionId);
            if (session != null) {
                session.removePlayer(playerId);

                // Notify opponent
                Player opponent = session.getOpponent(playerId);
                if (opponent != null) {
                    opponent.sendMessage("OPPONENT_DISCONNECTED");
                    opponent.setCurrentSessionId(null);
                }

                // Remove session if both players disconnected or game over
                if (!session.isActive() || session.isGameOver()) {
                    activeSessions.remove(sessionId);
                    waitingPlayers.remove(sessionId);
                    logger.info("Session {} removed", sessionId);
                }
            }
        }

        // Remove from waiting players
        waitingPlayers.values().removeIf(p -> p.getPlayerId().equals(playerId));

        // Disconnect player
        player.disconnect();

        // Broadcast updated session list
        broadcastSessionList();

        logger.info("Player cleanup completed for {}", playerId);
    }

    private void startSessionMonitor() {
        Thread monitorThread = new Thread(() -> {
            while (isRunning) {
                try {
                    TimeUnit.SECONDS.sleep(30);

                    // Clean up inactive sessions
                    activeSessions.entrySet().removeIf(entry -> {
                        GameSession session = entry.getValue();
                        if (!session.isActive() || session.isGameOver()) {
                            logger.info("Cleaning up inactive session: {}", entry.getKey());
                            return true;
                        }
                        return false;
                    });

                    // Clean up waiting sessions without players
                    waitingPlayers.entrySet().removeIf(entry -> {
                        Player player = entry.getValue();
                        if (!player.isActive()) {
                            logger.info("Cleaning up waiting session: {}", entry.getKey());
                            return true;
                        }
                        return false;
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private void sendErrorMessage(Socket socket, String message) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("ERROR:" + message);
        } catch (IOException e) {
            logger.error("Error sending error message: {}", e.getMessage());
        }
    }

    public void stop() {
        isRunning = false;

        logger.info("Shutting down server...");

        try {
            // Close server socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            // Disconnect all players
            for (Player player : connectedPlayers.values()) {
                player.disconnect();
            }

            // Shutdown executors
            clientExecutor.shutdown();
            sessionExecutor.shutdown();

            if (!clientExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                clientExecutor.shutdownNow();
            }

            if (!sessionExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                sessionExecutor.shutdownNow();
            }

            logger.info("Server shutdown complete");

        } catch (Exception e) {
            logger.error("Error during server shutdown: {}", e.getMessage());
        }
    }

    public int getConnectedPlayerCount() {
        return connectedPlayers.size();
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public int getWaitingSessionCount() {
        return waitingPlayers.size();
    }
}