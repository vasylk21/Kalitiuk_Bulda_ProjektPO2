package main;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.concurrent.*;

public class GameServer extends WebSocketServer {
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    protected static ConcurrentHashMap<String, GameRoom> activeGames = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<WebSocket, Player> connectedPlayers = new ConcurrentHashMap<>();

    // Konstruktor serwera, przyjmujący port
    public GameServer(int port) {
        super(new InetSocketAddress(port));
    }

    // Obsługuje nowe połączenie klienta
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Player player = new Player(conn);
        connectedPlayers.put(conn, player);
        System.out.println("Nowe połączenie: " + conn.getRemoteSocketAddress());
    }

    // Obsługuje wiadomości przychodzące od klienta
    @Override
    public void onMessage(WebSocket conn, String message) {
        threadPool.execute(() -> handleMessage(conn, message));
    }

    // Przetwarza wiadomości od gracza
    private void handleMessage(WebSocket conn, String message) {
        Player player = connectedPlayers.get(conn);
        String[] parts = message.split(":", 2);
        String command = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        try {
            // Zależnie od komendy wykonuje odpowiednią akcję
            switch (command) {
                case "CREATE":
                    createGame(player, data);
                    break;
                case "JOIN":
                    joinGame(player, data);
                    break;
                case "ACTION":
                    handleGameAction(player, data);
                    break;
                case "CHAT":
                    handleChatMessage(player, data);
                    break;
                case "SAVE":
                    handleSaveGame(player, data);
                    break;
                case "LOAD":
                    handleLoadGame(player, data);
                    break;
                default:
                    player.send("BŁĄD: Nieznana komenda");
            }
        } catch (Exception e) {
            System.err.println("Błąd serwera: " + e.getMessage());
            e.printStackTrace();
            player.send("BŁĄD: " + e.getMessage());
        }
    }

    // Tworzy nową grę
    private void createGame(Player player, String gameId) {
        if (activeGames.containsKey(gameId)) {
            player.send("BŁĄD: Gra o tym ID już istnieje");
            return;
        }

        try {
            GameRoom room = new GameRoom(gameId, player);
            activeGames.put(gameId, room);
            player.send("GRA_UTWORZONA:" + gameId);
        } catch (Exception e) {
            player.send("BŁĄD: Błąd przy tworzeniu gry");
        }
    }

    // Dołącza gracza do gry
    private void joinGame(Player player, String gameId) {
        GameRoom game = activeGames.get(gameId);
        if (game != null) {
            game.addPlayer(player);
            player.send("GRA_DOŁĄCZONA:" + gameId);

            // Gra nie jest uruchamiana tutaj, dzieje się to w metodzie addPlayer()
        } else {
            player.send("BŁĄD: Gra nie znaleziona");
        }
    }

    // Obsługuje akcję gracza w grze
    private void handleGameAction(Player player, String action) {
        if (player.currentGame != null) {
            player.currentGame.handleAction(player, action);
        }
    }

    // Obsługuje wiadomość czatu od gracza
    private void handleChatMessage(Player player, String message) {
        if (player.currentGame != null) {
            player.currentGame.broadcastMessage(player.name + ": " + message);
        }
    }

    // Obsługuje zapis gry
    private void handleSaveGame(Player player, String filename) {
        try {
            if (player.currentGame != null) {
                player.currentGame.saveGame(filename);
                player.send("ZAPIS_SUCCEED: Gra zapisana pomyślnie");
            }
        } catch (IOException e) {
            player.send("ZAPIS_BŁĄD:" + e.getMessage());
            e.printStackTrace();
        }
    }

    // Obsługuje ładowanie gry
    private void handleLoadGame(Player player, String filename) {
        try {
            if (player.currentGame != null) {
                player.currentGame.loadGame(filename);
                player.send("ŁADOWANIE_SUCCEED: Gra załadowana pomyślnie");
                player.currentGame.broadcastGameState();
            }
        } catch (IOException | ClassNotFoundException e) {
            player.send("ŁADOWANIE_BŁĄD:" + e.getMessage());
            e.printStackTrace();
        }
    }

    // Obsługuje zamknięcie połączenia z klientem
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Player player = connectedPlayers.remove(conn);
        if (player != null && player.currentGame != null) {
            GameRoom game = player.currentGame;
            game.removePlayer(player);
            System.out.println("Gracz rozłączył się z gry: " + game.getGameId());

            // Jeśli gra jest pusta, usuwamy ją z aktywnych gier
            if (game.isEmpty()) {
                activeGames.remove(game.getGameId());
                System.out.println("Gra " + game.getGameId() + " usunięta (brak graczy)");
            }
        }
    }

    // Obsługuje błędy WebSocket
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Błąd WebSocket: " + ex.getMessage());
        ex.printStackTrace();
    }

    // Inicjalizacja serwera
    @Override
    public void onStart() {
        System.out.println("Serwer WebSocket uruchomiony!");
    }

    // Główna metoda uruchamiająca serwer
    public static void main(String[] args) {
        GameServer server = new GameServer(12345);
        server.start();
        System.out.println("Serwer uruchomiony na porcie 12345");
    }
}
