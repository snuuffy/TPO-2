import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Client extends JFrame {

    private JTextField txtWordPL;
    private JTextField txtTargetLang;
    private JButton btnTranslate;
    private JTextArea txtResult;

    private final String mainServerAddress = "127.0.0.1";
    private final int mainServerPort = 1500;

    public Client() {
        setTitle("Dictionary Client");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(3, 2));
        inputPanel.add(new JLabel("Word in Polish:"));
        txtWordPL = new JTextField();
        inputPanel.add(txtWordPL);
        inputPanel.add(new JLabel("Target Language (e.g. EN)"));
        txtTargetLang = new JTextField();
        inputPanel.add(txtTargetLang);

        btnTranslate = new JButton("Translate");
        inputPanel.add(btnTranslate);

        add(inputPanel, BorderLayout.NORTH);

        txtResult = new JTextArea();
        txtResult.setEditable(false);
        add(new JScrollPane(txtResult), BorderLayout.CENTER);

        btnTranslate.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String word = txtWordPL.getText().trim();
                String targetLang = txtTargetLang.getText().trim();
                if (word.isEmpty() || targetLang.isEmpty()) {
                    JOptionPane.showMessageDialog(Client.this, "Please fill in all fields");
                    return;
                }
                txtResult.setText("Translating...");
                new Thread(() -> {
                    String translation = requestTranslation(word, targetLang);
                    SwingUtilities.invokeLater(() -> {
                        txtResult.setText("Translation: " + translation);
                    });
                }).start();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            client.setVisible(true);
        });
    }

    private String requestTranslation(String wordPL, String targetLang) {
        String translation = "NOT_FOUND";
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
            }
            connectionSocket = listener.accept();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()))) {
                String responseJson = in.readLine();
                if (responseJson != null) {
                    JSONObject jsonResponse = new JSONObject(responseJson);
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
