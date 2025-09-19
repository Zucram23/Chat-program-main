package org.example;

import java.util.ArrayList;
import java.util.List;

public class Room {
    String roomName;
    List<ClientHandler> clients;
    int maxCapacity;


    public Room(String roomName, int maxCapacity) {
        this.roomName = roomName;
        this.clients = new ArrayList<>();
        this.maxCapacity = maxCapacity;
    }

    public boolean addClient(ClientHandler client) {
        if (clients.size() >= maxCapacity){
            return false;
        }
        clients.add(client);
        broadcastToRoom("["+client.getUsername()+"] has joined the room!", client);
        return true;
    }

    public boolean removeClient(ClientHandler client) {
        if (clients.remove(client)) {
            broadcastToRoom("["+client.getUsername()+"] has left the room!", client);
            return true;
        }
        return false;
    }

    public boolean isRoomFull(){
        return clients.size() >= maxCapacity;
    }

    public int howManyInroom(){
        return clients.size();
    }
    public int getMaxCapacity() {
        return maxCapacity;
    }
    public List<String> clientNamesInRoom(){
        List<String> names = new ArrayList<>();
        for (ClientHandler client : clients) {
            names.add(client.getUsername());
        }
        return names;
    }

    public void broadcastToRoom(String message, ClientHandler sender) {
        for (ClientHandler clientHandler : clients) {
            //if (clientHandler != sender) {
            clientHandler.sendMessage(message);
            //}
        }
    }
    public String getRoomName() {
        return roomName;
    }

}
