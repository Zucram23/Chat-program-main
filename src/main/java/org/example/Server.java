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
        if (client==null){
            System.err.println("Client is null");
            return;
        }
            clients.remove(client);
            System.out.println("Client removed. Total clients: " + clients.size());

    }

    public ClientHandler findClientByUsername(String username) {
        if (username.isEmpty() || username.equals("")) {
            System.err.println("Username is empty or null");
            return null;
        }
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
        statusThread.setDaemon(true);
        statusThread.start();
    }

    public static void main(String[] args) {
        Server server = new Server();

        try {
            ServerSocket serverSocket = new ServerSocket(5001);
            System.out.println("Chat Server listening on port 5001");
            System.out.println("Available rooms: Lobby, testRoom1, testRoom2, testRoom3, testRoom4)");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, server);
                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        } catch (SocketException e) {
            System.err.println("Socket exception: " + e.getMessage());
        } catch (SocketTimeoutException e) {
            System.err.println("Socket timeout: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O exception: " + e.getMessage());
        }
    }
}
