import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    private static final Logger LOGGER = Logger.getLogger(Client.class.getName());
    
    public static void main(String[] args) {
        // Default to localhost if no arguments provided
        String serverIp = args.length > 0 ? args[0] : "localhost";
        // Default to 8080 if no port specified
        int serverPort = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
        
        LOGGER.info("Connecting to server at " + serverIp + ":" + serverPort);
        
        try {
            // Connect to the server
            Socket socket = new Socket(serverIp, serverPort);
            LOGGER.info("Connected to server!");
            
            // Set up reader to receive messages from server
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Set up writer to send messages to server
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            // Thread to handle server messages
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        if (serverMessage.startsWith("CURRENT_CLIENTS:")) {
                            int clientCount = Integer.parseInt(serverMessage.split(":")[1]);
                            System.out.println("Number of connected clients: " + clientCount);
                        } else if (serverMessage.startsWith("MESSAGE:")) {
                            // Extract the message parts
                            String[] parts = serverMessage.split(":", 3);
                            if (parts.length >= 3) {
                                String sender = parts[1];
                                String message = parts[2];
                                System.out.println(sender + ": " + message);
                            } else {
                                System.out.println("Received malformed message: " + serverMessage);
                            }
                        } else {
                            System.out.println("Server: " + serverMessage);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            }).start();
            
            // Thread to send user input to the server
            new Thread(() -> {
                try {
                    BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
                    System.out.println("Type your messages and press Enter to send. Type 'exit' to quit.");
                    
                    String message;
                    while ((message = userInput.readLine()) != null) {
                        if ("exit".equalsIgnoreCase(message)) {
                            break;
                        }
                        out.println(message);
                    }
                    
                    // Close the socket when the user wants to exit
                    socket.close();
                    System.exit(0);
                    
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error reading user input: " + e.getMessage(), e);
                }
            }).start();
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error connecting to server: " + e.getMessage(), e);
        }
    }
}