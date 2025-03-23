import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LanguageServer {
    private final String langCode;
    private final String mainServerAddress;
    private final int mainServerPort;
    private final int myPort;

    public LanguageServer(String langCode, String mainServerAddress, int mainServerPort, int myPort) {
        this.langCode = langCode;
        this.mainServerAddress = mainServerAddress;
        this.mainServerPort = mainServerPort;
        this.myPort = myPort;
    }

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: java LanguageServer <langCode> <mainServerAddress> <mainServerPort> <myPort>");
            return;
        }

        String langCode = args[0];
        String mainServerAddress = args[1];
        int mainServerPort = Integer.parseInt(args[2]);
        int myPort = Integer.parseInt(args[3]);

        LanguageServer server = new LanguageServer(langCode, mainServerAddress, mainServerPort, myPort);
        server.start();
    }

    public void start() {
        registerAtMainServer();

        try (ServerSocket serverSocket = new ServerSocket(myPort)) {
            System.out.println("Language server started on port " + myPort);

            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new RequestHandler(socket, this)).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting language server at port " + myPort);
            e.printStackTrace();
        }
    }

    private void registerAtMainServer() {
        try (Socket socket = new Socket(mainServerAddress, mainServerPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            JSONObject reg = new JSONObject();
            reg.put("langCode", langCode);
            reg.put("address", "127.0.0.1");
            reg.put("port", myPort);

            out.println(reg);

            String response = in.readLine();
            if (response != null) {
                System.out.println("Registered at main server");
            }
        } catch (IOException e) {
            System.err.println("Error registering at main server");
            e.printStackTrace();
        }
    }

    public String fetchTranslation(String word) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            JSONObject requestBody = new JSONObject();
            requestBody.put("q", word);
            requestBody.put("source", "pl");
            requestBody.put("target", langCode.toLowerCase());
            requestBody.put("format", "text");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:50000/translate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Translation response: " + response.body());
            JSONObject jsonResponse = new JSONObject(response.body());

            if (jsonResponse.has("translatedText")) {
                return jsonResponse.getString("translatedText");
            } else if (jsonResponse.has("error")) {
                System.err.println("LibreTranslate API error: " + jsonResponse.getString("error"));
                return "NOT_FOUND";
            } else {
                System.err.println("Unexpected API response: " + jsonResponse);
                return "NOT_FOUND";
            }

        } catch (Exception e) {
            System.err.println("Error fetching translation from LibreTranslate");
            e.printStackTrace();
            return "NOT_FOUND";
        }
    }

}
