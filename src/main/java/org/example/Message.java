package org.example;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private String clientId;
    private MessageType messageType;
    private String timestamp;
    private String payload;

    public Message(String clientId, MessageType messageType, String payload) {
        this.clientId = clientId;
        this.messageType = messageType;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.payload = payload;
    }
    public Message(String rawMessage) {
        parseMessage(rawMessage);
    }

    private void parseMessage(String rawMessage) {
        try {
            String[] parts = rawMessage.split("\\|", 4); // Split på |, max 4 dele
            if (parts.length == 4) {
                this.clientId = parts[0].trim();
                this.timestamp = parts[1].trim();
                this.messageType = MessageType.valueOf(parts[2].trim());
                this.payload = parts[3].trim();
            } else {
                throw new IllegalArgumentException("Invalid message format");
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid message format");
            parseMessageErrorHandling(rawMessage);

        } catch (ArrayIndexOutOfBoundsException e) {
            parseMessageErrorHandling(rawMessage);
            System.err.println("Array index out of bounds");
        } catch (NullPointerException e) {
            parseMessageErrorHandling(rawMessage);
        }
    }
    private void parseMessageErrorHandling(String rawMessage) {
        System.err.println("Invalid message format");
        this.clientId = "unknown";
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.messageType = MessageType.TEXT;
        this.payload = rawMessage;
    }


    // Convert Message object til protocol string
    public String toProtocolString() {
        return clientId + "|" + timestamp + "|" + messageType + "|" + payload;
    }

    // Static factory methods fjernet - brug MessageFactory i stedet

    // Getters
    public String getClientId() { return clientId; }
    public String getTimestamp() { return timestamp; }
    public MessageType getMessageType() { return messageType; }
    public String getPayload() { return payload; }

    // Check om beskeden er af en bestemt type
    public boolean isTextMessage() { return messageType == MessageType.TEXT; }
    public boolean isEmojiMessage() { return messageType == MessageType.EMOJI; }
    public boolean isLoginMessage() { return messageType == MessageType.LOGIN; }
    public boolean isJoinRoomMessage() { return messageType == MessageType.JOIN_ROOM; }
    public boolean isPrivateMessage() { return messageType == MessageType.PRIVATE; }
    public boolean isFileTransferMessage() { return messageType == MessageType.FILE_TRANSFER; }

    @Override
    public String toString() {
        return "Message{" +
                "clientId='" + clientId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", messageType=" + messageType +
                ", payload='" + payload + '\'' +
                '}';
    }
}