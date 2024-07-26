import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

// DEVELOPER'S NOTE : Code is tested

public class Client {
    private Socket clientSocket = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private ArrayList<String> users = new ArrayList<>();
    private boolean check = false; // Track connection status
    private String address;
    private int portNum;

    // List of keywords
    private String[] keywords = {"/join", "/leave", "/register", "/store", "/dir", "/get", "/?", "/end"};

    public Client(String address, int portNum) {
        this.address = address;
        this.portNum = portNum;
    }

    // Basic UI of the client
    public void UI() {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("\nEnter command: ");
            String command = sc.nextLine();
            String[] comSplit = command.split(" ");

            if (!checkCommands(comSplit[0])) {
                System.out.println("\nERROR: Invalid Command");
                continue;
            }

            switch (comSplit[0]) {
                case "/join":
                    if (comSplit.length == 3) {
                        address = comSplit[1];
                        portNum = Integer.parseInt(comSplit[2]);
                        check = createConnect(address, portNum);
                    } else {
                        System.out.println("\nERROR: Command parameters are incorrect/incomplete");
                    }
                    break;
                case "/leave":
                    if (check && comSplit.length == 1) {
                        disConnect();
                    } else {
                        System.out.println("\nERROR: Command does not have parameters or not connected to server.");
                    }
                    break;
                case "/register":
                    if (check && comSplit.length == 2) {
                        registerUser(comSplit[1]);
                    } else {
                        System.out.println("\nERROR: Command parameters are incorrect/incomplete or not connected to server.");
                    }
                    break;
                case "/store":
                    if (check && comSplit.length == 2) {
                        storeFile(comSplit[1]);
                    } else {
                        System.out.println("\nERROR: Command parameters are incorrect/incomplete or not connected to server.");
                    }
                    break;
                case "/dir":
                    if (check && comSplit.length == 1) {
                        listFiles();
                    } else {
                        System.out.println("\nERROR: Command does not have parameters or not connected to server.");
                    }
                    break;
                case "/get":
                    if (check && comSplit.length == 2) {
                        getFile(comSplit[1]);
                    } else {
                        System.out.println("\nERROR: Command parameters are incorrect/incomplete or not connected to server.");
                    }
                    break;
                case "/?":
                    dispCommands();
                    break;
                case "/end":
                    if (check) {
                        System.out.println("\nERROR: Cannot end program. Disconnect from server first.");
                    } else {
                        System.out.println("\nEnding program, goodbye!");
                        return; // Exit the loop and end the program
                    }
                    break;
                default:
                    System.out.println("\nERROR: Invalid Command");
            }
        }
    }

    // checks if the given command is within the list of commands
    public boolean checkCommands (String command) {
        for (String k : keywords) {
            if (command.equals(k)) {
                return true;
            }
        }
        return false;
    }

    // creates the connection between client and server
    // returns true if connection has been made, otherwise false
    public boolean createConnect (String address, int portNum) {
        try {
            clientSocket = new Socket(address, portNum);
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
            System.out.println("\nConnection Successful!");
            return true;
        } catch (IOException e) {
            System.out.println("\nERROR: Cannot find server. Please check the IP address and port number");
            return false;
        }
    }

    // disconnects the client from the server
    public void disConnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
            System.out.println("\nDisconnection Successful!");
            check = false;
        } catch (IOException e) {
            System.out.println("\nERROR: Cannot disconnect. Unresolved issues");
        }
    }

    // registers the user
    public void registerUser(String name) {
        try {
            out.writeUTF("/register " + name);
            String response = in.readUTF();
            if (response.startsWith("Error")) {
                System.out.println(response);
            } else {
                System.out.println(response);
            }
        } catch (IOException e) {
            System.out.println("\nERROR: Unable to register user");
        }
    }

    // Stores a file on the server.
    public void storeFile(String fileName) {
        File file = new File(fileName);
    
        if (file.exists() && file.isFile()) {
            try {
                out.writeUTF("/store " + fileName); // Send the command and file name
                out.writeLong(file.length()); // Send the file size
    
                byte[] buffer = new byte[1024 * 4]; // 4 KB buffer size
                int bytesRead;
                try (FileInputStream fileInput = new FileInputStream(file)) {
                    while ((bytesRead = fileInput.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead); // Send file data in chunks
                    }
                }
                out.flush(); // Ensure all data is sent
                System.out.println("\nUploaded " + fileName + " successfully!");
            } catch (IOException e) {
                System.out.println("\nERROR: Unable to store file.");
                e.printStackTrace();
            }
        } else {
            System.out.println("\nError: File not found.");
        }
    }
    
    

    // A method to list files from the server.
    public void listFiles() {
        try {
            out.writeUTF("/dir");
            String fileList = in.readUTF();
            System.out.println("\nFiles on server:");
            System.out.println(fileList);
        } catch (IOException e) {
            System.out.println("\nERROR: Unable to retrieve file list.");
        }
    }

    // Retrieves a file from the server based on the provided file name.
    public void getFile(String fileName) {
        try {
            out.writeUTF("/get " + fileName);
            long fileSize = in.readLong();
    
            if (fileSize > 0) {
                File file = new File(fileName);
                try (FileOutputStream fileOutput = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while (fileSize > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                        fileOutput.write(buffer, 0, bytesRead);
                        fileSize -= bytesRead;
                    }
                }
                System.out.println("\nFile retrieved successfully!");
            } else {
                System.out.println("\nERROR: File not found on server");
            }
        } catch (IOException e) {
            System.out.println("\nERROR: Unable to retrieve file");
        }
    }
    
    // displays the list of available commands
    public void dispCommands() {
        System.out.println("\nList of commands:");
        for (String k : keywords) {
            System.out.println(k);
        }
    }

    public static void main(String[] args) {
        // Example usage: java Client 127.0.0.1 12345
        if (args.length != 2) {
            System.out.println("Usage: java Client <server_ip> <port>");
            return;
        }

        String address = args[0];
        int portNum = Integer.parseInt(args[1]);

        Client client = new Client(address, portNum);
        client.UI();
    }
}