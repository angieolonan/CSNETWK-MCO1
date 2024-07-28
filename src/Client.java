import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class Client {

    private JFrame mainFrame;
    private JTextField inputField;
    private JTextArea outputArea;
    private JButton sendBtn;
    private Socket clientSocket;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private final String separatorLine = "----------------------------------------\n";
    private boolean connected = false;
    private boolean registered = false;
    private String username;

    public Client() {
        setupUI();
    }

    private void setupUI() {
        mainFrame = new JFrame("Client Application");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(600, 400);
        mainFrame.setLayout(new BorderLayout());

        inputField = new JTextField();
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        sendBtn = new JButton("Send");

        mainFrame.add(inputField, BorderLayout.NORTH);
        mainFrame.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        mainFrame.add(sendBtn, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> handleCommand());

        mainFrame.setVisible(true);
    }

    private void handleCommand() {
        try {
            String command = inputField.getText().trim();
            inputField.setText(""); // Clear the input field

            if (command.equals("/?")) {
                showHelp();
                return;
            }

            if (!connected && !command.startsWith("/join ")) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(mainFrame,
                        "Not connected to a server. Please connect first.",
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
                });
                return;
            }

            if (command.startsWith("/join ")) {
                String[] parts = command.split(" ");
                if (parts.length == 3) {
                    String host = parts[1];
                    int port = Integer.parseInt(parts[2]);
                    createConnect(host, port);
                } else {
                    appendToOutput("\nInvalid command. Usage: /join <server-ip> <port>\n" + separatorLine);
                }
                return;
            }

            if (!registered && (command.startsWith("/store ") || command.startsWith("/get ") || command.equals("/dir"))) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(mainFrame,
                        "You need to register first.",
                        "Registration Required",
                        JOptionPane.ERROR_MESSAGE);
                });
                return;
            }

            processCommand(command);
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                appendToOutput("\nIO Exception: " + e.getMessage() + "\n" + separatorLine);
            });
        }
    }

    private void createConnect(String host, int port) {
        try {
            clientSocket = new Socket(host, port);
            outputStream = new DataOutputStream(clientSocket.getOutputStream());
            inputStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            appendToOutput("Connection to the File Exchange Server is successful!");
            connected = true;
        } catch (IOException e) {
            appendToOutput("\nError: Connection to the Server has failed! Please check IP Address and Port Number.\n" + separatorLine);
        }
    }  

    private void disconnect() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                outputStream.writeUTF("/disconnect");
                outputStream.flush();
                clientSocket.close();
                appendToOutput("\nConnection closed. Thank you!.\n" + separatorLine);
                connected = false;
                registered = false;
            } else {
                appendToOutput("\nError: Disconnection failed. Please connect to the server first.\n" + separatorLine);
            }
        } catch (IOException e) {
            appendToOutput("\nError: Disconnection failed. Please connect to the server first.\n" + separatorLine);
        }
    }

    private void processCommand(String command) throws IOException {
        if (command.startsWith("/store ")) {
            storeFile(command);
        } else if (command.startsWith("/get ")) {
            getFile(command);
        } else if (command.equals("/leave")) {
            disconnect();
        } else if (command.equals("/dir")) {
            sendCommandToServer(command);
            listFile();
        } else if (command.startsWith("/register ")) {
            sendCommandToServer(command);
            handleRegistrationResponse(command);
        } else {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(mainFrame, "Error: Command not found.\n" + separatorLine);
            });
        }
    }

    private void storeFile(String command) throws IOException {
        String filename = command.substring(7).trim();
        File file = new File(filename);
        if (file.exists()) {
            sendCommandToServer(command);
    
            String serverSignal = inputStream.readUTF();
            if (serverSignal.equals("START_OF_FILE")) {
                uploadFile(filename);
                serverSignal = inputStream.readUTF();
                if (serverSignal.equals("END_OF_FILE")) {
                    String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                    appendToOutput("\n" + username + "<" + timestamp + ">: Uploaded " + filename + "\n" + separatorLine);
                }
            }
        } else {
            appendToOutput("File not found: " + filename + "\n" + separatorLine);
        }
    }

    private void getFile(String command) throws IOException {
        sendCommandToServer(command);

        String serverSignal = inputStream.readUTF();
        if (serverSignal.equals("START_OF_FILE")) {
            String filename = command.substring(5).trim();
            downloadFile(filename);
            serverSignal = inputStream.readUTF();
            if (serverSignal.equals("END_OF_FILE")) {
                appendToOutput("\nFile received from Server: " + filename + "\n" + separatorLine);
            }
        } else {
            appendToOutput("\nError: File not found in the server.\n" + separatorLine);
        }
    }

    private void sendCommandToServer(String command) throws IOException {
        if (connected) {
            outputStream.writeUTF(command);
            outputStream.flush();
        } else {
            appendToOutput("Error: Command parameters do not match or is not allowed.\n" + separatorLine);
        }
    }

    private void handleRegistrationResponse(String commandType) throws IOException {
        StringBuilder responseBuilder = new StringBuilder();
        String line;

        if (commandType.startsWith("/register ")) {
            while (!(line = inputStream.readUTF()).equals("END_OF_RESPONSE")) {
                responseBuilder.append(line).append("\n");
                if (line.startsWith("Welcome")) {
                    registered = true;
                }
                if (line.startsWith("USERNAME:")) {
                    username = line.substring(9);
                }
            }
        }
        SwingUtilities.invokeLater(() -> {
            appendToOutput("\n[Server] " + responseBuilder.toString() + separatorLine);
        });
    }

    private void uploadFile(String filename) {
        File file = new File(filename);
        try (FileInputStream fileIn = new FileInputStream(file)) {
            long fileSize = file.length();
            outputStream.writeLong(fileSize);
            outputStream.flush();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            appendToOutput("\nFile uploaded to server: " + filename + "\n" + separatorLine);
        } catch (IOException e) {
            appendToOutput("\nError uploading file: " + e.getMessage() + "\n" + separatorLine);
        }
    }

    private void downloadFile(String filename) throws IOException {
        File directory = new File("./client_downloads");
        if (!directory.exists()) {
            directory.mkdir();
        }
    
        File file = new File(directory, filename);
    
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            long fileSize = inputStream.readLong();
            long totalBytesRead = 0;
            byte[] buffer = new byte[4096];
            int bytesRead;
    
            while (totalBytesRead < fileSize) {
                bytesRead = inputStream.read(buffer, 0, Math.min(buffer.length, (int)(fileSize - totalBytesRead)));
                fileOut.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
    
            appendToOutput("\nFile downloaded and saved to " + file.getAbsolutePath() + "\n" + separatorLine);
        } catch (IOException e) {
            appendToOutput("\nError downloading file: " + e.getMessage() + "\n" + separatorLine);
        }
    }

    private void listFile() {
        try {
            String[] fileNames = inputStream.readUTF().split(",");
            for (String fileName : fileNames) {
                appendToOutput("\n[Server] " + fileName + "\n");
            }
        } catch (IOException e) {
            appendToOutput("\nError retrieving server file list");
        }
    }

    private void showHelp() {
        SwingUtilities.invokeLater(() -> {
            appendToOutput("\nAvailable commands:\n");
            appendToOutput("/join\n");
            appendToOutput("/leave\n");
            appendToOutput("/register\n");
            appendToOutput("/store\n");
            appendToOutput("/get\n");
            appendToOutput("/dir\n");
            
        });
    }

    private void appendToOutput(String message) {
        SwingUtilities.invokeLater(() -> {
            outputArea.append(message);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
