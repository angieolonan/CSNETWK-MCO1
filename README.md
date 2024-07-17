# File Exchange System
Machine Project for CSNETWK AY2023-2024
Submitted by: David Buban, Rain David, Angela Olonan, Ashley Ramos, and Vincent Tabuzo

## Project Description
The File Exchange System is designed as the final project for our CSNETWK course. It is a client-server application that allows users to store, share, and fetch files using either TCP or UDP protocols. The system comprises a server application and a client application. Users can interact with the server through the client application to perform various file operations.

## Client Application Specifications
1. The client application will function as the User Interface of a user when using the File Exchange System.
2. **Input Commands:**
   - Connect to the server application
     ```
     /join <server_ip_add> <port>
     ```
     e.g., `/join 192.168.1.1 12345`
   - Disconnect from the server application
     ```
     /leave
     ```
   - Register a unique handle or alias
     ```
     /register <handle>
     ```
     e.g., `/register User1`
   - Send file to server
     ```
     /store <filename>
     ```
     e.g., `/store Hello.txt`
   - Request directory file list from a server
     ```
     /dir
     ```
   - Fetch a file from a server
     ```
     /get <filename>
     ```
     e.g., `/get Hello.txt`
   - Request command help to output all Input Syntax commands for references
     ```
     /?
     ```
3. **Output Field:** The output field is included to display server status from other users as well as systemmessages from the interaction of the client and the server application.
5. **Error Message:** Error messages are outputted upon unsuccessful execution of commands.
