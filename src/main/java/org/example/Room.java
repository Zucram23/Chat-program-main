package org.example;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private String roomName;
    private List<ClientHandler> clients;
    private int maxCapacity;

    public Room(String roomName, int maxCapacity) {
        this.roomName = roomName;
        this.clients = new ArrayList<>();
        this.maxCapacity = maxCapacity;
    }

    public boolean addClient(ClientHandler client) {
        if (clients.size() >= maxCapacity) {
            return false;
        }

        if (!clients.contains(client)) {
            clients.add(client);
            broadcastToRoom("[" + client.getUsername() + " joined the room]", client);
            return true;
        }
        return false;
    }

    public boolean removeClient(ClientHandler client) {
        if (clients.remove(client)) {
            // Only broadcast if there are still clients in the room
            if (!clients.isEmpty()) {
                broadcastToRoom("[" + client.getUsername() + " left the room]", null);
            }
            return true;
        }
        return false;
    }

    public boolean isRoomFull() {
        return clients.size() >= maxCapacity;
    }

    public int howManyInroom() {
        return clients.size();
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public List<String> clientNamesInRoom() {
        List<String> names = new ArrayList<>();
        for (ClientHandler client : clients) {
            names.add(client.getUsername());
        }
        return names;
    }

    public void broadcastToRoom(String message, ClientHandler sender) {
        // Log til server
        System.out.println("Broadcasting to " + roomName + " (" + clients.size() + " clients): " + message);

        for (ClientHandler clientHandler : clients) {
            if (clientHandler != sender) { // Don't send to sender
                try {
                    clientHandler.sendMessage(message);
                } catch (Exception e) {
                    System.err.println("Error while messaging");
                }
            }
        }
    }

    public String getRoomName() {
        return roomName;
    }

    public boolean containsClient(ClientHandler client) {
        return clients.contains(client);
    }

}