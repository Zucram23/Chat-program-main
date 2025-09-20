package org.example;

public class MessageFactory {

    public Message createTextMessage(String clientId, String text) {
        return new Message(clientId, MessageType.TEXT, text);
    }

    public Message createEmojiMessage(String clientId, String emojiCode) {
        return new Message(clientId, MessageType.EMOJI, emojiCode);
    }

    public Message createLoginMessage(String clientId, String username) {
        return new Message(clientId, MessageType.LOGIN, username);
    }

    public Message createJoinRoomMessage(String clientId, String roomName) {
        return new Message(clientId, MessageType.JOIN_ROOM, roomName);
    }

    public Message createPrivateMessage(String clientId, String recipientAndMessage) {
        return new Message(clientId, MessageType.PRIVATE, recipientAndMessage);
    }

    public Message createFileTransferMessage(String clientId, String fileInfo) {
        return new Message(clientId, MessageType.FILE_TRANSFER, fileInfo);
    }

    // Parse en rå besked string til Message object
    public Message parseMessage(String rawMessage) {
        return new Message(rawMessage);
    }
}
