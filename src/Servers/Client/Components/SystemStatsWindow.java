package Servers.Client.Components;

import Common.DataStructures.BarrelStats;
import Common.DataStructures.SystemStats;

import javax.swing.*;
import java.awt.*;
import java.util.List;
public class SystemStatsWindow extends JFrame {
    private final JLabel statusLabel;
    private final JTextArea textArea;

    public SystemStatsWindow() {
        super("System Statistics");
        setSize(800, 800);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        //Status
        statusLabel = new JLabel();
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 20));
        add(statusLabel, BorderLayout.NORTH);

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
    }

    public void updateStats(SystemStats systemStats) {
        StringBuilder sb = new StringBuilder();

        sb.append("[Client]\n");
        //Top 10 Searches
        sb.append("--- Top 10 Searches ---\n");
        List<String> topSearches = systemStats.getTop10Searches();
        if (topSearches.isEmpty()) {
            sb.append("No searches recorded.\n");
        } else {
            for (int i = 0; i < topSearches.size(); i++) {
                sb.append(i + 1).append(". '").append(topSearches.get(i)).append("'\n");
            }
        }

        //Barrels Statistics
        sb.append("\n--- Barrels Statistics ---\n");
        List<BarrelStats> barrels = systemStats.getBarrelsStats();
        if (barrels.isEmpty()) {
            sb.append("No barrels initialized.\n");
        } else {
            for (BarrelStats barrel : barrels) {
                sb.append("\nBarrel: ").append(barrel.getName()).append("\n");
                sb.append("  Status: ").append(barrel.getStatus() ? "Online" : "Offline").append("\n");
                if (barrel.getStatus()) {
                    sb.append("  Pages Received: ").append(barrel.getPagesReceived()).append("\n");
                    sb.append("  Pages Indexed: ").append(barrel.getPages()).append("\n");
                    sb.append("  Tokens: ").append(barrel.getTokens()).append("\n");
                    sb.append("  Token URLs: ").append(barrel.getTokenURLs()).append("\n");
                    sb.append("  URLs: ").append(barrel.getURLs()).append("\n");
                    sb.append("  Linking URLs: ").append(barrel.getLinkingURLs()).append("\n");
                    sb.append("  Avg Response Time: ").append(barrel.getAvgResponseTime() / 100).append(" ds\n");
                    sb.append("  Requests Handled: ").append(barrel.getNRequests()).append("\n");
                }
            }
        }
        textArea.setText(sb.toString());
    }
    public void showStatusMessage(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Status: " + message));
    }
}