package com.synclabos.producerconsumer;

import com.synclabos.common.EventSink;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class ProducerConsumerPanel extends JPanel {
    private final Runnable onBack;
    private ProducerConsumerSimulation simulation;
    private volatile ProducerConsumerState latestState;

    private final List<TestScenario> scenarios = buildScenarios();
    private final JList<TestScenario> scenarioList = new JList<>(scenarios.toArray(new TestScenario[0]));
    private TestScenario selectedScenario;
    private Path customFile;

    private final JLabel selectedName = new JLabel("-");
    private final JLabel selectedDescription = new JLabel("-");
    private final JLabel selectedExpected = new JLabel("-");
    private final JLabel selectedCapacity = new JLabel("-");
    private final JLabel selectedConsumers = new JLabel("-");

    private final JLabel currentScenarioLabel = new JLabel("Prueba actual: ninguna");
    private final JLabel producerStateLabel = statusBadge("IDLE");
    private final JLabel criticalLabel = statusBadge("NINGUNA");
    private final JLabel sizeLabel = statusBadge("0/0");
    private final JLabel sumEvenLabel = statusBadge("Pares: 0");
    private final JLabel sumOddLabel = statusBadge("Impares: 0");
    private final JLabel sumPrimeLabel = statusBadge("Primos: 0");
    private final JTextArea consumersSummary = new JTextArea();
    private final JTextArea eventsArea = new JTextArea();

    private final JPanel slotsPanel = new JPanel();
    private final List<JLabel> slotLabels = new ArrayList<>();

    public ProducerConsumerPanel(Runnable onBack) {
        this.onBack = onBack;
        this.selectedScenario = scenarios.get(0);
        this.simulation = createSimulation(selectedScenario);
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(new Color(246, 248, 252));
        buildUi();
        applyScenarioInfo(selectedScenario);
        rebuildSlots(selectedScenario.bufferSize());
        Timer timer = new Timer(120, e -> refreshState());
        timer.start();
    }

    private void buildUi() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Productor-Consumidor - Escenarios de Prueba");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        JLabel subtitle = new JLabel("Selecciona un caso, ejecútalo y observa bloqueos, extracción e inserción en tiempo real.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JPanel topText = new JPanel(new BorderLayout());
        topText.setOpaque(false);
        topText.add(title, BorderLayout.NORTH);
        topText.add(subtitle, BorderLayout.SOUTH);
        header.add(topText, BorderLayout.WEST);

        JButton backButton = new JButton("Volver al menú");
        backButton.addActionListener(e -> {
            simulation.stop();
            onBack.run();
        });
        header.add(backButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel left = buildScenarioPanel();
        JPanel center = buildSimulationPanel();
        JPanel right = buildResultsPanel();

        JPanel body = new JPanel(new BorderLayout(12, 12));
        body.setOpaque(false);
        body.add(left, BorderLayout.WEST);
        body.add(center, BorderLayout.CENTER);
        body.add(right, BorderLayout.EAST);
        add(body, BorderLayout.CENTER);
    }

    private JPanel buildScenarioPanel() {
        JPanel panel = panelCard("Selector de pruebas", 360, 0);
        panel.setLayout(new BorderLayout(0, 10));

        scenarioList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scenarioList.setSelectedIndex(0);
        scenarioList.setCellRenderer(new ScenarioRenderer());
        scenarioList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                TestScenario scenario = scenarioList.getSelectedValue();
                if (scenario != null) {
                    selectedScenario = scenario;
                    applyScenarioInfo(scenario);
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(scenarioList);
        listScroll.setBorder(BorderFactory.createLineBorder(new Color(214, 222, 234)));
        panel.add(listScroll, BorderLayout.CENTER);

        JPanel info = new JPanel(new GridLayout(0, 1, 4, 0));
        info.setOpaque(false);
        info.setBorder(BorderFactory.createTitledBorder("Detalle de la prueba"));
        info.add(selectedName);
        info.add(selectedDescription);
        info.add(selectedExpected);
        info.add(selectedCapacity);
        info.add(selectedConsumers);
        panel.add(info, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildSimulationPanel() {
        JPanel panel = panelCard("Zona de simulación", 0, 0);
        panel.setLayout(new BorderLayout(0, 10));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.setOpaque(false);
        controls.add(controlButton("Ejecutar prueba", new Color(44, 124, 205), e -> runSelectedScenario()));
        controls.add(controlButton("Cambiar prueba", new Color(121, 103, 193), e -> applyScenarioChange()));
        controls.add(controlButton("Pausar", new Color(174, 134, 46), e -> simulation.pause()));
        controls.add(controlButton("Reanudar", new Color(31, 140, 99), e -> simulation.resume()));
        controls.add(controlButton("Reiniciar", new Color(176, 77, 77), e -> resetSimulation()));
        controls.add(controlButton("Archivo externo", new Color(96, 96, 96), e -> loadCustomFile()));
        panel.add(controls, BorderLayout.NORTH);

        currentScenarioLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        panel.add(currentScenarioLabel, BorderLayout.CENTER);

        JPanel bufferPanel = new JPanel(new BorderLayout(0, 10));
        bufferPanel.setOpaque(false);
        bufferPanel.setBorder(BorderFactory.createTitledBorder("Buffer compartido (animación principal)"));
        slotsPanel.setOpaque(false);
        bufferPanel.add(slotsPanel, BorderLayout.CENTER);

        JPanel statusRow = new JPanel(new GridLayout(1, 3, 8, 8));
        statusRow.setOpaque(false);
        statusRow.add(boxWithLabel("Productor", producerStateLabel));
        statusRow.add(boxWithLabel("Sección crítica", criticalLabel));
        statusRow.add(boxWithLabel("Buffer", sizeLabel));
        bufferPanel.add(statusRow, BorderLayout.SOUTH);

        panel.add(bufferPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildResultsPanel() {
        JPanel panel = panelCard("Resultados resumidos", 330, 0);
        panel.setLayout(new BorderLayout(0, 10));

        JPanel sums = new JPanel(new GridLayout(3, 1, 6, 6));
        sums.setOpaque(false);
        sums.add(sumEvenLabel);
        sums.add(sumOddLabel);
        sums.add(sumPrimeLabel);
        panel.add(sums, BorderLayout.NORTH);

        consumersSummary.setEditable(false);
        consumersSummary.setLineWrap(true);
        consumersSummary.setWrapStyleWord(true);
        consumersSummary.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane consumersPane = new JScrollPane(consumersSummary);
        consumersPane.setBorder(BorderFactory.createTitledBorder("Consumidores (activo/esperando/finalizado)"));
        panel.add(consumersPane, BorderLayout.CENTER);

        eventsArea.setEditable(false);
        eventsArea.setLineWrap(true);
        eventsArea.setWrapStyleWord(true);
        eventsArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane eventsPane = new JScrollPane(eventsArea);
        eventsPane.setPreferredSize(new Dimension(280, 130));
        eventsPane.setBorder(BorderFactory.createTitledBorder("Eventos recientes"));
        panel.add(eventsPane, BorderLayout.SOUTH);
        return panel;
    }

    private void runSelectedScenario() {
        try {
            simulation.start(selectedScenario.numbers(), "Escenario: " + selectedScenario.name());
            currentScenarioLabel.setText("Prueba actual: " + selectedScenario.name());
            eventsArea.setText("");
            consumersSummary.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo iniciar la prueba: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyScenarioChange() {
        simulation.stop();
        simulation = createSimulation(selectedScenario);
        latestState = null;
        rebuildSlots(selectedScenario.bufferSize());
        currentScenarioLabel.setText("Prueba actual: " + selectedScenario.name() + " (lista para ejecutar)");
    }

    private void resetSimulation() {
        simulation.reset();
        eventsArea.setText("");
        consumersSummary.setText("");
        currentScenarioLabel.setText("Prueba actual: " + selectedScenario.name() + " (reiniciada)");
    }

    private void loadCustomFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        customFile = chooser.getSelectedFile().toPath();
        try {
            List<Integer> values = java.nio.file.Files.readAllLines(customFile).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();
            TestScenario custom = new TestScenario(
                "Archivo personalizado",
                "Datos cargados por el usuario desde archivo.",
                "Permite validar casos propios del equipo.",
                10,
                1,
                values
            );
            selectedScenario = custom;
            applyScenarioInfo(custom);
            applyScenarioChange();
            currentScenarioLabel.setText("Prueba actual: archivo personalizado listo");
            appendEvent("Archivo externo cargado: " + customFile);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Archivo inválido: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ProducerConsumerSimulation createSimulation(TestScenario scenario) {
        return new ProducerConsumerSimulation(scenario.bufferSize(), scenario.consumerGroups(), uiLogger(), state -> latestState = state);
    }

    private EventSink uiLogger() {
        return message -> SwingUtilities.invokeLater(() -> appendEvent(message));
    }

    private void appendEvent(String message) {
        eventsArea.append(message + "\n");
        eventsArea.setCaretPosition(eventsArea.getDocument().getLength());
    }

    private void applyScenarioInfo(TestScenario scenario) {
        selectedName.setText("Escenario: " + scenario.name());
        selectedDescription.setText("Descripción: " + scenario.description());
        selectedExpected.setText("Esperado: " + scenario.expectedBehavior());
        selectedCapacity.setText("Buffer: " + scenario.bufferSize() + " espacios");
        selectedConsumers.setText("Consumidores: " + (scenario.consumerGroups() * 3));
    }

    private void rebuildSlots(int capacity) {
        slotsPanel.removeAll();
        slotLabels.clear();
        int columns = Math.min(5, Math.max(3, capacity / 2));
        int rows = (int) Math.ceil(capacity / (double) columns);
        slotsPanel.setLayout(new GridLayout(rows, columns, 8, 8));
        for (int i = 0; i < capacity; i++) {
            JLabel slot = new JLabel("_", JLabel.CENTER);
            slot.setOpaque(true);
            slot.setBackground(new Color(241, 244, 249));
            slot.setBorder(BorderFactory.createLineBorder(new Color(182, 192, 206)));
            slot.setFont(new Font("Monospaced", Font.BOLD, 12));
            slotLabels.add(slot);
            slotsPanel.add(slot);
        }
        slotsPanel.revalidate();
        slotsPanel.repaint();
    }

    private void refreshState() {
        ProducerConsumerState state = latestState;
        if (state == null) {
            return;
        }
        producerStateLabel.setText(state.producerState());
        criticalLabel.setText(state.criticalSectionOwner());
        sizeLabel.setText(state.size() + "/" + state.capacity());

        for (int i = 0; i < slotLabels.size() && i < state.slots().size(); i++) {
            String value = state.slots().get(i);
            JLabel slot = slotLabels.get(i);
            slot.setText(value);
            if ("_".equals(value)) {
                slot.setBackground(new Color(241, 244, 249));
            } else {
                slot.setBackground(new Color(203, 239, 213));
            }
        }

        int even = 0;
        int odd = 0;
        int prime = 0;
        StringBuilder consumersText = new StringBuilder();
        for (Map.Entry<String, Integer> entry : state.consumerSums().entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();
            if (key.contains("PARES")) {
                even += value;
            } else if (key.contains("IMPARES")) {
                odd += value;
            } else if (key.contains("PRIMOS")) {
                prime += value;
            }
        }
        sumEvenLabel.setText("Suma pares: " + even);
        sumOddLabel.setText("Suma impares: " + odd);
        sumPrimeLabel.setText("Suma primos: " + prime);

        state.consumerStates().forEach((k, v) -> consumersText.append(k).append(" -> ").append(v).append('\n'));
        consumersSummary.setText(consumersText.toString());
    }

    private static JPanel panelCard(String title, int width, int height) {
        JPanel panel = new JPanel();
        panel.setOpaque(true);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(214, 222, 234)),
            title
        ));
        if (width > 0 || height > 0) {
            panel.setPreferredSize(new Dimension(width > 0 ? width : panel.getPreferredSize().width, height > 0 ? height : panel.getPreferredSize().height));
        }
        return panel;
    }

    private static JLabel statusBadge(String text) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setOpaque(true);
        label.setBackground(new Color(235, 241, 250));
        label.setBorder(BorderFactory.createLineBorder(new Color(192, 205, 222)));
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        return label;
    }

    private static JPanel boxWithLabel(String title, JLabel content) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JLabel label = new JLabel(title, JLabel.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(label, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JButton controlButton(String text, Color color, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        button.addActionListener(listener);
        return button;
    }

    private List<TestScenario> buildScenarios() {
        return List.of(
            new TestScenario(
                "Prueba 1 - Secuencia Mixta",
                "Secuencia completa de números para clasificación mixta.",
                "Verificar clasificación completa en pares, impares y primos con carga alta.",
                5,
                1,
                loadWalterDataset("walter/Pruebas/numeros.txt")
            ),
            new TestScenario(
                "Prueba 2 - Dominio de Pares",
                "Subconjunto centrado en números pares.",
                "Validar consumo predominante del consumidor de pares.",
                5,
                1,
                loadWalterDataset("walter/Pruebas/pares.txt")
            ),
            new TestScenario(
                "Prueba 3 - Validación Rápida",
                "Conjunto corto para validación rápida.",
                "Demostración breve de clasificación y acumulación de sumas.",
                5,
                1,
                loadWalterDataset("walter/Pruebas/doc.txt")
            )
        );
    }

    private List<Integer> loadWalterDataset(String resourcePath) {
        InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (input == null) {
            throw new IllegalStateException("No se encontró el recurso de prueba seleccionado.");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            return reader.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer recurso: " + resourcePath, e);
        }
    }

    private record TestScenario(
        String name,
        String description,
        String expectedBehavior,
        int bufferSize,
        int consumerGroups,
        List<Integer> numbers
    ) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static final class ScenarioRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
            JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus
        ) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof TestScenario scenario) {
                label.setText("<html><b>" + scenario.name() + "</b><br/>" + scenario.description() + "</html>");
            }
            label.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            label.setBackground(isSelected ? new Color(215, 229, 249) : Color.WHITE);
            label.setForeground(new Color(36, 52, 82));
            return label;
        }
    }
}
