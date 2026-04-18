package com.heisenberg.snake;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public final class SnakeGameFrame extends JFrame {
    private final GamePanel gamePanel = new GamePanel();
    private final JLabel statusLabel = new JLabel("Loading...");
    private final JLabel playerOneLabel = new JLabel("Player 1");
    private final JLabel playerTwoLabel = new JLabel("Player 2");
    private final JTextArea leaderboardArea = new JTextArea(8, 22);

    private final AsyncDatabaseService databaseService;
    private final GameEngine engine;

    public SnakeGameFrame() {
        super("Multiplayer Snake");
        setUndecorated(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        List<PlayerProfile> profiles = List.of(
                new PlayerProfile("Player 1", new Color(46, 204, 113), new Color(34, 153, 84),
                        Direction.RIGHT, new Point(5, 6),
                        KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D),
                new PlayerProfile("Player 2", new Color(52, 152, 219), new Color(41, 128, 185),
                        Direction.LEFT, new Point(24, 17),
                        KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT)
        );

        this.databaseService = new AsyncDatabaseService(
                new ScoreRepository(Path.of("data", "snake.db").toAbsolutePath().toString()));
        this.engine = new GameEngine(
                gamePanel.rows(),
                gamePanel.columns(),
                profiles,
                snapshot -> SwingUtilities.invokeLater(() -> renderSnapshot(snapshot)),
                result -> handleMatchFinished(result, profiles));

        configureWindow();
        configureInput();
        databaseService.initialize(this::setStatus);
        refreshLeaderboard();
    }

    private void configureWindow() {
        setLayout(new BorderLayout(16, 16));
        getContentPane().setBackground(new Color(9, 13, 17));

        add(gamePanel, BorderLayout.CENTER);
        add(buildSidebar(), BorderLayout.EAST);
        add(buildControls(), BorderLayout.SOUTH);

        pack();

        GraphicsDevice device = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();
        if (device.isFullScreenSupported()) {
            device.setFullScreenWindow(this);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    private JPanel buildSidebar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(15, 23, 32));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Multiplayer Snake");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        statusLabel.setForeground(new Color(255, 203, 107));
        playerOneLabel.setForeground(new Color(46, 204, 113));
        playerTwoLabel.setForeground(new Color(52, 152, 219));

        JTextArea controlsArea = new JTextArea("""
                Controls
                P1: W A S D
                P2: Arrow Keys

                Goal
                Stay alive and outscore the other snake.
                Match results are stored in SQLite.
                """);
        controlsArea.setEditable(false);
        controlsArea.setLineWrap(true);
        controlsArea.setWrapStyleWord(true);
        controlsArea.setForeground(new Color(209, 213, 219));
        controlsArea.setBackground(new Color(15, 23, 32));
        controlsArea.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));

        leaderboardArea.setEditable(false);
        leaderboardArea.setLineWrap(true);
        leaderboardArea.setWrapStyleWord(true);
        leaderboardArea.setBackground(new Color(9, 13, 17));
        leaderboardArea.setForeground(new Color(201, 209, 217));
        leaderboardArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(48, 54, 61)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));

        panel.add(title);
        panel.add(statusLabel);
        panel.add(playerOneLabel);
        panel.add(playerTwoLabel);
        panel.add(controlsArea);
        panel.add(new JScrollPane(leaderboardArea));
        return panel;
    }

    private JPanel buildControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        controls.setBackground(new Color(15, 23, 32));

        JButton startButton = new JButton("Start Match");
        JButton resetButton = new JButton("Reset");

        startButton.addActionListener(event -> engine.start());
        resetButton.addActionListener(event -> {
            engine.reset();
            refreshLeaderboard();
        });

        controls.add(startButton);
        controls.add(resetButton);
        return controls;
    }

    private void configureInput() {
        JComponent target = (JComponent) getContentPane();
        bindKey(target, KeyEvent.VK_W);
        bindKey(target, KeyEvent.VK_A);
        bindKey(target, KeyEvent.VK_S);
        bindKey(target, KeyEvent.VK_D);
        bindKey(target, KeyEvent.VK_UP);
        bindKey(target, KeyEvent.VK_DOWN);
        bindKey(target, KeyEvent.VK_LEFT);
        bindKey(target, KeyEvent.VK_RIGHT);
    }

    private void bindKey(JComponent component, int keyCode) {
        String actionKey = "key-" + keyCode;
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keyCode, 0), actionKey);
        component.getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                engine.changeDirection(keyCode);
            }
        });
    }

    private void renderSnapshot(GameSnapshot snapshot) {
        gamePanel.updateSnapshot(snapshot);
        if (snapshot.snakes().size() >= 2) {
            playerOneLabel.setText(snapshot.snakes().get(0).playerName() + ": " + snapshot.snakes().get(0).score());
            playerTwoLabel.setText(snapshot.snakes().get(1).playerName() + ": " + snapshot.snakes().get(1).score());
        }
        statusLabel.setText(snapshot.statusText());
    }

    private void refreshLeaderboard() {
        databaseService.loadLeaderboard(records -> SwingUtilities.invokeLater(() -> {
            if (records.isEmpty()) {
                leaderboardArea.setText("Leaderboard\nNo saved matches yet.");
                return;
            }
            StringBuilder builder = new StringBuilder("Leaderboard\n");
            for (ScoreRepository.ScoreRecord record : records) {
                builder.append(record.winner())
                        .append(" - ")
                        .append(record.topScore())
                        .append(" pts - ")
                        .append(record.finishedAt())
                        .append('\n');
            }
            leaderboardArea.setText(builder.toString());
        }), this::setStatus);
    }

    private void handleMatchFinished(MatchResult result, List<PlayerProfile> profiles) {
        List<String> playerNames = profiles.stream()
                .map(PlayerProfile::name)
                .toList();
        databaseService.saveMatch(result, playerNames, this::setStatus);
        refreshLeaderboard();
    }

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    @Override
    public void dispose() {
        engine.shutdown();
        databaseService.shutdown();
        super.dispose();
    }
}
