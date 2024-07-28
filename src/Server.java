import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

    private JFrame mainWindow;
    private JTextArea logOutput;
    private JButton toggleServerButton;
    private ServerSocket server;
    private boolean running = false;
    private final ArrayList<ClientSession> connectedClients = new ArrayList<>();

    public Server() {
        mainWindow = new JFrame("Server Application");
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainWindow.setSize(400, 300);
        mainWindow.setLayout(new BorderLayout());

        logOutput = new JTextArea();
        logOutput.setEditable(false);
        toggleServerButton = new JButton("Start Server");

        toggleServerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!running) {
                    initiateServer();
                } else {
                    terminateServer();
                }
            }
        });

        mainWindow.add(new JScrollPane(logOutput), BorderLayout.CENTER);
        mainWindow.add(toggleServerButton, BorderLayout.SOUTH);
        mainWindow.setVisible(true);
    }

    public void clientDisconnected(Socket socket) {
        SwingUtilities.invokeLater(() -> {
            logOutput.append("Client disconnected: " + socket.getInetAddress().getHostAddress() + "\n");
        });
    }

    private void initiateServer() {
        try {
            server = new ServerSocket(12345);
            running = true;
            toggleServerButton.setText("Stop Server");
            logOutput.append("Server started on port " + server.getLocalPort() + "\n");

            Thread serverThread = new Thread(() -> {
                try {
                    while (!server.isClosed()) {
                        Socket clientSocket = server.accept();
                        logOutput.append("Client connected: " + clientSocket.getInetAddress().getHostAddress() + "\n");

                        ClientSession clientSession = new ClientSession(clientSocket);
                        connectedClients.add(clientSession);
                        new Thread(clientSession).start();
                    }
                } catch (IOException e) {
                    logOutput.append("Server has stopped.\n");
                }
            });
            serverThread.start();

        } catch (IOException e) {
            logOutput.append("Error initiating server: " + e.getMessage() + "\n");
        }
    }

    private void terminateServer() {
        try {
            for (ClientSession clientSession : connectedClients) {
                clientSession.closeConnection();
            }
            connectedClients.clear();

            if (!server.isClosed()) {
                server.close();
            }
            running = false;
            toggleServerButton.setText("Start Server");
        } catch (IOException e) {
            logOutput.append("Error stopping server: " + e.getMessage() + "\n");
            System.exit(1); // Exit with an error status
        }
    }

    public void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            logOutput.append(message + "\n");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Server());
    }
}
