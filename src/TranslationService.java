import org.json.JSONObject;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class TranslationService {
    private final String mainServerAddress = "127.0.0.1";
    private final int mainServerPort = 1500;

    public String requestTranslation(String wordPL, String targetLang) {
        String translation = "ERROR";
        ServerSocket listener = null;
        Socket connectionSocket = null;

        try {
            listener = new ServerSocket(0);
            int clientPort = listener.getLocalPort();

            JSONObject request = new JSONObject();
            request.put("wordPL", wordPL);
            request.put("targetLang", targetLang);
            request.put("clientPort", clientPort);

            try (Socket mainSocket = new Socket(mainServerAddress, mainServerPort);
                 PrintWriter out = new PrintWriter(mainSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new java.io.InputStreamReader(mainSocket.getInputStream()))){

                out.println(request);
                String confirmation = in.readLine();
                System.out.println("Confirmation: " + confirmation);
                if (confirmation.contains("ERROR")) {
                    return translation;
                }
            }
            connectionSocket = listener.accept();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()))) {
                String responseJson = in.readLine();
                if (responseJson != null) {
                    JSONObject jsonResponse = new JSONObject(responseJson);
                    System.out.println("Response: " + jsonResponse);
                    translation = jsonResponse.optString("translation", "NOT_FOUND");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connectionSocket != null) {
                try {connectionSocket.close();} catch (IOException e) {e.printStackTrace();}
            }
            if (listener != null) {
                try {listener.close();} catch (IOException e) {e.printStackTrace();}
            }
        }
        return translation;
    }
}