import javax.swing.*;
import java.awt.*;

public class Client extends JFrame {

    private JTextField txtWordPL;
    private JTextField txtTargetLang;
    private JButton btnTranslate;
    private JTextArea txtResult;

    private TranslationService translationService = new TranslationService();

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

        btnTranslate.addActionListener(e -> {
            String word = txtWordPL.getText().trim();
            String targetLang = txtTargetLang.getText().trim();
            if (word.isEmpty() || targetLang.isEmpty()) {
                JOptionPane.showMessageDialog(Client.this, "Please fill in all fields");
                return;
            }
            txtResult.setText("");
            new Thread(() -> {
                String translation = translationService.requestTranslation(word, targetLang);
                SwingUtilities.invokeLater(() -> {
                    if (translation.equals("ERROR")) {
                        JOptionPane.showMessageDialog(Client.this, "Error translating word - Language server unavailable");
                    } else {
                        txtResult.setText("Translation: " + translation);
                    }
                });
            }).start();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            client.setVisible(true);
        });
    }
}
