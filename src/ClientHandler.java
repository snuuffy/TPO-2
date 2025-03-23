import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class ClientHandler implements Runnable {
    private final Socket socket;

    private final Map<String, ServerInfo> languageServers;

    public ClientHandler(Socket socket, Map<String, ServerInfo> languageServers) {
        this.socket = socket;
        this.languageServers = languageServers;
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String request = in.readLine();
            if (request != null) {
                JSONObject json = new JSONObject(request);
                if (json.has("langCode") && json.has("address") && json.has("port")){
                    String langCode = json.getString("langCode").toUpperCase();
                    String address = json.getString("address");
                    int port = json.getInt("port");

                    languageServers.put(langCode, new ServerInfo(address, port));
                    System.out.println("Registered language server " + langCode + " at " + address + ":" + port);

                    JSONObject response = new JSONObject();
                    response.put("status", "OK");
                    response.put("message", "Registered language server " + langCode + " at " + address + ":" + port);
                    out.println(response);
                }
                else if (json.has("wordPL") && json.has("targetLang") && json.has("clientPort")) {
                    String wordPL = json.getString("wordPL");
                    String targetLang = json.getString("targetLang").toUpperCase();
                    int clientPort = json.getInt("clientPort");
                    String clientAddress = socket.getInetAddress().getHostAddress();

                    if (!languageServers.containsKey(targetLang)) {
                        JSONObject error = new JSONObject();
                        error.put("status", "ERROR_UNSUPPORTED");
                        out.println(error);
                    } else {
                        ServerInfo serverInfo = languageServers.get(targetLang);
                        forwardTranslationRequest(serverInfo, wordPL, clientAddress, clientPort);
                        JSONObject response = new JSONObject();
                        response.put("status", "OK");
                        response.put("message", "Translation request forwarded to " + targetLang + " server");
                        out.println(response);
                    }
                } else {
                    JSONObject error = new JSONObject();
                    error.put("status", "ERROR_BAD_REQUEST");
                    out.println(error);

                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client request: ");
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: ");
                e.printStackTrace();
            }
        }
    }

    private void forwardTranslationRequest(ServerInfo serverInfo, String wordPL, String clientAddress, int clientPort) {
        try (Socket langSocket = new Socket(serverInfo.address(), serverInfo.port());
             PrintWriter out = new PrintWriter(langSocket.getOutputStream(), true)) {

            JSONObject requestToLangServer = new JSONObject();
            requestToLangServer.put("wordPL", wordPL);
            requestToLangServer.put("clientAddress", clientAddress);
            requestToLangServer.put("clientPort", clientPort);

            out.println(requestToLangServer);
            System.out.println("Forwarded translation request to " + serverInfo.address() + ":" + serverInfo.port());
        } catch (IOException e) {
            System.err.println("Error forwarding translation request to " + serverInfo.address() + ":" + serverInfo.port());
            e.printStackTrace();
        }
    }
}
