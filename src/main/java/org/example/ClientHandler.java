package org.example;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler implements Runnable {
    private Socket socket;
    private String clientId;
    private BufferedReader reader;
    private PrintWriter writer;
    private Server server;
    private String username;
    private Map<String, Runnable> commandMap;
    private RoomManager roomManager;
    private Room currentRoom;
    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.clientId = "Client-"+socket.getPort();
        this.roomManager = server.getRoomManager();

        initializeCommandMap();
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (
                IOException e) {
            System.err.println("Failed to get streams for " + clientId + ": " + e.getMessage());
        }
    }
    private void initializeCommandMap(){
        commandMap = new HashMap<>();
        commandMap.put("/help", this::sendHelpMessage);
        commandMap.put("/rooms", this::listRooms);
        commandMap.put("/leave", this::leaveRoom);
        commandMap.put("/who", this::showWhoInRoom);
        commandMap.put("/quit", this::quitClient);
        commandMap.put("/exit", this::quitClient);

    }

    public void sendMessage(String message) {
        if (writer != null){
            writer.println(message);
        }
    }

    public String getUsername() {
        return username != null ? username : clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(Room room) {
        this.currentRoom = room;
    }
    @Override
    public void run() {
        try {
            server.addClient(this);

            sendMessage("Welcome! Please enter your username: ");
            username = reader.readLine();

            if (username == null || username.trim().isEmpty()) {
                username = clientId;
            }

            sendMessage("Hello " + username + "! You are now connected to the chat server.");

            // Automatisk join Lobby når bruger forbinder
            if (roomManager.joinRoom(this, "Lobby")) {
                this.currentRoom = roomManager.findRoomByName("Lobby");
                sendMessage("You automatically joined the Lobby room!");
            }

            sendHelpMessage();

            String message;
            while ((message = reader.readLine()) != null) {
                if (message.startsWith("/")) {
                    handleCommand(message);
                } else {
                    handleChatMessage(message);
                }
            }

        } catch (IOException e) {
            System.err.println("Error with client " + clientId + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    private void handleCommand(String command) {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0].toLowerCase();

        // Special handling for commands with parameters
        if (cmd.equals("/join")) {
            if (parts.length > 1) {
                joinRoom(parts[1]);
            } else {
                sendMessage("Usage: /join <roomname>");
            }
            return;
        }

        // Use command map for simple commands
        Runnable commandAction = commandMap.get(cmd);
        if (commandAction != null) {
            commandAction.run();
        } else {
            sendMessage("Unknown command: " + cmd);
            sendMessage("Type /help for available commands.");
        }
    }
    private void handleChatMessage(String message) {
        if (currentRoom != null) {
            String formattedMessage = username + ": " + message;

            // Log til server konsol
            System.out.println("[" + currentRoom.getRoomName() + "] " + formattedMessage);

            currentRoom.broadcastToRoom(formattedMessage, this);
            sendMessage("[You]: " + message);
        } else {
            sendMessage(" You are not in any room!");
            sendMessage(" Use /join <roomname> to join a room");
            sendMessage(" Available rooms: /rooms");
        }
    }
    private void sendHelpMessage() {
        sendMessage("=== CHAT COMMANDS ===\n" +
                "/join <room>  - Join a room (Lobby, Gaming, Study, Random, VIP)\n" +
                "/leave        - Leave current room\n" +
                "/rooms        - List all rooms\n" +
                "/who          - Show users in current room\n" +
                "/help         - Show this message again\n" +
                "/quit         - Leave the chat");
    }

    private void listRooms(){
        sendMessage("-----AVAILABLE ROOMS------");
        for (Room room : roomManager.getAllRooms()) {
            int occupants= room.howManyInroom();
            sendMessage(room.getRoomName()+" "+occupants+"/"+room.getMaxCapacity());
        }
    }

    private void leaveRoom() {
        if (currentRoom==null) {
            sendMessage("You are not in a room");
            return;
        }
        String roomName = currentRoom.getRoomName();
        if (currentRoom.removeClient(this)){
            sendMessage("You have left the room "+roomName);
            currentRoom = null;
        }
    }
    private void showWhoInRoom() {
        if (currentRoom != null) {
            sendMessage("=== USERS IN " + currentRoom.getRoomName().toUpperCase() + " ===");
            for (String name : currentRoom.clientNamesInRoom()) {
                sendMessage("- " + name);
            }
        } else {
            sendMessage("You are not in any room.");
        }
    }
    private void joinRoom(String roomName) {
        if (roomManager.joinRoom(this, roomName)) {
            Room room = roomManager.findRoomByName(roomName);
            this.currentRoom = room;
            sendMessage("You joined room: " + roomName);
        } else {
            Room room = roomManager.findRoomByName(roomName);
            if (room == null) {
                sendMessage("Room '" + roomName + "' does not exist.");
                sendMessage("Available rooms: Lobby, Gaming, Study, Random, VIP");
            } else if (room.isRoomFull()) {
                sendMessage("Room '" + roomName + "' is full!");
            } else {
                sendMessage("Could not join room: " + roomName);
            }
        }
    }
    private void quitClient() {
        sendMessage("Goodbye!");
        try {
            socket.close();
        } catch (IOException e) {
            // Handle quietly
        }
    }
    private void cleanup() {
        try {
            // Leave current room
            if (currentRoom != null) {
                currentRoom.removeClient(this);
            }

            // Remove from server
            server.removeClient(this);

            // Close streams and socket
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();

            System.out.println(username + " (" + clientId + ") disconnected");

        } catch (IOException e) {
            System.err.println("Error during cleanup for " + clientId + ": " + e.getMessage());
        }
    }

}
