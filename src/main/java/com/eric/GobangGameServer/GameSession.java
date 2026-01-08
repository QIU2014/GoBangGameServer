package com.eric.GobangGameServer;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a single game session between two players
 */
public class GameSession {
    private static final Logger logger = LoggerFactory.getLogger(GameSession.class);

    private final String sessionId;
    private Player player1;
    private Player player2;
    private Player currentTurn;
    private boolean gameStarted = false;
    private boolean gameOver = false;
    private final ConcurrentHashMap<String, String> gameState;

    public GameSession(String sessionId, Player player1, Player player2) {
        this.sessionId = sessionId;
        this.player1 = player1;
        this.player2 = player2;
        this.currentTurn = player1; // Player1 (host) goes first
        this.gameState = new ConcurrentHashMap<>();
        initializeGameState();

        if (player1 != null) {
            player1.setCurrentSessionId(sessionId);
        }
        if (player2 != null) {
            player2.setCurrentSessionId(sessionId);
            this.gameStarted = true;
        }
    }

    private void initializeGameState() {
        gameState.put("board", "15x15");
        gameState.put("turn", "black");
        gameState.put("player1_color", "black");
        gameState.put("player2_color", "white");
    }

    public String getSessionId() {
        return sessionId;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
        if (player2 != null) {
            player2.setCurrentSessionId(sessionId);
            this.gameStarted = true;
        }
    }

    public Player getCurrentTurn() {
        return currentTurn;
    }

    public void switchTurn() {
        if (player1 != null && player2 != null) {
            currentTurn = (currentTurn == player1) ? player2 : player1;
            gameState.put("turn", getCurrentColor());
        }
    }

    private String getCurrentColor() {
        if (currentTurn == player1) {
            return "black";
        } else if (currentTurn == player2) {
            return "white";
        }
        return "black"; // default
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    /**
     * Send a message to both players in the session
     */
    public void broadcast(String message) {
        sendToPlayer(player1, message);
        sendToPlayer(player2, message);
    }

    /**
     * Send a message to a specific player
     */
    public void sendToPlayer(Player player, String message) {
        try {
            if (player != null && player.getOutputStream() != null) {
                PrintWriter out = player.getOutputStream();
                out.println(message);
                logger.debug("Sent to player {}: {}", player.getPlayerId(), message);
            }
        } catch (Exception e) {
            logger.error("Error sending message to player {}: {}",
                    player != null ? player.getPlayerId() : "null", e.getMessage());
        }
    }

    /**
     * Send a message to the opponent of a player
     */
    public void sendToOpponent(Player player, String message) {
        Player opponent = getOpponent(player.getPlayerId());
        sendToPlayer(opponent, message);
    }

    /**
     * Check if a player is in this session
     */
    public boolean containsPlayer(String playerId) {
        return (player1 != null && player1.getPlayerId().equals(playerId)) ||
                (player2 != null && player2.getPlayerId().equals(playerId));
    }

    /**
     * Get the opponent of a player
     */
    public Player getOpponent(String playerId) {
        if (player1 != null && player1.getPlayerId().equals(playerId)) {
            return player2;
        } else if (player2 != null && player2.getPlayerId().equals(playerId)) {
            return player1;
        }
        return null;
    }

    /**
     * Remove a player from the session (disconnect)
     */
    public void removePlayer(String playerId) {
        if (player1 != null && player1.getPlayerId().equals(playerId)) {
            player1.setActive(false);
            player1.setCurrentSessionId(null);
        } else if (player2 != null && player2.getPlayerId().equals(playerId)) {
            player2.setActive(false);
            player2.setCurrentSessionId(null);
        }
        gameOver = true;
    }

    /**
     * Get the player object by ID
     */
    public Player getPlayerById(String playerId) {
        if (player1 != null && player1.getPlayerId().equals(playerId)) {
            return player1;
        } else if (player2 != null && player2.getPlayerId().equals(playerId)) {
            return player2;
        }
        return null;
    }

    /**
     * Check if session is still active (both players connected)
     */
    public boolean isActive() {
        boolean player1Active = player1 != null && player1.isActive();
        boolean player2Active = player2 != null && player2.isActive();

        if (player2 == null) {
            // Waiting for second player
            return player1Active;
        }

        // Game in progress
        return player1Active && player2Active;
    }

    /**
     * Check if session has room for another player
     */
    public boolean hasRoom() {
        return player2 == null;
    }
}