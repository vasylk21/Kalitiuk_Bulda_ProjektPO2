package main;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;
import java.util.*;

public class GameRoom implements Serializable {
    private static final long serialVersionUID = 1L; // Do serializacji

    private final String gameId;
    private final CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();
    private final GameLogic gameLogic;
    private final AtomicInteger currentPlayerIndex = new AtomicInteger(0);
    private final List<List<Card>> playerHands = new ArrayList<>();

    private transient ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(); // transient do wykluczenia z serializacji
    private volatile boolean gameStarted = false;

    public GameRoom(String gameId, Player creator) {
        this.gameId = gameId;
        this.gameLogic = new GameLogic();
        addPlayer(creator);

        if (!players.isEmpty()) {
            gameLogic.initialize(players.size());
        }
    }

    public String getGameId() {
        return gameId;
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public synchronized void addPlayer(Player player) {
        if (players.size() >= 4) {
            player.send("BŁĄD: Pokój jest pełny");
            return;
        }

        players.add(player);
        player.currentGame = this;

        new Thread(() -> {
            try {
                Thread.sleep(500); // Dajemy czas na inicjalizację UI
                broadcastGameState();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        // Upewniamy się, że playerHands ma odpowiednią ilość kart dla wszystkich graczy
        while (playerHands.size() < players.size()) {
            playerHands.add(new ArrayList<>()); // Inicjalizujemy pustą rękę dla nowego gracza
        }

        if (players.size() >= 2 && !gameStarted) {
            gameStarted = true;
            startGame();
        } else {
            broadcastGameState();
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

    public void handleAction(Player player, String action) {
        int playerIndex = players.indexOf(player);
        if (playerIndex == currentPlayerIndex.get()) {
            gameLogic.handleAction(playerIndex, action);
            nextTurn();
            broadcastGameState();
        }
    }

    public void checkIfAllPlayersReady() {
        if (players.size() >= 2 && players.stream().allMatch(Player::isReady)) {
            startGame();  // Jeśli wszyscy są gotowi, rozpoczynamy grę
        }
    }

    public void startGame() {
        if (players.isEmpty()) {
            System.out.println("Błąd: Brak graczy do rozpoczęcia gry!");
            return;
        }

        if (gameStarted) {
            System.out.println("⚠ Gra już się rozpoczęła!");
            return;
        }

        System.out.println("🚀 Gra rozpoczyna się!");
        gameStarted = true;

        gameLogic.initialize(players.size());
        broadcastMessage("START"); // Wysyłamy sygnał do klientów
        broadcastGameState(); // Aktualizujemy stan gry dla wszystkich
        startTurnTimer();
    }

    public boolean hasStarted() {
        return gameStarted;
    }

    public int getPlayerCount() {
        return players.size(); // Zwracamy rozmiar listy graczy
    }

    private void nextTurn() {
        currentPlayerIndex.set((currentPlayerIndex.get() + 1) % players.size());
        startTurnTimer();
    }

    private void startTurnTimer() {
        if (timer.isShutdown()) {
            timer = Executors.newSingleThreadScheduledExecutor();
        }
        timer.schedule(this::autoNextTurn, 30, TimeUnit.SECONDS);
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

    public void saveGame(String filename) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(this.gameLogic); // Zapisujemy tylko logikę gry
        } catch (IOException e) {
            System.out.println("Błąd podczas zapisywania gry: " + e.getMessage());
            throw e;
        }
    }

    public void loadGame(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            GameLogic loadedLogic = (GameLogic) in.readObject();
            this.gameLogic.copyFrom(loadedLogic); // Przywracamy logikę gry
            broadcastGameState();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Błąd podczas ładowania gry: " + e.getMessage());
            throw e;
        }
    }

    private void closeRoom() {
        timer.shutdown();
        GameServer.activeGames.remove(gameId);
        System.out.println("🗑 Pokój " + gameId + " został zamknięty (wszyscy gracze wyszli)");
    }
}
