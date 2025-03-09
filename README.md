# Multi-Client Java Server

A Java server that allows multiple clients to connect and broadcasts the count of current clients to all connected clients.

## Features

- Real-time updates of connected client count
- Thread-safe handling of multiple clients
- Configurable through environment variables
- Deployable to Render and other cloud platforms

## Local Development

### Prerequisites

- Java JDK 11 or higher
- Maven

### Building the Project

```bash
mvn clean package
```

This will create a JAR file in the `target` directory.

### Running the Server Locally

```bash
java -jar target/multi-client-server-1.0-SNAPSHOT.jar
```

### Running the Client

```bash
# Connect to local server
java -cp target/multi-client-server-1.0-SNAPSHOT.jar Client

# Connect to remote server
java -cp target/multi-client-server-1.0-SNAPSHOT.jar Client <server-host> <server-port>
```

## Deploying to Render

1. Create a new Web Service on Render
2. Connect your GitHub repository
3. Use the following settings:
   - **Environment**: Java
   - **Build Command**: `mvn clean package`
   - **Start Command**: `java -jar target/multi-client-server-1.0-SNAPSHOT.jar`
4. Add the following environment variable:
   - `PORT`: Render will automatically assign a port

### Project Structure
```
multi-client-server/
├── src/
│   └── main/
│       └── java/
│           ├── MultiClientServer.java
│           └── Client.java
├── pom.xml
└── README.md
```

## Connecting Clients to the Deployed Server

Once deployed, clients can connect to your server using:

```bash
java -cp multi-client-server-1.0-SNAPSHOT.jar Client <your-render-url> <port>
```

Note: If your application is deployed on Render's free tier, it may sleep after periods of inactivity.
