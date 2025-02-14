package main;

import java.util.concurrent.*;
import java.io.*;
import java.util.*;
import java.time.LocalDate;
import java.util.stream.Collectors;
import org.json.JSONObject;

// Klasa reprezentująca pokój gry zarządzający graczami i logiką
public class GameRoom implements Serializable {
    private static final long serialVersionUID = 1L; // Do serializacji

    private final String gameId;
    private final CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();
    private final GameLogic gameLogic;
    private final List<List<Card>> playerHands = new ArrayList<>();

    private transient ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(); // transient do wykluczenia z serializacji
    private volatile boolean gameStarted = false;

    public GameRoom(String gameId, Player creator) {
        this.gameId = gameId;
        this.gameLogic = new GameLogic();
        addPlayer(creator);
    }

    public String getGameId() {
        return gameId;
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }
    // Metody zarządzania graczami
    public synchronized void addPlayer(Player player) {
        if (players.size() >= 4) {
            player.send("ERROR: Pokój jest pełny");
            return;
        }

        players.add(player);
        player.currentGame = this;
        int playerIndex = players.indexOf(player);
        player.name = "Player " + (playerIndex + 1);
        new Thread(() -> {
            try {
                Thread.sleep(500);
                if (gameStarted) {
                    broadcastGameState();
                } else {
                    JSONObject state = new JSONObject();
                    state.put("status", "WAITING");
                    state.put("players", players.size());
                    player.send("GAME_STATE:" + state.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        if (players.size() >= 2 && !gameStarted) {
            startGame();
            // Отправляем правильные индексы всем игрокам
            for (int i = 0; i < players.size(); i++) {
                players.get(i).send("PLAYER_INDEX:" + i);
            }
        }
    }

    public void removePlayer(Player player) {
        players.remove(player);
        if (players.isEmpty()) {
            closeRoom();
        } else {
            broadcastGameState();
        }
    }

    // Mechanika gry
    public void handleAction(Player player, String action) {
        int playerIndex = players.indexOf(player);
        int currentPlayer = gameLogic.getCurrentPlayer();

        if (playerIndex != currentPlayer) {
            player.send("ERROR: Nie twoja kolej");
            return;
        }

        try {
            if (action.startsWith("COLOR:")) {
                String chosenColor = action.split(":")[1];
                gameLogic.chooseColor(chosenColor);
                broadcastGameState();
                checkWinner();
            } else {
                gameLogic.handleAction(playerIndex, action);
                broadcastGameState();
                checkWinner();
            }
        } catch (Exception e) {
            player.send("ERROR: " + e.getMessage());
        }
    }

    private void startGame() {
        if (gameStarted || players.size() < 2) return;
        System.out.println("[GAME] Starting game with players: " + players.size());
        System.out.println("[GAME] Player order: " +
                players.stream()
                        .map(p -> "Player " + players.indexOf(p))
                        .collect(Collectors.joining(", ")));
        try {
            gameLogic.initialize(players.size());
            gameStarted = true;
            gameLogic.setCurrentPlayer(0);
            System.out.println("Game started: " + gameId);
            broadcastMessage("START");
            broadcastGameState();
            startTurnTimer();
        } catch (Exception e) {
            System.err.println("Game initialization failed: " + e.getMessage());
            broadcastMessage("ERROR: Failed to start game");
        }
    }

    private void nextTurn() {
        gameLogic.nextTurn();
        int nextPlayer = gameLogic.getCurrentPlayer();
        System.out.println("[SERVER] Turn passed to player: " + nextPlayer);
        broadcastGameState(); // Рассылаем обновленное состояние всем игрокам
    }

    private void startTurnTimer() {
        if (timer.isShutdown()) {
            timer = Executors.newSingleThreadScheduledExecutor();
        }
        timer.schedule(this::autoNextTurn, 30, TimeUnit.SECONDS);
    }
    public void handleColorChoice(Player player, String color) {
        int playerIndex = players.indexOf(player);
        int currentPlayer = gameLogic.getCurrentPlayer();

        if (playerIndex != currentPlayer) {
            player.send("ERROR: Not your turn to choose color");
            return;
        }

        try {
            gameLogic.chooseColor(color);
            broadcastGameState();
            checkWinner();
        } catch (Exception e) {
            player.send("ERROR: " + e.getMessage());
        }
    }


    private void autoNextTurn() {
        nextTurn();
        broadcastGameState();
    }

    public void broadcastMessage(String message) {
        players.forEach(p -> p.send("CHAT:" + message));
    }

    public void broadcastGameState() {
        players.forEach(p -> {
            String state = gameLogic.getGameState(players.indexOf(p));
            p.send("GAME_STATE:" + state);
        });
    }

    // Zapis/odczyt stanu gry
    public void saveGame(String filename) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(this.gameLogic);
        } catch (IOException e) {
            System.out.println("Błąd podczas zapisywania gry: " + e.getMessage());
            throw e;
        }
    }

    public void loadGame(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            GameLogic loadedLogic = (GameLogic) in.readObject();
            this.gameLogic.copyFrom(loadedLogic);
            broadcastGameState();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Błąd podczas ładowania gry: " + e.getMessage());
            throw e;
        }
    }
    private void checkWinner() {
        if (gameLogic.hasWinner()) {
            gameLogic.getWinningPlayerIndex().ifPresent(winnerIndex -> {
                String winnerName = players.get(winnerIndex).name;

                saveResult(winnerName);

                for (int i = 0; i < players.size(); i++) {
                    Player p = players.get(i);
                    if (i == winnerIndex) {
                        p.send("WINNER:YOU_WIN");
                    } else {
                        p.send("WINNER:YOU_LOSE");
                    }
                }

                // Закрываем комнату
                closeRoom();
            });
        }
    }

    public void handleDrawAction(Player player) {
        int playerIndex = players.indexOf(player);
        int currentPlayer = gameLogic.getCurrentPlayer();

        if (playerIndex != currentPlayer) {
            player.send("ERROR: Not your turn");
            return;
        }

        try {
            gameLogic.handleAction(playerIndex, "DRAW");
            broadcastGameState();
            checkWinner();
        } catch (Exception e) {
            player.send("ERROR: " + e.getMessage());
        }
    }

    private void saveResult(String winnerName) {
        FileManager fileManager = new FileManager("game_history.csv");

        String specialCardsUsed = gameLogic.getUsedSpecialCards();
        if (specialCardsUsed.isEmpty()) {
            specialCardsUsed = "Nie";
        }

        String data = winnerName + "," + LocalDate.now() + "," + specialCardsUsed;

        fileManager.saveResult(data);
    }

    private void closeRoom() {
        timer.shutdown();
        GameServer.activeGames.remove(gameId);
        System.out.println("Gra " + gameId + " została zamknięta (gracze opuścili pokój)");
    }

}
