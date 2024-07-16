import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

// DEVELOPER'S NOTE : Code is untested

public class Client {
    private Socket clientSocket = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private ArrayList<String> users;


    //                               0         1          2           3        4       5      6      7
    ArrayList<String> keywords = {"/join", "/leave", "/register", "/store", "/dir", "/get", "/?", "/end"};
    // server address and port is 127.0.0.1 12345 

    // DEVELOPER'S NOTE : I'm not yet sure if this is correct and proper, I think I need to research more on this
    public Client (String address, int portNum) {
        this.address = address;
        this.portNum = portNum;
        this.clientSocket = new Socket();
        this.in = new DataInputStream();
        this.out = new DataOutputStream();
    }

    // basic UI of the client
    public void UI () {
        Scanner sc = new Scanner(System.in);
        boolean check = false;

        while (true) {
            System.out.print("\nEnter command: ");

            String command = sc.nextLine();

            String comSplit[] = command.split(" ");

            // outputs error message for checkCommands function
            if (!checkCommands(comSplit[0])) {
                System.out.println("\nERROR: Invalid Command");
                continue;
            }

            // /join 
            if (comSplit[0].equals(keywords[0])) {
                if (comSplit[1] != null && comSplit[2] != null) {
                    String address = comSplit[1];
                    int portNum = ((int)comSplit[2]);
                    check = this.createConnect(address, portNum, clientSocket); // address remains as String, port is typecasted into int
                }
                else {
                    System.out.println("\nERROR: Command parameters are incorrect/incomplete");
                }
                continue;
            }

            // prevents the client from running the other commands if not connected to server
            if (!check) {
                System.out.println("\nERROR: Client not connected to server. Please connect first before continuing");
                continue;
            }
            else {
                // /leave
                if (comSplit[0].equals(keywords[1])) {
                    this.disConnect();
                    continue;
                }

                // /register
                if (comSplit[0].equals(keywords[2])) {
                    if (comSplit[1] != null && comSplit[2] == null) {
                        this.registerUser(comSplit[1]);
                    }
                    else {
                        System.out.println ("\nERROR: Command parameters are incorrect/incomplete");
                    }
                    continue;
                }
                
                // /store
                if (comSplit[0].equals(keywords[3])) {
                    if (comSplit.length == 2) {
                        this.storeFile(comSplit[1]);
                    }
                    else {
                        System.out.println("\nERROR: Command parameters are incorrect/incomplete");
                    }
                    continue;
                }

                // /dir
                if (comSplit[0].equals(keywords[4])) {
                    if (comSplit.length == 1) {
                        this.listFiles();
                    } else {
                        System.out.println("\nERROR: Command does not have parameters");
                    }
                    continue;
                }

                // /get
                if (comSplit[0].equals(keywords[5])) {
                    if (comSplit.length == 2) {
                        this.getFile(comSplit[1]);
                    } else {
                        System.out.println("\nERROR: Command parameters are incorrect/incomplete");
                    }
                    continue;
                }
            }

            // /?
            if (comSplit[0].equals(keywords[6])) {
                if (comSplit[1] != null) {
                    System.out.println ("\nERROR: Command does not have parameters");
                }
                else {
                    this.dispCommands();
                }
                continue;
            }

            // /end
            // ends the program immediately, unless the client is still connected to the server
            // DEVELOPER'S NOTE : I added this command, not sure if it's needed tho
            if (comSplit[0].equals(keywords[7])) {
                if (comSplit[1] != null) {
                    System.out.println ("\nERROR: Command does not have parameters");
                }
                else if (createConnect(address, portNum, clientSocket) && !(disConnect())) {
                    System.out.println("\nERROR: Cannot end program. Disconnect from server first");
                }
                else {
                    System.out.println("\nEnding program, goodbye!");
                    break;
                }
                continue;
            }
        }
    }

    // checks if the given command is within the list of commands
    public boolean checkCommands (String command) {
        boolean result = false;
        for (String k : keywords) {
            if (command.equals(k)) {
                result  = true;
                break;
            }
        }
        return result;
    }

    // creates the connection between client and server
    // returns true if connection has been made, otherwise false
    public boolean createConnect (String address, int portNum, Socket clientSocket) {
        boolean result = false;
        try {
            InetAddress ipAddress = InetAddress.getByName(address);

            clientSocket.connect(ipAddress, portNum);

            System.out.println("\nConnection Successful!");

            result = true;
        }
        catch (UnknownHostException u) {
            System.out.println("\nERROR: Cannot find server. Please check the IP address and port number");
        }
        return result;
    }

    // disconnects the client from the server
    // returns true if disconnection was successful, otherwise false
    // DEVELOPER'S NOTE : I don't know if the catch is even needed, just added it for the try function to work
    public boolean disConnect () {
        boolean result = false;

        try {
            in.close();
            out.close();
            clientSocket.close();

            System.out.println("\nDisconnection Successful!");

            check = false;
            result = true;
        }
        catch (IOException i) {
            System.out.println("\nERROR: Cannot disconnect. Unresolved issues");
        }

        return result;
    }

    // registers the user 
    // returns true if registration was successful, otherwise false;
    public boolean registerUser (String name) {
        boolean result = false;
        if (!(isNumber(name))) {
            for (String u : users) {
                if (name.equals(u)) {
                    System.out.println("\nERROR: Cannot register. Username has already been taken");
                    break;
                }
                else {
                    System.out.println("Welcome " + name + "!");
                    this.users.add(name);
                    result = true;
                }
            }
        }
        else {
            System.out.println("\nERROR: Username must not be a number");
        }
        return result;
    }

    // parses the string into a number
    // returns true if the string is successful, otherwise returns false
    public boolean isNumber (String name) {
        boolean result = false;
        try {
            Double num = Double.parseDouble(name);
        }
        catch (NumberFormatException e) {
            result = true;
        }
        return result;
    }

    /**
     * Stores a file on the server.
     *
     * @param  fileName  the name of the file to store
     */
    public boolean storeFile (String fileName) {
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                System.out.println("\nError: File not found.");
                return false;
            }

            FileInputStream fileInput = new FileInputStream(file);
            out.writeUTF("/store " + fileName);
            out.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while((bytesRead = fileInput.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            fileInput.close();
            System.out.println("\nFile stored successfully.");
            return true;
        } catch (IOException e){
            System.out.println("\nError: Unable to store file.");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * A method to list files from the server.
     */
    public void listFiles() {
        try {
            out.writeUTF("/dir");

            String fileList = in.readUTF();
            System.out.println("\nFiles on server:");
            System.out.println(fileList);
        } catch (IOException e) {
            System.out.println("\nERROR: Unablee to retrieve file list.");
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a file from the server based on the provided file name.
     *
     * @param  fileName  the name of the file to retrieve
     */
    public boolean getFile (String fileName) {
        try {
            out.writeUTF("/get " + fileName);
            long fileSize = in.readLong();

            if(fileSize == -1) {
                System.out.println("\nERROR: File not found on server.");
                return false;
            }

            File file = new File(fileName);
            FileOutputStream fileOutput = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while(fileSize > 0 && (bytesRead = in.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                fileOutput.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
            }

            fileOutput.close();
            System.out.println("\nFile receive successfully.");
            return true;
        } catch(IOException e){
            System.out.println("\nERROR: Unable to fetch file.");
            e.printStackTrace();
            return false;
        }
    }

    // displays the list of available commands
    public void dispCommands () {
        System.out.println("\nList of commands:");
        for (String k : keywords) {
            System.out.println(k);
        }
    }

    public static void main (String args[]) {
        Client client = new Client ();

        client.UI();
    }
}
