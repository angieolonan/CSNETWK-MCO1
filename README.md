# File Exchange System
Machine Project for CSNETWK AY2023-2024
Submitted by: David Buban, Rain David, Angela Olonan, Ashley Ramos, and Vincent Tabuzo

## Project Description
The File Exchange System is designed as the final project for our CSNETWK course. It is a client-server application that allows users to store, share, and fetch files using either TCP or UDP protocols. The system comprises a server application and a client application. Users can interact with the server through the client application to perform various file operations.

## Client Application Specifications
1. The client application will function as the User Interface of a user when using the File Exchange System.
2. Input Commands:
   - Connect to the server application
     ```
     /join <server_ip_add> <port>
     ```
   - Disconnect from the server application
     ```
     /leave
     ```
   - Register a unique handle or alias
     ```
     /register <handle>
     ```
   - Send file to server
     ```
     /store <filename>
     ```
   - Request directory file list from a server
     ```
     /dir
     ```
   - Fetch a file from a server
     ```
     /get <filename>
     ```
   - Request command help to output all Input Syntax commands for references
     ```
     /?
     ```
