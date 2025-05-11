import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CopyOnWriteArrayList;

public class Serverr {
    private static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static final ExecutorService connectionController = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        int port = 8000;
        int maxClients = 10;


        try (Scanner scanner = new Scanner(new File("utils/server_configs.txt"))) {
            port = Integer.parseInt(scanner.nextLine().split(" = ")[1]);
            maxClients = Integer.parseInt(scanner.nextLine().split(" = ")[1]);
        } catch (IOException e) {
            System.out.println("Error loading configuration: " + e.getMessage());
            System.out.println("Using default values - port: " + port + ", maxClients: " + maxClients);
        }


        startConnectionController();

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from: " + clientSocket.getInetAddress());

                if (clients.size() >= maxClients) {
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    out.println("Serwer osiągnął maksymalną liczbę połączeń. Spróbuj później.");
                    System.out.println("Odrzucono połączenie od " + clientSocket.getInetAddress() +
                            " - osiągnięto limit " + maxClients + " połączeń");
                    clientSocket.close();
                    continue;
                }

                configureSocket(clientSocket);
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);

                Thread clientThread = new Thread(clientHandler);
                clientThread.start();
            }
        } catch (Exception e) {
            System.out.println("Server error: " + e.getMessage());
        } finally {
            connectionController.shutdown();
        }
    }

    private static void configureSocket(Socket socket) {
        try {
            socket.setKeepAlive(true);
            socket.setSoTimeout(600000); // 30 sekund
            socket.setTcpNoDelay(true);
        } catch (IOException e) {
            System.out.println("Error configuring socket: " + e.getMessage());
        }
    }

    private static void startConnectionController() {
        connectionController.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    for (ClientHandler handler : clients) {
                        handler.checkConnection();
                    }
                    clients.removeIf(handler -> !handler.isConnected());

                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    System.out.println("Connection controller interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}