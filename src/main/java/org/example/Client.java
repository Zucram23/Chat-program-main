package org.example;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String hostname = "localhost";
        int port = 5001;

        try {
            Socket socket = new Socket(hostname, port);
            Scanner scanner = new Scanner(System.in);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Connected to " + hostname + ":" + port);

            Thread recieveThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = reader.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {

                }

            });
            recieveThread.start();

            String input;
            while ((input = scanner.nextLine()) != null) {
                writer.println(input);
            }


        } catch (IOException e) {

        }


    }

}
