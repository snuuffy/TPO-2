import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MainServer {
    private final int port;
    private final Map<String, ServerInfo> languageServers;

    public MainServer(int port) {
        this.port = port;
        this.languageServers = new ConcurrentHashMap<>();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java MainServer <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        MainServer server = new MainServer(port);
        server.start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Main server started on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket, languageServers)).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting main server at port " + port);
            e.printStackTrace();
        }
    }
}
