import java.io.*;
import java.net.*;

public class Chatclient {
    private static String currentUser = null;

    public static void main(String[] args) {
        String host = "localhost";
        int port = 4000;

        if (args.length >= 1)
            host = args[0];
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }

        try (
                Socket socket = new Socket(host, port);
                BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                PrintWriter serverOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"),
                        true);
                BufferedReader userIn = new BufferedReader(new InputStreamReader(System.in, "UTF-8"))) {

            Thread readerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = serverIn.readLine()) != null) {
                        if (currentUser != null && line.contains(currentUser)) {
                            continue;
                        }
                        if (line.equals("PONG")) {
                            continue;
                        }

                        if (line.equals("OK")) {
                            System.out.println("OK");
                            continue;
                        }

                        System.out.println(line);
                        System.out.print(">> ");
                        System.out.flush();
                    }
                } catch (IOException e) {
                    System.err.println("\n[Connection closed]");
                }
            });

            readerThread.setDaemon(true);
            readerThread.start();

            Thread heartbeatThread = new Thread(() -> {
                while (!socket.isClosed()) {
                    try {
                        Thread.sleep(30000);
                        serverOut.println("PING");
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            heartbeatThread.setDaemon(true);
            heartbeatThread.start();

            System.out.println(serverIn.readLine());
            System.out.println(serverIn.readLine());

            System.out.print(">> ");
            String input;
            while ((input = userIn.readLine()) != null) {
                if (input.startsWith("LOGIN ")) {
                    String[] parts = input.split("\\s+", 2);
                    if (parts.length > 1) {
                        currentUser = parts[1];
                    }
                }
                if (input.trim().equalsIgnoreCase("LOGOUT")) {
                    currentUser = null;
                    serverOut.println(input);
                    break;
                }

                serverOut.println(input);
                System.out.print(">> ");
            }

            System.out.println("Disconnected from chat.");
        } catch (ConnectException ce) {
            System.err.println("Could not connect to server: " + ce.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}