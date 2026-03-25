package com.synclabos.ui;

import com.synclabos.diningphilosophers.DiningPhilosophersPanel;
import com.synclabos.producerconsumer.ProducerConsumerPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class MainFrame extends JFrame {
    private static final String CARD_HOME = "home";
    private static final String CARD_PC = "pc";
    private static final String CARD_DP = "dp";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    public MainFrame() {
        setTitle("SyncLab OS");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1320, 840));
        setLocationRelativeTo(null);
        buildUi();
    }

    private void buildUi() {
        cardPanel.add(buildHomePanel(), CARD_HOME);
        cardPanel.add(new ProducerConsumerPanel(this::goHome), CARD_PC);
        cardPanel.add(new DiningPhilosophersPanel(this::goHome), CARD_DP);
        add(cardPanel, BorderLayout.CENTER);
        cardLayout.show(cardPanel, CARD_HOME);
    }

    private JPanel buildHomePanel() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBorder(BorderFactory.createEmptyBorder(36, 48, 36, 48));
        root.setBackground(new Color(242, 246, 251));

        JLabel title = new JLabel("SyncLab OS", JLabel.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 46));
        title.setForeground(new Color(27, 46, 86));

        JLabel subtitle = new JLabel(
            "Visualizando la concurrencia, entendiendo la sincronización.",
            JLabel.CENTER
        );
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 18));
        subtitle.setForeground(new Color(73, 87, 112));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.NORTH);
        top.add(subtitle, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(16, 150, 16, 150));

        JPanel cards = new JPanel(new GridLayout(1, 2, 16, 0));
        cards.setOpaque(false);

        JButton producerConsumerButton = cardButton(
            "Productor-Consumidor",
            "Buffer finito, bloqueos, sección crítica y sumas por consumidor.",
            new Color(34, 139, 87)
        );
        producerConsumerButton.addActionListener(e -> cardLayout.show(cardPanel, CARD_PC));

        JButton diningButton = cardButton(
            "Filósofos Comensales",
            "Mesa circular con estados visuales y sincronización concurrente.",
            new Color(44, 124, 205)
        );
        diningButton.addActionListener(e -> cardLayout.show(cardPanel, CARD_DP));

        cards.add(producerConsumerButton);
        cards.add(diningButton);

        JButton exitButton = new JButton("Salir");
        exitButton.setFont(new Font("SansSerif", Font.BOLD, 15));
        exitButton.setBackground(new Color(173, 67, 67));
        exitButton.setForeground(Color.WHITE);
        exitButton.setFocusPainted(false);
        exitButton.setBorder(BorderFactory.createEmptyBorder(10, 26, 10, 26));
        exitButton.setMargin(new Insets(10, 26, 10, 26));
        exitButton.addActionListener(e -> dispose());

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.add(exitButton);

        center.add(cards, BorderLayout.CENTER);
        center.add(bottom, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        return root;
    }

    private static JButton cardButton(String title, String description, Color baseColor) {
        JButton button = new JButton(
            "<html><div style='text-align:center;'><b style='font-size:15px;'>"
                + title + "</b><br/><span style='font-size:11px;'>" + description + "</span></div></html>"
        );
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        button.setFont(new Font("SansSerif", Font.PLAIN, 14));
        button.setBackground(baseColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(22, 18, 22, 18));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(baseColor.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseColor);
            }
        });
        return button;
    }

    private void goHome() {
        cardLayout.show(cardPanel, CARD_HOME);
    }
}
