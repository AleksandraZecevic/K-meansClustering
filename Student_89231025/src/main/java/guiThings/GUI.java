package guiThings;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.Sequential.MapFrame;
import org.Sequential.MapFrameJustGermany;
import util.LogLevel;
import util.Logger;

public class GUI extends JFrame {
    public GUI(){

        setTitle("K-means clustering");
        setSize(600, 200);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.lightGray);

        JLabel modeLabel = new JLabel("Choose mode");
        modeLabel.setFont(new Font("Arial", Font.BOLD, 18));
        panel.add(modeLabel, BorderLayout.NORTH);
        modeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        modeLabel.setBorder(new EmptyBorder(0, 0, 20, 0)); // 20px bottom padding

        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 10, 10));

        Color buttonColor = Color.decode("#7393B3");

        JButton sequentialButton = new JButton("Sequential");
        sequentialButton.setBackground(buttonColor);
        //sequentialButton.setBorder(new RoundedBorder(20));
        sequentialButton.setSize(150,80);

        JButton parallelButton = new JButton("Parallel");
        parallelButton.setBackground(buttonColor);
        //parallelButton.setBorder(new RoundedBorder(20));
        parallelButton.setSize(150,80);

        JButton distributedButton = new JButton("Distributed");
        distributedButton.setBackground(buttonColor);
        //distributedButton.setBorder(new RoundedBorder(20));
        distributedButton.setSize(150,80);

        buttonPanel.add(sequentialButton);
        buttonPanel.add(parallelButton);
        buttonPanel.add(distributedButton);

        panel.add(buttonPanel, BorderLayout.CENTER);

        sequentialButton.addActionListener(e -> openSequential());
        parallelButton.addActionListener(e-> openParallel());
        //parallelButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Parallel mode is under development."));
        distributedButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Distributed mode is under development."));
        
        add(panel);
        Logger.log("GUI - Menu made", LogLevel.Success);

        while (true){
           if ( testInternetConection() == true){
               break;
           }
        }

    }

    public boolean testInternetConection(){ // from beloved stackoverflow
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

    private void openSequential() {
        dispose();
        Border Crniborder = BorderFactory.createLineBorder(Color.BLACK);

        JFrame sequentialFrame = new JFrame("Sequential settings");
        sequentialFrame.setSize(400,300);
        sequentialFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        sequentialFrame.setLocation(200,200);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBackground(Color.pink);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel sitesLabel = new JLabel("Number of Accumulation Sites:");
        JTextField sitesField = new JTextField();
        // editing looks
        sitesField.setBorder(Crniborder);
        sitesField.setBackground(Color.getHSBColor(100,75,79));

        JLabel clustersLabel = new JLabel("Number of Clusters:");
        JTextField clustersField = new JTextField();
        // editing looks
        clustersField.setBorder(Crniborder);
        clustersField.setBackground(Color.getHSBColor(100,75,79));

        JLabel cyclesLabel = new JLabel("Number of Cycles:");
        JTextField cyclesField = new JTextField();
        // editing looks
        cyclesField.setBorder(Crniborder);
        cyclesField.setBackground(Color.getHSBColor(100,75,79));

        JButton confirmButton = new JButton("Confirm");

        panel.add(sitesLabel);
        panel.add(sitesField);

        panel.add(clustersLabel);
        panel.add(clustersField);

        panel.add(cyclesLabel);
        panel.add(cyclesField);

        panel.add(new JLabel()); // Empty space
        panel.add(confirmButton);

        sequentialFrame.add(panel);
        sequentialFrame.setVisible(true);

        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int numSites = Integer.parseInt(sitesField.getText());
                    int numClusters = Integer.parseInt(clustersField.getText());
                    int numCycles = Integer.parseInt(cyclesField.getText());

                    if(numSites<0 || numClusters<0 || numCycles<0 || numClusters>numSites){
                      numCycles= Integer.parseInt(""); // couldn't make up better solution so here it is hahah
                    }

                    // Pass these values to your MapFrame
                    // new MapFrame(numSites, numClusters, numCycles);
                    new MapFrameJustGermany(numSites, numClusters, numCycles, false);

                    // Close the settings frame
                    sequentialFrame.dispose();

                } catch (NumberFormatException ex) {
                    sitesField.setText("");
                    clustersField.setText("");
                    cyclesField.setText("");
                    JOptionPane.showMessageDialog(sequentialFrame, "Please enter valid numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void openParallel() {
        dispose();
        Border Crniborder = BorderFactory.createLineBorder(Color.BLACK);

        JFrame parallelFrame = new JFrame("Parallel settings");
        parallelFrame.setSize(400,300);
        parallelFrame.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        parallelFrame.setLocation(200,200);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBackground(Color.pink);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel sitesLabel = new JLabel("Number of Accumulation Sites:");
        JTextField sitesField = new JTextField();
        // editing looks
        sitesField.setBorder(Crniborder);
        sitesField.setBackground(Color.getHSBColor(100,75,79));

        JLabel clustersLabel = new JLabel("Number of Clusters:");
        JTextField clustersField = new JTextField();
        // editing looks
        clustersField.setBorder(Crniborder);
        clustersField.setBackground(Color.getHSBColor(100,75,79));

        JLabel cyclesLabel = new JLabel("Number of Cycles:");
        JTextField cyclesField = new JTextField();
        // editing looks
        cyclesField.setBorder(Crniborder);
        cyclesField.setBackground(Color.getHSBColor(100,75,79));

        JButton confirmButton = new JButton("Confirm");

        panel.add(sitesLabel);
        panel.add(sitesField);

        panel.add(clustersLabel);
        panel.add(clustersField);

        panel.add(cyclesLabel);
        panel.add(cyclesField);

        panel.add(new JLabel()); // Empty space
        panel.add(confirmButton);

        parallelFrame.add(panel);
        parallelFrame.setVisible(true);

        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int numSites = Integer.parseInt(sitesField.getText());
                    int numClusters = Integer.parseInt(clustersField.getText());
                    int numCycles = Integer.parseInt(cyclesField.getText());

                    if(numSites<0 || numClusters<0 || numCycles<0 || numClusters>numSites){
                        numCycles= Integer.parseInt(""); // couldn't make up better solution so here it is hahah
                    }

                    // Pass these values to your MapFrame
                    // new MapFrame(numSites, numClusters, numCycles);
                    new MapFrameJustGermany(numSites, numClusters, numCycles, true);

                    // Close the settings frame
                    parallelFrame.dispose();

                } catch (NumberFormatException ex) {
                    sitesField.setText("");
                    clustersField.setText("");
                    cyclesField.setText("");
                    JOptionPane.showMessageDialog(parallelFrame, "Please enter valid numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
}
