package com.heisenberg.snake;

import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SnakeGameFrame frame = new SnakeGameFrame();
            frame.setVisible(true);
        });
    }
}
