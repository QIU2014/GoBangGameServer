// src/main/java/com/eric/GobangGameServer/MessageProtocol.java
package com.eric.GobangGameServer;

/**
 * Defines the message protocol between client and server
 */
public class MessageProtocol {

    // Client to Server messages
    public static final String CLIENT_CONNECT = "CONNECT";
    public static final String CLIENT_DISCONNECT = "DISCONNECT";
    public static final String CREATE_SESSION = "CREATE_SESSION";
    public static final String JOIN_SESSION = "JOIN_SESSION";
    public static final String MAKE_MOVE = "MOVE";
    public static final String SEND_CHAT = "CHAT";
    public static final String REQUEST_RESTART = "RESTART";
    public static final String LIST_SESSIONS = "LIST_SESSIONS";
    public static final String GET_PLAYER_INFO = "GET_PLAYER_INFO";

    // Server to Client messages
    public static final String SERVER_CONNECTED = "CONNECTED";
    public static final String SERVER_ERROR = "ERROR";
    public static final String SESSION_CREATED = "SESSION_CREATED";
    public static final String SESSION_JOINED = "SESSION_JOINED";
    public static final String GAME_START = "GAME_START";
    public static final String PLAYER_MOVED = "PLAYER_MOVED";
    public static final String TURN_CHANGE = "TURN_CHANGE";
    public static final String CHAT_MESSAGE = "CHAT";
    public static final String RESTART_REQUEST = "RESTART_REQUEST";
    public static final String RESTART_ACCEPTED = "RESTART_ACCEPTED";
    public static final String RESTART_REJECTED = "RESTART_REJECTED";
    public static final String SESSION_LIST = "SESSION_LIST";
    public static final String SESSION_LIST_UPDATE = "SESSION_LIST_UPDATE";
    public static final String PLAYER_INFO = "PLAYER_INFO";
    public static final String PLAYER_JOINED = "PLAYER_JOINED";
    public static final String OPPONENT_DISCONNECTED = "OPPONENT_DISCONNECTED";
    public static final String GAME_OVER = "GAME_OVER";

    // Message formats
    public static String formatConnectMessage(String playerName, boolean isHost) {
        return String.format("PLAYER_INFO:%s:%s", playerName, isHost);
    }

    public static String formatMoveMessage(int row, int col) {
        return String.format("MOVE:%d,%d", row, col);
    }

    public static String formatChatMessage(String sender, String message) {
        return String.format("CHAT:%s:%s", sender, message);
    }

    public static String formatGameStartMessage(String yourColor, String opponentName, String opponentColor) {
        return String.format("GAME_START:%s:%s:%s", yourColor, opponentName, opponentColor);
    }

    public static String parseMove(String moveMessage) {
        if (!moveMessage.startsWith("MOVE:")) {
            return null;
        }
        return moveMessage.substring(5);
    }
}