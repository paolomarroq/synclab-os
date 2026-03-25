package com.synclabos.diningphilosophers;

import com.synclabos.common.EventSink;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class DiningPhilosophersPanel extends JPanel {
    private static final int N = 5;
    private static final String[] NAMES = {
        "Sócrates", "Platón", "Aristóteles", "Descartes", "Nietzsche"
    };
    private static final Color[] BASE_COLORS = {
        new Color(96, 171, 255),
        new Color(106, 206, 167),
        new Color(255, 183, 110),
        new Color(212, 157, 242),
        new Color(247, 143, 143)
    };

    private final Runnable onBack;
    private final JTextArea hiddenLog = new JTextArea();
    private final JLabel statusBar = new JLabel("Estado general: listo");
    private final List<JLabel> compactLabels = new ArrayList<>();
    private final CircularTableView tableView = new CircularTableView();
    private final DiningPhilosophersSimulation simulation;
    private volatile DiningState latestState;

    public DiningPhilosophersPanel(Runnable onBack) {
        this.onBack = onBack;
        this.simulation = new DiningPhilosophersSimulation(N, uiLogger(), state -> latestState = state);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(new Color(246, 248, 252));
        buildUi();
        Timer timer = new Timer(90, e -> refreshState());
        timer.start();
    }

    private EventSink uiLogger() {
        return message -> SwingUtilities.invokeLater(() -> {
            hiddenLog.append(message + "\n");
            if (message.contains("sección crítica")) {
                statusBar.setText("Estado general: un filósofo está comiendo");
            } else if (message.contains("bloqueado")) {
                statusBar.setText("Estado general: hay filósofos esperando recursos");
            } else if (message.contains("pensando")) {
                statusBar.setText("Estado general: filósofos en reflexión");
            }
        });
    }

    private void buildUi() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Filósofos Comensales - Simulación Visual");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        JLabel subtitle = new JLabel("Estados visibles en la mesa: pensar, esperar y comer");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JPanel titleBox = new JPanel(new BorderLayout());
        titleBox.setOpaque(false);
        titleBox.add(title, BorderLayout.NORTH);
        titleBox.add(subtitle, BorderLayout.SOUTH);
        header.add(titleBox, BorderLayout.WEST);

        JButton backButton = new JButton("Volver al menú");
        backButton.addActionListener(e -> {
            simulation.stop();
            onBack.run();
        });
        header.add(backButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        controls.setOpaque(false);
        JButton startButton = controlButton("Iniciar", new Color(44, 124, 205), e -> simulation.start());
        JButton pauseButton = controlButton("Pausar", new Color(172, 133, 45), e -> simulation.pause());
        JButton resumeButton = controlButton("Reanudar", new Color(28, 142, 97), e -> simulation.resume());
        JButton resetButton = controlButton("Reiniciar", new Color(130, 108, 202), e -> {
            simulation.stop();
            hiddenLog.setText("");
            simulation.start();
        });
        JButton debugButton = controlButton("Ver debug", new Color(96, 96, 96), e -> showDebugDialog());

        controls.add(startButton);
        controls.add(pauseButton);
        controls.add(resumeButton);
        controls.add(resetButton);
        controls.add(debugButton);

        JPanel rightLegend = new JPanel();
        rightLegend.setOpaque(false);
        rightLegend.setLayout(new BorderLayout(0, 8));
        rightLegend.setBorder(BorderFactory.createTitledBorder("Resumen rápido"));
        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new javax.swing.BoxLayout(rows, javax.swing.BoxLayout.Y_AXIS));
        for (int i = 0; i < N; i++) {
            JLabel row = new JLabel(NAMES[i] + " - Pensando");
            row.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
            compactLabels.add(row);
            rows.add(row);
        }
        JLabel legend = new JLabel("Azul: pensando | Amarillo: esperando | Verde: comiendo");
        legend.setFont(new Font("SansSerif", Font.PLAIN, 12));
        rightLegend.add(rows, BorderLayout.CENTER);
        rightLegend.add(legend, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);
        center.add(controls, BorderLayout.NORTH);
        center.add(tableView, BorderLayout.CENTER);
        center.add(rightLegend, BorderLayout.EAST);
        add(center, BorderLayout.CENTER);

        statusBar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        statusBar.setOpaque(true);
        statusBar.setBackground(new Color(233, 239, 248));
        add(statusBar, BorderLayout.SOUTH);
    }

    private JButton controlButton(String text, Color color, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        button.addActionListener(listener);
        return button;
    }

    private void showDebugDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Debug de eventos");
        dialog.setModal(false);
        JTextArea area = new JTextArea(hiddenLog.getText());
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        dialog.add(new JScrollPane(area));
        dialog.setSize(720, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void refreshState() {
        DiningState state = latestState;
        if (state == null) {
            return;
        }
        for (int i = 0; i < N; i++) {
            compactLabels.get(i).setText(
                NAMES[i] + " - " + state.philosopherStates().get(i).label() + " | comidas: " + state.mealsByPhilosopher().get(i)
            );
        }
        tableView.repaint();
    }

    private final class CircularTableView extends JPanel {
        private CircularTableView() {
            setPreferredSize(new Dimension(860, 620));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createTitledBorder("Mesa circular"));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int cx = width / 2;
            int cy = height / 2;
            int tableRadius = Math.min(width, height) / 4;
            int philosopherRadius = Math.min(width, height) / 3;
            int forkRadius = philosopherRadius - 55;

            g.setColor(new Color(219, 198, 162));
            g.fill(new Ellipse2D.Double(cx - tableRadius, cy - tableRadius, tableRadius * 2, tableRadius * 2));
            g.setColor(new Color(102, 78, 43));
            g.setStroke(new BasicStroke(2.5f));
            g.draw(new Ellipse2D.Double(cx - tableRadius, cy - tableRadius, tableRadius * 2, tableRadius * 2));
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString("Mesa", cx - 20, cy + 6);

            DiningState state = latestState;

            for (int i = 0; i < N; i++) {
                double angle = (-Math.PI / 2) + (2 * Math.PI * i / N);
                int px = cx + (int) (Math.cos(angle) * philosopherRadius);
                int py = cy + (int) (Math.sin(angle) * philosopherRadius);

                PhilosopherState philosopherState = state == null ? PhilosopherState.THINKING : state.philosopherStates().get(i);
                Color stateColor = switch (philosopherState) {
                    case THINKING -> new Color(162, 207, 255);
                    case WAITING -> new Color(247, 214, 124);
                    case EATING -> new Color(146, 216, 158);
                };

                g.setColor(BASE_COLORS[i]);
                g.fillOval(px - 34, py - 34, 68, 68);
                g.setColor(stateColor);
                g.fillOval(px - 28, py - 28, 56, 56);
                g.setColor(new Color(54, 54, 54));
                g.drawOval(px - 28, py - 28, 56, 56);
                g.setFont(new Font("SansSerif", Font.BOLD, 12));
                g.drawString("F" + i, px - 8, py + 4);

                g.setFont(new Font("SansSerif", Font.PLAIN, 13));
                g.drawString(NAMES[i], px - 42, py + 48);

                if (philosopherState == PhilosopherState.EATING) {
                    g.setColor(new Color(66, 170, 95));
                    g.drawOval(px - 38, py - 38, 76, 76);
                }
            }

            for (int i = 0; i < N; i++) {
                double forkAngle = (-Math.PI / 2) + (2 * Math.PI * i / N) + (Math.PI / N);
                int fx = cx + (int) (Math.cos(forkAngle) * forkRadius);
                int fy = cy + (int) (Math.sin(forkAngle) * forkRadius);
                String owner = state == null ? "Libre" : state.forkOwners().get(i);
                boolean occupied = !"Libre".equals(owner);
                g.setColor(occupied ? new Color(206, 72, 72) : new Color(130, 130, 130));
                g.fillRoundRect(fx - 6, fy - 24, 12, 48, 8, 8);
                g.setColor(Color.BLACK);
                g.drawRoundRect(fx - 6, fy - 24, 12, 48, 8, 8);
                g.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g.drawString("T" + i, fx - 8, fy + 36);
            }
            g.dispose();
        }
    }
}
