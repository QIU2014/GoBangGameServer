// src/main/java/com/eric/GobangGameServer/ServerConfig.java
package com.eric.GobangGameServer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration manager for the server
 */
public class ServerConfig {
    private static final String CONFIG_FILE = "server.properties";
    private final Properties properties;

    public ServerConfig() {
        properties = new Properties();
        loadDefaultConfig();
        loadConfigFile();
    }

    private void loadDefaultConfig() {
        properties.setProperty("server.port", "12345");
        properties.setProperty("server.max_players", "100");
        properties.setProperty("server.max_sessions", "50");
        properties.setProperty("server.timeout", "300"); // seconds
        properties.setProperty("server.heartbeat_interval", "30"); // seconds
        properties.setProperty("log.level", "INFO");
        properties.setProperty("log.file", "server.log");
        properties.setProperty("server.bind_address", "0.0.0.0"); // Bind to all interfaces
    }

    private void loadConfigFile() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
            System.out.println("Loaded configuration from " + CONFIG_FILE);
        } catch (IOException e) {
            System.out.println("Configuration file not found, using defaults");
            // Create default config file
            saveConfig();
        }
    }

    public void saveConfig() {
        try (FileOutputStream output = new FileOutputStream(CONFIG_FILE)) {
            properties.store(output, "Gobang Game Server Configuration");
            System.out.println("Configuration saved to " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("Failed to save configuration: " + e.getMessage());
        }
    }

    public int getPort() {
        return Integer.parseInt(properties.getProperty("server.port"));
    }

    public void setPort(int port) {
        properties.setProperty("server.port", String.valueOf(port));
    }

    public int getMaxPlayers() {
        return Integer.parseInt(properties.getProperty("server.max_players"));
    }

    public void setMaxPlayers(int maxPlayers) {
        properties.setProperty("server.max_players", String.valueOf(maxPlayers));
    }

    public int getMaxSessions() {
        return Integer.parseInt(properties.getProperty("server.max_sessions"));
    }

    public int getTimeout() {
        return Integer.parseInt(properties.getProperty("server.timeout"));
    }

    public int getHeartbeatInterval() {
        return Integer.parseInt(properties.getProperty("server.heartbeat_interval"));
    }

    public String getLogLevel() {
        return properties.getProperty("log.level");
    }

    public String getLogFile() {
        return properties.getProperty("log.file");
    }

    public String getBindAddress() {
        return properties.getProperty("server.bind_address");
    }

    public Properties getProperties() {
        return properties;
    }
}