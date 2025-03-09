import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MultiClientServer {
    // Logger for server logs
    private static final Logger LOGGER = Logger.getLogger(MultiClientServer.class.getName());
    
    // Set to keep track of all the connected clients' output streams
    private static final Set<PrintWriter> clientWriters = new HashSet<>();
    
    public static void main(String[] args) {
        // Get port from environment variable or use default
        int port = getPort();
        ExecutorService pool = Executors.newFixedThreadPool(10);
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.info("Server started on port " + port);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info("New client connected: " + clientSocket.getInetAddress().getHostAddress());
                
                // Create a new handler for this client and submit it to the thread pool
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                pool.execute(clientHandler);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error in the server: " + e.getMessage(), e);
        }
    }
    
    // Get port from environment variable or use default 8080
    private static int getPort() {
        String portStr = System.getenv("PORT");
        if (portStr != null && !portStr.isEmpty()) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid PORT environment variable, using default");
            }
        }
        return 8080; // Default port for Render
    }
    
    // Broadcast the current number of connected clients to all clients
    private static void broadcastClientCount() {
        synchronized (clientWriters) {
            int clientCount = clientWriters.size();
            LOGGER.info("Broadcasting client count: " + clientCount);
            
            for (PrintWriter writer : clientWriters) {
                writer.println("CURRENT_CLIENTS:" + clientCount);
                writer.flush();
            }
        }
    }
    
    // Broadcast a message to all clients except the sender
    private static void broadcastMessage(PrintWriter sender, String message, String senderAddress) {
        synchronized (clientWriters) {
            LOGGER.info("Broadcasting message from " + senderAddress + ": " + message);
            
            for (PrintWriter writer : clientWriters) {
                if (writer != sender) {  // Don't send back to the original sender
                    writer.println("MESSAGE:" + senderAddress + ":" + message);
                    writer.flush();
                }
            }
        }
    }
    
    // Class to handle each client connection
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientAddress;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.clientAddress = socket.getInetAddress().getHostAddress();
        }
        
        @Override
        public void run() {
            try {
                // Set up the input and output streams for this client
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                // Add this client's writer to the set of all writers
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }
                
                // Broadcast the updated client count to all connected clients
                broadcastClientCount();
                
                // Welcome message to the new client
                out.println("MESSAGE:Server:Welcome! There are " + clientWriters.size() + " clients connected.");
                
                // Main loop to process client messages
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    LOGGER.info("Received message from " + clientAddress + ": " + inputLine);
                    
                    // Echo the message to all other clients
                    broadcastMessage(out, inputLine, clientAddress);
                }
                
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error handling client: " + e.getMessage(), e);
            } finally {
                // When a client disconnects, remove them from the set and broadcast the updated count
                try {
                    if (out != null) {
                        synchronized (clientWriters) {
                            clientWriters.remove(out);
                        }
                    }
                    if (in != null) {
                        in.close();
                    }
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                    
                    // Broadcast the updated client count after a client disconnects
                    broadcastClientCount();
                    
                    // Broadcast that this client has left
                    broadcastMessage(null, "has disconnected", clientAddress);
                    
                    LOGGER.info("Client " + clientAddress + " disconnected");
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error closing client connection: " + e.getMessage(), e);
                }
            }
        }
    }
}