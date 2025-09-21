package org.example;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
    private Map<String, Runnable> commandMap;  // HashMap mapping
    private Map<MessageType, Runnable> protocolMap;  // HashMap for protocol messages
    private RoomManager roomManager;
    private Room currentRoom;
    private boolean isLoggedIn = false;
    private MessageFactory messageFactory;  // MessageFactory
    private Message currentMessage;  // Current message being processed

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        this.clientId = "Client-" + socket.getPort();
        this.roomManager = server.getRoomManager();
        this.messageFactory = new MessageFactory();  // Initialize MessageFactory

        initializeCommandMap();  // Initialize HashMap
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Failed to get streams for " + clientId + ": " + e.getMessage());
            sendMessage("Failed trying to establish connection, try again");
        }
    }

    // HashMap mapping initialization
    private void initializeCommandMap() {
        // Commands mapping
        commandMap = new HashMap<>();
        commandMap.put("/help", this::sendHelpMessage);
        commandMap.put("/rooms", this::listRooms);
        commandMap.put("/leave", this::leaveRoom);
        commandMap.put("/who", this::showWhoInRoom);
        commandMap.put("/quit", this::quitClient);
        commandMap.put("/exit", this::quitClient);

        // Protocol messages mapping
        protocolMap = new HashMap<>();
        protocolMap.put(MessageType.TEXT, () -> handleTextMessage(currentMessage));
        protocolMap.put(MessageType.EMOJI, () -> handleEmojiMessage(currentMessage));
        protocolMap.put(MessageType.PRIVATE, () -> handlePrivateMessage(currentMessage));
        protocolMap.put(MessageType.JOIN_ROOM, () -> handleJoinRoomMessage(currentMessage));
        protocolMap.put(MessageType.LOGIN, () -> handleLoginMessage(currentMessage));
        protocolMap.put(MessageType.FILE_TRANSFER, () -> handleFileTransferMessage(currentMessage));
    }

    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    // Send protocol message til klient
    public void sendProtocolMessage(Message message) {
        if (writer != null) {
            writer.println(message.toProtocolString());
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
            String inputUsername = reader.readLine();

            if (inputUsername == null || inputUsername.trim().isEmpty()) {
                username = clientId;
            } else {
                username = inputUsername.trim();
            }

            // Send LOGIN besked using MessageFactory
            Message loginMessage = messageFactory.createLoginMessage(clientId, username);
            System.out.println("Login: " + loginMessage.toProtocolString());
            isLoggedIn = true;

            sendMessage("Hello " + username + "! You are now connected to the chat server.");

            // Automatisk join Lobby using MessageFactory
            if (roomManager.joinRoom(this, "Lobby")) {
                this.currentRoom = roomManager.findRoomByName("Lobby");

                // Send JOIN_ROOM besked using MessageFactory
                Message joinMessage = messageFactory.createJoinRoomMessage(clientId, "Lobby");
                System.out.println("Join room: " + joinMessage.toProtocolString());

                sendMessage("You automatically joined the Lobby room!");
            }

            sendHelpMessage();

            String rawInput;
            while ((rawInput = reader.readLine()) != null) {
                processInput(rawInput);
            }

        } catch (SocketException e){
            System.err.println("socket error for client: "+clientId + e.getMessage());
            sendMessage("Network connection error happened. You will be disconnected");
        } catch (SocketTimeoutException e){
            System.err.println("socket timeout for client: "+clientId + e.getMessage());
            sendMessage("Connection timed out. You will be disconnected");
        } catch (IOException e) {
            System.err.println("Error with client " + clientId + ": " + e.getMessage());
            sendMessage("A communication error occured, shutting down connection");
        } finally {
            cleanup();
        }
    }

    private void processInput(String rawInput) {
        if (rawInput.startsWith("/")) {
            // Traditional command
            handleCommand(rawInput);
        } else if (rawInput.contains("|")) {
            // Protocol message
            handleProtocolMessage(rawInput);
        } else {
            // Plain text message - convert to protocol using MessageFactory
            handlePlainTextMessage(rawInput);
        }
    }

    private void handleProtocolMessage(String rawMessage) {
        try {
            currentMessage = messageFactory.parseMessage(rawMessage);  // Use MessageFactory
            System.out.println("Received protocol message: " + currentMessage.toProtocolString());

            // Use HashMap mapping instead of switch case
            Runnable protocolAction = protocolMap.get(currentMessage.getMessageType());
            if (protocolAction != null) {
                protocolAction.run();  // Execute mapped protocol handler
            } else {
                sendMessage("Unknown message type: " + currentMessage.getMessageType());
            }
        } catch (NumberFormatException e){
            System.err.println("Timestamp is used wrong");
            sendMessage("Timestamp is causing error");
        } catch (IllegalArgumentException e) {
            System.err.println("Wrong use of message.");
            sendMessage("Invalid message format. Use: ClientID|timestamp|type|payload");
        }
    }

    private void handlePlainTextMessage(String text) {
        if (currentRoom != null) {
            // Check if message is a file (ends with common file extensions)
            if (isFileMessage(text)) {
                // Treat as file transfer
                Message fileMessage = messageFactory.createFileTransferMessage(clientId, text);
                System.out.println("File transfer: " + fileMessage.toProtocolString());

                String formattedMessage = username + " shared file: " + text;
                currentRoom.broadcastToRoom(formattedMessage, this);
                sendMessage("[You shared file]: " + text);
            } else if (text.startsWith(":") && text.endsWith(":")) {
                // Treat as emoji message
                Message emojiMessage = messageFactory.createEmojiMessage(clientId, text);
                System.out.println("Emoji message: " + emojiMessage.toProtocolString());

                String emoji = convertEmojiCode(text);
                String formattedMessage = username + ": " + emoji;
                currentRoom.broadcastToRoom(formattedMessage, this);
                sendMessage("[You]: " + emoji);
            } else {
                // Treat as normal text
                Message textMessage = messageFactory.createTextMessage(clientId, text);
                System.out.println("Text message: " + textMessage.toProtocolString());

                String formattedMessage = username + ": " + text;
                currentRoom.broadcastToRoom(formattedMessage, this);
                sendMessage("[You]: " + text);
            }
        } else {
            sendMessage("❌ You are not in any room!");
            sendMessage("💡 Use /join <roomname> to join a room");
        }
    }
    private boolean isFileMessage(String text) {
        String[] fileExtensions = {".pdf", ".jpg", ".png", ".gif", ".txt", ".doc", ".docx", ".zip", ".mp3", ".mp4"};
        String lowerText = text.toLowerCase();
        for (String ext : fileExtensions) {
            if (lowerText.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private void handleTextMessage(Message message) {
        if (currentRoom != null) {
            String formattedMessage = getUsername() + ": " + message.getPayload();
            currentRoom.broadcastToRoom(formattedMessage, this);
        }
    }

    private void handleEmojiMessage(Message message) {
        if (currentRoom != null) {
            String emoji = convertEmojiCode(message.getPayload());
            String formattedMessage = getUsername() + ": " + emoji;
            currentRoom.broadcastToRoom(formattedMessage, this);
        }
    }

    private void handlePrivateMessage(Message message) {
        String[] parts = message.getPayload().split(" ", 2);
        if (parts.length == 2) {
            String recipientName = parts[0];
            String privateText = parts[1];

            ClientHandler recipient = server.findClientByUsername(recipientName);
            if (recipient != null) {
                // Check if it's a file being shared
                if (isFileMessage(privateText)) {
                    // File transfer via private message
                    recipient.sendMessage("[FILE from " + getUsername() + "]: " + privateText + " 📁");
                    sendMessage("[FILE sent to " + recipientName + "]: " + privateText + " 📁");
                } else {
                    // Regular private message with emoji conversion
                    String processedText = convertEmojiCode(privateText);
                    recipient.sendMessage("[PM from " + getUsername() + "]: " + processedText);
                    sendMessage("[PM to " + recipientName + "]: " + processedText);
                }
            } else {
                sendMessage("User '" + recipientName + "' not found.");
            }
        }
    }

    private void handleJoinRoomMessage(Message message) {
        joinRoom(message.getPayload());
    }

    private void handleLoginMessage(Message message) {
        sendMessage("Login processed for: " + message.getPayload());
    }

    private void handleFileTransferMessage(Message message) {
        String fileInfo = message.getPayload();

        if (currentRoom != null) {
            // Simulate file transfer (in real implementation, this would handle actual file data)
            String formattedMessage = getUsername() + " shared file: " + fileInfo + " 📁";
            currentRoom.broadcastToRoom(formattedMessage, this);
            sendMessage("File shared successfully: " + fileInfo);
        } else {
            sendMessage("Cannot share file - you are not in any room!");
        }
    }


    private String convertEmojiCode(String emojiCode) {
        switch (emojiCode.toLowerCase()) {
            case ":rocket:": return "🚀";
            case ":smile:": return "😊";
            case ":heart:": return "❤️";
            case ":thumbsup:": return "👍";
            case ":fire:": return "🔥";
            default: return emojiCode;
        }
    }

    // HashMap mapping command handler
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

        if (cmd.equals("/pm")) {
            if (parts.length > 1) {
                String[] pmParts = parts[1].split(" ", 2);
                if (pmParts.length == 2) {
                    // Use MessageFactory for private message
                    Message privateMsg = messageFactory.createPrivateMessage(clientId, parts[1]);
                    System.out.println("Private message: " + privateMsg.toProtocolString());
                    handlePrivateMessage(privateMsg);
                } else {
                    sendMessage("Usage: /pm <username> <message>");
                }
            } else {
                sendMessage("Usage: /pm <username> <message>");
            }
            return;
        }

        // Use HashMap mapping for simple commands
        Runnable commandAction = commandMap.get(cmd);
        if (commandAction != null) {
            commandAction.run();  // Execute mapped command
        } else {
            sendMessage("Unknown command: " + cmd);
            sendMessage("Type /help for available commands.");
        }
    }

    private void sendHelpMessage() {
        sendMessage("=== CHAT COMMANDS ===");
        sendMessage("/join <room>     - Join a room: testRoom1, testRoom2, testRoom3, testRoom4)");
        sendMessage("/leave           - Leave current room");
        sendMessage("/rooms           - List all rooms");
        sendMessage("/who             - Show users in current room");
        sendMessage("/pm <user> <msg> - Send private message");
        sendMessage("/help            - Show this help");
        sendMessage("/quit            - Leave the chat");
        sendMessage("");
        sendMessage("=== PROTOCOL MESSAGES ===");
        sendMessage("ClientID|timestamp|TEXT|your message");
        sendMessage("ClientID|timestamp|EMOJI|:rocket:");
        sendMessage("ClientID|timestamp|PRIVATE|username message");
        sendMessage("ClientID|timestamp|FILE_TRANSFER|filename.pdf");
        sendMessage("");
        sendMessage("=== AUTO-DETECTION ===");
        sendMessage("document.pdf     - Auto-detected as file transfer");
        sendMessage(":smile:          - Auto-detected as emoji");
        sendMessage("normal text      - Auto-detected as text message");
    }

    private void listRooms() {
        sendMessage("=== AVAILABLE ROOMS ===");
        for (Room room : roomManager.getRooms()) {
            int occupants = room.howManyInroom();
            int maxCapacity = room.getMaxCapacity();
            String status = room.isRoomFull() ? " (FULL)" : "";
            sendMessage(room.getRoomName() + " (" + occupants + "/" + maxCapacity + ")" + status);
        }
    }

    private void leaveRoom() {
        if (currentRoom == null) {
            sendMessage("You are not in any room.");
            return;
        }

        String roomName = currentRoom.getRoomName();
        if (currentRoom.removeClient(this)) {
            sendMessage("You have left the room: " + roomName);
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

            // Log JOIN_ROOM protocol message using MessageFactory
            Message joinMessage = messageFactory.createJoinRoomMessage(clientId, roomName);
            System.out.println("Join room: " + joinMessage.toProtocolString());

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

        } catch (SocketException e) {
            System.err.println("Socket already closed or error while closing.");
        }
        catch (IOException e) {
          System.err.println("I/O error while closing.");
        }
    }

    private void cleanup() {
        try {
            if (currentRoom != null) {
                currentRoom.removeClient(this);
            }

            server.removeClient(this);

            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();

            System.out.println(username + " (" + clientId + ") disconnected");

        } catch (SocketException e) {
            System.err.println("Socket error during cleanup.");

        } catch (IOException e) {
            System.err.println("Error during cleanup for " + clientId + ": " + e.getMessage());
        }
    }
}