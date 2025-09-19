package org.example;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private List<ClientHandler> clients = new ArrayList<>();
    private RoomManager roomManager;

    public Server() {
        this.roomManager = new RoomManager();
        startStatusThread();
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public void addClient(ClientHandler client) {
        clients.add(client);
        System.out.println("Client added. Total clients: " + clients.size());
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client removed. Total clients: " + clients.size());
    }

    public ClientHandler findClientByUsername(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equalsIgnoreCase(username)) {
                return client;
            }
        }
        return null;
    }

    public void broadcastMessage(String message, ClientHandler sender) {
        System.out.println("Broadcasting message: " + message);
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    // Start a thread that prints server statistics every 5 minutes
    private void startStatusThread() {
        Thread statusThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(300000); // 5 minutes

                    // Print statistics
                    System.out.println("\n=== SERVER STATUS ===");
                    System.out.println("Total clients: " + clients.size());


                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        statusThread.setDaemon(true); // Daemon thread closes with main program
        statusThread.start();
    }

    public static void main(String[] args) {
        Server server = new Server();

        try {
            ServerSocket serverSocket = new ServerSocket(5001);
            System.out.println("Chat Server listening on port 5001");
            System.out.println("Available rooms: Lobby, Gaming, Study, Random, VIP");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, server);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
