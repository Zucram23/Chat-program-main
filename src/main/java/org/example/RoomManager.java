package org.example;

import java.util.ArrayList;
import java.util.List;

public class RoomManager {

    private List<Room> rooms;

    public RoomManager() {
        this.rooms = new ArrayList<Room>();
        // Ændre til ordentlige room navne:
        rooms.add(new Room("Lobby", 5));
        rooms.add(new Room("testRoom1", 5));
        rooms.add(new Room("testRoom2", 5));
        rooms.add(new Room("testRoom3", 5));
        rooms.add(new Room("testRoom4", 5));
    }

    public Room findRoomByName(String roomName) {
        for(Room room : rooms) {
            if (room.getRoomName().equalsIgnoreCase(roomName)) {
                return room;
            }
        }
        return null;
    }

    public boolean joinRoom(ClientHandler client, String roomName) {
        Room room = findRoomByName(roomName);
        if (room == null){
            return false;
        }

        if (room.isRoomFull()) {
            return false;
        }

        leaveAllRooms(client);
        return room.addClient(client);
    }

    public boolean leaveRoom(ClientHandler client, String roomName) {
        Room room = findRoomByName(roomName);
        if (room != null) {
            return room.removeClient(client);
        }
        return false;
    }

    public void leaveAllRooms(ClientHandler client) {
        for (Room room : rooms) {
            room.removeClient(client);
        }
    }

    public List<String> getRoomNames() {
        List<String> names = new ArrayList<>();
        for (Room room : rooms) {
            names.add(room.getRoomName());
        }
        return names;
    }

    public List<Room> getAllRooms() {
        return rooms;
    }


    public List<Room> getRooms() {
        return rooms;
    }
}