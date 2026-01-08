# Gobang Game Server

A dedicated server for the Gobang (Five-in-a-Row) multiplayer game.  
[![Java CI with Maven](https://github.com/QIU2014/GoBangGameServer/actions/workflows/maven.yml/badge.svg)](https://github.com/QIU2014/GoBangGameServer/actions/workflows/maven.yml)
![GitHub Release](https://img.shields.io/github/v/release/qiu2014/Gobanggameserver)
![GitHub License](https://img.shields.io/github/license/qiu2014/gobanggameserver)

## Features

- **Multiplayer Support**: Host and join game sessions
- **Session Management**: Create, join, and list available sessions
- **Real-time Communication**: Instant move and chat message delivery
- **Connection Management**: Automatic cleanup of disconnected players
- **Console Interface**: Administrative console for server management
- **Configurable**: Adjustable port, player limits, and game settings

## Requirements

- Java 17 or higher
- Maven (for building)

## Building

```bash
mvn clean package
