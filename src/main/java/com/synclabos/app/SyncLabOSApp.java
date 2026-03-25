package com.synclabos.app;

import com.synclabos.ui.MainFrame;
import javax.swing.SwingUtilities;

public final class SyncLabOSApp {
    private SyncLabOSApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
