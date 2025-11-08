import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Chatserver {
    private final Set<String> usernames = new HashSet<>();
    private final Map<String, PrintWriter> clients = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final int port;

    public Chatserver(int port) {
        this.port = port;
    }

    public void start() {
        System.out.println("Chat Server running on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from: " + clientSocket.getInetAddress());
                executor.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private String username;
        private PrintWriter out;
        private BufferedReader in;
        private long lastActivityTime;

        ClientHandler(Socket socket) {
            this.socket = socket;
            this.lastActivityTime = System.currentTimeMillis();

            new Thread(() -> {
                while (!socket.isClosed()) {
                    if (System.currentTimeMillis() - lastActivityTime > 60000) {
                        System.out.println("Client timed out: " + username);
                        handleLogout();
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

                out.println("Welcome to Chat Server");
                out.println(
                        "Available commands: LOGIN <username>, MSG <message>, DM <username> <message>, WHO, PING, LOGOUT");

                String input;
                while ((input = in.readLine()) != null) {
                    lastActivityTime = System.currentTimeMillis();
                    String[] parts = input.split("\\s+", 2);
                    String command = parts[0].toUpperCase();

                    System.out.println("Received command: " + command + " from " +
                            (username != null ? username : "unknown"));

                    switch (command) {
                        case "LOGIN":
                            handleLogin(parts);
                            break;
                        case "MSG":
                            handleMessage(parts);
                            break;
                        case "DM":
                            handleDirectMessage(parts);
                            break;
                        case "WHO":
                            handleWho();
                            break;
                        case "PING":
                            handlePing();
                            break;
                        case "LOGOUT":
                            handleLogout();
                            return;
                        default:
                            out.println("ERR unknown-command");
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + (username != null ? username : "unknown"));
                handleLogout();
            }
        }

        private void handleDirectMessage(String[] parts) {
            if (!isLoggedIn())
                return;

            if (parts.length < 2) {
                out.println("ERR invalid-dm-format");
                return;
            }

            String[] dmParts = parts[1].split("\\s+", 2);
            if (dmParts.length < 2) {
                out.println("ERR invalid-dm-format");
                return;
            }

            String targetUser = dmParts[0];
            String message = dmParts[1];

            PrintWriter targetWriter = clients.get(targetUser);
            if (targetWriter != null) {
                targetWriter.println("DM from " + username + ": " + message);
                out.println("DM to " + targetUser + ": " + message);
                System.out.println("DM from " + username + " to " + targetUser + ": " + message);
            } else {
                out.println("ERR user-not-found");
            }
        }

        private void handlePing() {
            out.println("PONG");
            System.out.println("Sent PONG to " + (username != null ? username : "unknown"));
        }

        private void handleLogin(String[] parts) {
            if (username != null) {
                out.println("ERR already-logged-in");
                return;
            }

            if (parts.length < 2) {
                out.println("ERR missing-username");
                return;
            }

            String newUsername = parts[1].trim();
            synchronized (usernames) {
                if (usernames.contains(newUsername)) {
                    out.println("ERR username-taken");
                    System.out.println("Login failed - username taken: " + newUsername);
                    return;
                }
                username = newUsername;
                usernames.add(username);
                clients.put(username, out);
                out.println("OK");
                System.out.println("User logged in: " + username);
                broadcast("INFO " + username + " joined", username);
            }
        }

        private void handleMessage(String[] parts) {
            if (!isLoggedIn())
                return;

            if (parts.length < 2) {
                out.println("ERR missing-message");
                return;
            }

            String message = parts[1];
            System.out.println("Message from " + username + ": " + message);
            broadcast("MSG " + username + " " + message, null);
        }

        private void handleWho() {
            if (!isLoggedIn())
                return;

            System.out.println("WHO command from " + username);
            synchronized (usernames) {
                out.println("Connected users:");
                for (String user : usernames) {
                    out.println("USER " + user);
                }
            }
        }

        private void handleLogout() {
            if (username != null) {
                System.out.println("User logging out: " + username);
                clients.remove(username);
                usernames.remove(username);
                broadcast("INFO " + username + " disconnected", null);
                username = null;
            }
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }

        private boolean isLoggedIn() {
            if (username == null) {
                out.println("ERR not-logged-in");
                return false;
            }
            return true;
        }

        private void broadcast(String message, String excludeUser) {
            System.out.println("Broadcasting: " + message);
            clients.forEach((user, writer) -> {
                if (excludeUser == null || !user.equals(excludeUser)) {
                    writer.println(message);
                }
            });
        }
    }

    public static void main(String[] args) {
        int port = 4000;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        new Chatserver(port).start();
    }
}