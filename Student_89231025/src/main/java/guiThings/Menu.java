package guiThings;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;

import basics.MapPanelJustGermany;
import util.LogLevel;
import util.Logger;

public class Menu extends JFrame {
    private JPanel mapContainerPanel;
    private JLabel statusLabel;

    public Menu() {
        setTitle("K-means Clustering");
        setSize(1000, 800);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top control panel
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        controlPanel.setBackground(Color.lightGray);

        JLabel modeLabel = new JLabel("Choose mode");
        modeLabel.setFont(new Font("Arial", Font.BOLD, 18));
        modeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        controlPanel.add(modeLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        Color buttonColor = Color.decode("#7393B3");

        //mode buttons
        JButton sequentialButton = new JButton("Sequential");
        sequentialButton.setBackground(buttonColor);

        JButton parallelButton = new JButton("Parallel");
        parallelButton.setBackground(buttonColor);

        JButton distributedButton = new JButton("Distributed");
        distributedButton.setBackground(buttonColor);

        JButton clearMapButton = new JButton("Clear Map");
        clearMapButton.setBackground(Color.DARK_GRAY);
        clearMapButton.setForeground(Color.WHITE);

        buttonPanel.add(sequentialButton);
        buttonPanel.add(parallelButton);
        buttonPanel.add(distributedButton);
        buttonPanel.add(clearMapButton);

        controlPanel.add(buttonPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.NORTH);

        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        statusLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
        add(statusLabel, BorderLayout.SOUTH);

        // Map display area
        mapContainerPanel = new JPanel(new BorderLayout());
        mapContainerPanel.setBackground(Color.WHITE);
        add(mapContainerPanel, BorderLayout.CENTER);

        sequentialButton.addActionListener(e -> openInputDialog("Sequential", false, false));
        parallelButton.addActionListener(e -> openInputDialog("Parallel", true, false));
        distributedButton.addActionListener(e -> openInputDialog("Distributed", false, true));
        //distributedButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Distributed mode is under development."));

        clearMapButton.addActionListener(e -> {
            mapContainerPanel.removeAll();
            mapContainerPanel.revalidate();
            mapContainerPanel.repaint();
            statusLabel.setText("Map cleared.");
        });

        Logger.log("GUI - Menu initialized", LogLevel.Success);

        while (true){
            if ( testInternetConnection() == true){
                break;
            }
        }
    }

    public boolean testInternetConnection(){ // from beloved stackoverflow
        try {
            URL url = new URL("http://www.google.com");
            URLConnection connection = url.openConnection();
            connection.connect();
            // JOptionPane.showMessageDialog(this, "Internet is connected");
            return true;
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(this, "Internet is not connected");
            return false;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Internet is not connected");
            return false;
        }
    }

    private void openInputDialog(String modeName, boolean isParallel, boolean isDistributed) {

        JDialog settingsDialog = new JDialog(this, modeName + " Settings", true);
        settingsDialog.setSize(400, 300);
        settingsDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Correct background coloring
        if (isDistributed) {
            panel.setBackground(new Color(200, 180, 240)); // Light purple for distributed
        } else if (isParallel) {
            panel.setBackground(new Color(180, 230, 250)); // Light blue for parallel
        } else {
            panel.setBackground(Color.PINK);
        }

        Border border = BorderFactory.createLineBorder(Color.BLACK);

        JTextField sitesField = new JTextField();
        JTextField clustersField = new JTextField();
        JTextField cyclesField = new JTextField();

        sitesField.setBorder(border);
        clustersField.setBorder(border);
        cyclesField.setBorder(border);

        panel.add(new JLabel("Number of Accumulation Sites:"));
        panel.add(sitesField);
        panel.add(new JLabel("Number of Clusters:"));
        panel.add(clustersField);
        panel.add(new JLabel("Number of Cycles:"));
        panel.add(cyclesField);

        JButton confirmButton = new JButton("Confirm");
        panel.add(new JLabel()); // placeholder
        panel.add(confirmButton);

        confirmButton.addActionListener((ActionEvent e) -> {
            Logger.log("Confirm button pressed", LogLevel.Status);
            try {
                int numSites = Integer.parseInt(sitesField.getText());
                int numClusters = Integer.parseInt(clustersField.getText());
                int numCycles = Integer.parseInt(cyclesField.getText());

                if (numSites <= 0 || numClusters <= 0 || numCycles <= 0 || numClusters > numSites) {
                    throw new NumberFormatException("Invalid values");
                }

                settingsDialog.dispose();

                MapPanelJustGermany newMapPanel = new MapPanelJustGermany(
                        numSites,
                        numClusters,
                        numCycles,
                        isParallel,
                        isDistributed
                );

                mapContainerPanel.removeAll();
                mapContainerPanel.add(newMapPanel, BorderLayout.CENTER);
                mapContainerPanel.revalidate();
                mapContainerPanel.repaint();

                String modeText = isDistributed ? "Distributed" : (isParallel ? "Parallel" : "Sequential");
                statusLabel.setText("Running in " + modeText + " mode with " +
                        numSites + " sites, " + numClusters + " clusters, " + numCycles + " cycles.");

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(settingsDialog,
                        "Please enter valid positive numbers.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                sitesField.setText("");
                clustersField.setText("");
                cyclesField.setText("");
            }
        });

        settingsDialog.add(panel);
        settingsDialog.setVisible(true);
    }

}
