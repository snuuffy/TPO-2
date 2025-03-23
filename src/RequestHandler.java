import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class RequestHandler implements Runnable {
    private final Socket socket;
    private final LanguageServer languageServer;

    public RequestHandler(Socket socket, LanguageServer languageServer) {
        this.socket = socket;
        this.languageServer = languageServer;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String request = in.readLine();
            if (request != null) {
                JSONObject json = new JSONObject(request);
                String wordPL = json.optString("wordPL", "");
                String clientAddress = json.optString("clientAddress", "");
                int clientPort = json.optInt("clientPort", 0);

                String translation = languageServer.fetchTranslation(wordPL);

                sendTTranslationToClient(clientAddress, clientPort, wordPL, translation);
            }
        } catch (IOException e) {
            System.err.println("Error handling request: ");
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: ");
                e.printStackTrace();
            }
        }
    }

    private void sendTTranslationToClient(String address, int port, String wordPL, String translation) {
        try (Socket clientSocket = new Socket(address, port);
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            JSONObject response = new JSONObject();
            response.put("wordPL", wordPL);
            response.put("translation", translation);

            out.println(response);
            System.out.println("Translation sent to client (" + address + ":" + port + "):" + response);
        } catch (IOException e) {
            System.err.println("Error sending translation to client (" + address + ":" + port + "): ");
            e.printStackTrace();
        }
    }
}