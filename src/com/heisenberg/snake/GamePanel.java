package com.heisenberg.snake;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

public final class GamePanel extends JPanel {
    private static final int ROWS = 24;
    private static final int COLUMNS = 30;
    private static final int MIN_CELL_SIZE = 20;

    private volatile GameSnapshot snapshot;

    public GamePanel() {
        setPreferredSize(new Dimension(COLUMNS * MIN_CELL_SIZE, ROWS * MIN_CELL_SIZE));
        setBackground(new Color(13, 17, 23));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int cellSize = Math.max(MIN_CELL_SIZE, Math.min(getWidth() / COLUMNS, getHeight() / ROWS));
        int boardWidth = cellSize * COLUMNS;
        int boardHeight = cellSize * ROWS;
        int offsetX = Math.max(0, (getWidth() - boardWidth) / 2);
        int offsetY = Math.max(0, (getHeight() - boardHeight) / 2);

        paintGrid(g2, cellSize, offsetX, offsetY);
        GameSnapshot current = snapshot;
        if (current != null) {
            paintFood(g2, current.food(), cellSize, offsetX, offsetY);
            for (GameSnapshot.SnakeView snake : current.snakes()) {
                paintSnake(g2, snake, cellSize, offsetX, offsetY);
            }
        }

        g2.dispose();
    }

    private void paintGrid(Graphics2D g2, int cellSize, int offsetX, int offsetY) {
        g2.setColor(new Color(22, 27, 34));
        for (int x = 0; x <= COLUMNS; x++) {
            int lineX = offsetX + (x * cellSize);
            g2.drawLine(lineX, offsetY, lineX, offsetY + (ROWS * cellSize));
        }
        for (int y = 0; y <= ROWS; y++) {
            int lineY = offsetY + (y * cellSize);
            g2.drawLine(offsetX, lineY, offsetX + (COLUMNS * cellSize), lineY);
        }
    }

    private void paintFood(Graphics2D g2, Point food, int cellSize, int offsetX, int offsetY) {
        if (food == null) {
            return;
        }
        int x = offsetX + (food.x() * cellSize);
        int y = offsetY + (food.y() * cellSize);
        int inset = Math.max(4, cellSize / 6);
        g2.setColor(new Color(255, 179, 71));
        g2.fillOval(x + inset, y + inset, cellSize - (inset * 2), cellSize - (inset * 2));
    }

    private void paintSnake(Graphics2D g2, GameSnapshot.SnakeView snake, int cellSize, int offsetX, int offsetY) {
        int inset = Math.max(2, cellSize / 10);
        int arc = Math.max(8, cellSize / 3);
        for (int i = snake.segments().size() - 1; i >= 0; i--) {
            Point segment = snake.segments().get(i);
            int x = offsetX + (segment.x() * cellSize);
            int y = offsetY + (segment.y() * cellSize);
            g2.setColor(i == 0 ? snake.headColor() : snake.bodyColor());
            g2.fillRoundRect(x + inset, y + inset, cellSize - (inset * 2), cellSize - (inset * 2), arc, arc);
        }

        if (!snake.alive() && !snake.segments().isEmpty()) {
            Point head = snake.segments().getFirst();
            int x = offsetX + (head.x() * cellSize);
            int y = offsetY + (head.y() * cellSize);
            g2.setColor(Color.WHITE);
            g2.setFont(getFont().deriveFont(Font.BOLD, Math.max(11f, cellSize * 0.45f)));
            g2.drawString("X", x + (cellSize / 3), y + ((cellSize * 2) / 3));
        }
    }

    public void updateSnapshot(GameSnapshot snapshot) {
        this.snapshot = snapshot;
        repaint();
    }

    public int rows() {
        return ROWS;
    }

    public int columns() {
        return COLUMNS;
    }
}
