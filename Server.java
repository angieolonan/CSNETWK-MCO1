import java.io.*;
import java.net.*;
import java.util.*;

// DEVELOPER'S NOTE : Code is untested
public class Server {
    private ServerSocket serverSocket;
    private HashMap<String, ClientHandler> clients = new HashMap<>();
    private HashMap<String, String> userFiles = new HashMap<>();
    private List<String> keywords = Arrays.asList("/join", "/leave", "/register", "/store", "/dir", "/get", "/?", "/end");

    public Server(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clientHandler.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void registerClient(String handle, ClientHandler clientHandler) {
        clients.put(handle, clientHandler);
    }

    public void removeClient(String handle) {
        clients.remove(handle);
    }

    public boolean isHandleTaken(String handle) {
        return clients.containsKey(handle);
    }

    public void storeFile(String handle, String filename, byte[] data) {
        userFiles.put(filename, handle);
        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getDirectory() {
        return userFiles.keySet();
    }

    public byte[] getFile(String filename) throws IOException {
        File file = new File(filename);
        byte[] data = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(data);
        }
        return data;
    }

    public boolean isValidCommand(String command) {
        return keywords.contains(command);
    }

    public static void main(String[] args) {
        int port = 12345;
        Server server = new Server(port);
        server.start();
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private Server server;
        private DataInputStream in;
        private DataOutputStream out;
        private String clientHandle = null;

        public ClientHandler(Socket socket, Server server) {
            this.clientSocket = socket;
            this.server = server;
            try {
                in = new DataInputStream(clientSocket.getInputStream());
                out = new DataOutputStream(clientSocket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String command = in.readUTF();
                    String[] commandSplit = command.split(" ");
                    String cmd = commandSplit[0];

                    if (!server.isValidCommand(cmd)) {
                        out.writeUTF("Error: Command not found.");
                        continue;
                    }

                    switch (cmd) {
                        case "/leave":
                            handleLeave();
                            return; // End the thread after leaving
                        case "/register":
                            handleRegister(commandSplit);
                            break;
                        case "/store":
                            handleStore(commandSplit);
                            break;
                        case "/dir":
                            handleDir();
                            break;
                        case "/get":
                            handleGet(commandSplit);
                            break;
                        case "/?":
                            handleHelp();
                            break;
                        case "/end":
                            handleEnd();
                            break;
                        default:
                            out.writeUTF("Error: Command not found");
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleLeave() throws IOException {
            out.writeUTF("Connection closed. Thank you!");
            server.removeClient(clientHandle);
            in.close();
            out.close();
            clientSocket.close();
        }

        private void handleRegister(String[] commandSplit) throws IOException {
            if (commandSplit.length != 2) {
                out.writeUTF("Error: Command parameters do not match or is not allowed.");
                return;
            }
            String handle = commandSplit[1];
            if (server.isHandleTaken(handle)) {
                out.writeUTF("Error: Registration failed. Handle or alias already exists.");
            } else {
                server.registerClient(handle, this);
                clientHandle = handle;
                out.writeUTF("Welcome " + handle + "!");
            }
        }

        private void handleStore(String[] commandSplit) throws IOException {
            if (commandSplit.length != 2) {
                out.writeUTF("Error: Command parameters do not match or is not allowed.");
                return;
            }
            String filename = commandSplit[1];
            byte[] data = new byte[in.readInt()];
            in.readFully(data);
            server.storeFile(clientHandle, filename, data);
            out.writeUTF(clientHandle + "<" + new Date() + ">: Uploaded " + filename);
        }

        private void handleDir() throws IOException {
            Set<String> files = server.getDirectory();
            if (files.isEmpty()) {
                out.writeUTF("Server Directory is empty.");
            } else {
                out.writeUTF("Server Directory\n" + String.join("\n", files));
            }
        }

        private void handleGet(String[] commandSplit) throws IOException {
            if (commandSplit.length != 2) {
                out.writeUTF("Error: Command parameters do not match or is not allowed.");
                return;
            }
            String filename = commandSplit[1];
            String owner = server.userFiles.get(filename);
            if (owner == null) {
                out.writeUTF("Error: File not found in the server.");
            } else {
                byte[] data = server.getFile(filename);
                out.writeInt(data.length);
                out.write(data);
                out.writeUTF("File received from Server: " + filename);
            }
        }

        private void handleHelp() throws IOException {
            out.writeUTF("List of commands:\n" + String.join("\n", server.keywords));
        }

        private void handleEnd() throws IOException {
            if (clientHandle != null) {
                handleLeave();
            }
            out.writeUTF("Ending program, goodbye!");
            System.exit(0);
        }
    }
}
