// src/main/java/com/eric/GobangGameServer/Player.java
package com.eric.GobangGameServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a connected player
 */
public class Player {
    private static final Logger logger = LoggerFactory.getLogger(Player.class);

    private final String playerId;
    private final String playerName;
    private final Socket socket;
    private PrintWriter output;
    private BufferedReader input;
    private boolean isHost;
    private boolean isActive = true;
    private LocalDateTime connectedAt;
    private String currentSessionId;

    public Player(String playerId, String playerName, Socket socket, boolean isHost) throws IOException {
        this.playerId = playerId;
        this.playerName = playerName;
        this.socket = socket;
        this.isHost = isHost;
        this.connectedAt = LocalDateTime.now();

        // Initialize streams
        this.output = new PrintWriter(socket.getOutputStream(), true);
        this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Socket getSocket() {
        return socket;
    }

    public PrintWriter getOutputStream() {
        return output;
    }

    public BufferedReader getInputStream() {
        return input;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean isHost) {
        this.isHost = isHost;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }

    public String getCurrentSessionId() {
        return currentSessionId;
    }

    public void setCurrentSessionId(String sessionId) {
        this.currentSessionId = sessionId;
    }

    /**
     * Send a message to this player
     */
    public void sendMessage(String message) {
        if (output != null) {
            output.println(message);
            logger.debug("Sent to {}: {}", playerId, message);
        }
    }

    /**
     * Close connection and clean up resources
     */
    public void disconnect() {
        try {
            isActive = false;

            if (input != null) {
                input.close();
            }

            if (output != null) {
                output.close();
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            logger.info("Player {} disconnected", playerId);
        } catch (IOException e) {
            logger.error("Error disconnecting player {}: {}", playerId, e.getMessage());
        }
    }

    @Override
    public String toString() {
        return String.format("Player{id=%s, name=%s, host=%s, active=%s}",
                playerId, playerName, isHost, isActive);
    }
}