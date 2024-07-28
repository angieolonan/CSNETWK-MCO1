import java.io.*;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ClientSession extends Thread {
    private Socket clientSocket;
    private DataInputStream input;
    private DataOutputStream output;
    private static final Set<String> activeUsers = Collections.synchronizedSet(new HashSet<>());
    private boolean registered = false;

    public ClientSession(Socket socket) {
        this.clientSocket = socket;
        try {
            this.input = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            this.output = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Stream initialization error: " + e.getMessage());
        }
    }

    public void run() {
        try {
            String command;
            while (true) {
                command = this.input.readUTF();
                if (command == null || command.equalsIgnoreCase("/exit")) {
                    break;
                }
                processCommand(command);
            }
        } catch (EOFException e) {
            System.out.println("Client has disconnected.");
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void processCommand(String command) {
        try {
            if (command.startsWith("/register ")) {
                registerUser(command);
            } else if (!registered) {
                sendMessage("Error: You must register first using /register <username>");
            } else {
                if (command.startsWith("/store ")) {
                    storeFile(command);
                } else if (command.startsWith("/get ")) {
                    getFile(command);
                } else if (command.equals("/dir")) {
                    listFiles();
                } else {
                    sendMessage("Unknown command! Type \"/?\" for a list of commands.");
                }
            }
        } catch (IOException e) {
            System.out.println("Command processing error: " + e.getMessage());
        }
    }

    private void registerUser(String command) throws IOException {
        String username = command.substring(10).trim();
        synchronized (activeUsers) {
            if (!activeUsers.contains(username)) {
                activeUsers.add(username);
                sendMessage("Welcome, " + username + "!");
                sendMessage("USERNAME:" + username);
                registered = true;
            } else {
                sendMessage("Error: Registration failed. Handle or alias already exists.");
            }
            sendMessage("END_OF_RESPONSE"); // Signal end of response
        }
    }

    private void storeFile(String command) throws IOException {
        String filename = command.substring(7).trim();
        System.out.println("Initiating file upload...");
        sendMessage("START_OF_FILE");
        saveFile(filename);
        sendMessage("END_OF_FILE");
    }

    private void getFile(String command) throws IOException {
        String filename = command.substring(5).trim();
        File file = new File("./server_files", filename);
        if (file.exists() && file.isFile()) {
            System.out.println("Sending file to client...");
            sendMessage("START_OF_FILE");
            sendFile(filename);
            sendMessage("END_OF_FILE");
        } else {
            sendMessage("File not found on server.");
        }
    }

    private void listFiles() throws IOException {
        File directory = new File("./server_files");
        File[] files = directory.listFiles();
        if (files != null && files.length > 0) {
            StringBuilder fileList = new StringBuilder();
            for (File file : files) {
                if (file.isFile()) {
                    fileList.append(file.getName()).append(",");
                }
            }
            sendMessage(fileList.toString());
        } else {
            sendMessage("No files found in server directory.");
        }
    }

    private void sendFile(String filename) {
        File file = new File("./server_files", filename);
        if (file.exists()) {
            try (FileInputStream fileInput = new FileInputStream(file)) {
                long fileSize = file.length();
                output.writeLong(fileSize);
                output.flush();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInput.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.flush();
                System.out.println("File sent: " + filename);
            } catch (IOException e) {
                System.out.println("File sending error: " + e.getMessage());
            }
        } else {
            try {
                sendMessage("Error: File not found on server.");
            } catch (IOException e) {
                System.out.println("Error sending error message: " + e.getMessage());
            }
        }
    }

    private void saveFile(String filename) {
        File directory = new File("./server_files");
        if (!directory.exists()) {
            directory.mkdir();
        }
        File file = new File(directory, filename);
        try (FileOutputStream fileOutput = new FileOutputStream(file)) {
            long fileSize = this.input.readLong();
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytesRead = 0;
            while (totalBytesRead < fileSize && (bytesRead = input.read(buffer)) != -1) {
                fileOutput.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
            System.out.println("File received and saved: " + filename);
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }

    private void sendMessage(String message) throws IOException {
        output.writeUTF(message);
        output.flush();
    }

    public void closeConnection() {
        try {
            if (output != null) output.close();
            if (input != null) input.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            System.out.println("Client connection closed.");
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }
}
