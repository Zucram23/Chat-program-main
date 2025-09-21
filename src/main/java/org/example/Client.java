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
                } catch (SocketException e) {
                    System.err.println("Connection lost");
                } catch (SocketTimeoutException e) {
                    System.err.println("Connection timeout");
                }
                catch (IOException e) {
                System.err.println("I/O error");
                }

            });
            recieveThread.start();

            String input;
            while ((input = scanner.nextLine()) != null) {
                    writer.println(input);
                    System.err.println("I/O error");
            }


        } catch (UnknownHostException e) {
            System.err.println("Cant find server at " + hostname);
        } catch (ConnectException e) {
            System.err.println("Cant connect to " + hostname + ":" + port);
        } catch (SocketException e) {
            System.err.println("Error happened while conencting");
        } catch (SocketTimeoutException e) {
            System.err.println("Socket timed out");
        } catch (IOException e) {

        }


    }

}
