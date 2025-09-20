package org.example;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

enum MessageType {
    Text, Emoji, FILE_TRANSFER, LOGIN, JOIN_ROOM, PRIVATE
}

public class Message {
    private String clientId;
    private MessageType messageType;
    private String timestamp;
    private String payload;

    public Message(String clientId, MessageType messageType, LocalDate timestamp, String payload) {
        this.clientId = clientId;
        this.messageType = messageType;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.payload = payload;
    }
    private void parseMessage(String rawMessage) {
        try {

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}