package main;

import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import javafx.scene.control.Alert;
import java.util.function.Consumer;

public class GameClient extends WebSocketClient {
    private Consumer<String> onGameState;      // Obsługuje stan gry
    private Consumer<String> onChatMessage;    // Obsługuje wiadomości czatu
    private Runnable onGameStart;              // Obsługuje rozpoczęcie gry
    private Runnable onGameCreated;            // Obsługuje utworzenie gry
    private Runnable onGameJoined;             // Obsługuje dołączenie do gry

    public GameClient() {
        super(URI.create("ws://localhost:12345"));  // URI serwera (należy zastąpić prawdziwym adresem)
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Połączono z serwerem");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Otrzymano wiadomość: " + message);
        String[] parts = message.split(":", 2);
        String type = parts[0];  // Typ wiadomości (np. GAME_STATE, CHAT, itp.)
        String data = parts.length > 1 ? parts[1] : "";  // Treść wiadomości (np. dane stanu gry)

        switch (type) {
            case "GAME_STATE": // Zaktualizowanie stanu gry
                Platform.runLater(() -> {
                    if (onGameState != null) onGameState.accept(data);
                });
                break;
            case "CHAT": // Zaktualizowanie wiadomości czatu
                Platform.runLater(() -> {
                    if (onChatMessage != null) onChatMessage.accept(data);
                });
                break;
            case "START": // Rozpoczęcie gry
                Platform.runLater(() -> {
                    System.out.println("Gra rozpoczęta! Ładowanie UI...");
                    if (onGameStart != null) {
                        onGameStart.run();
                    }
                });
                break;

            case "GAME_CREATED": // Nowa gra została utworzona
                Platform.runLater(() -> {
                    System.out.println("Gra została pomyślnie utworzona!");
                    if (onGameCreated != null) onGameCreated.run();
                });
                break;
            case "GAME_JOINED": // Dołączono do gry
                Platform.runLater(() -> {
                    System.out.println("Dołączono do gry!");
                    if (onGameJoined != null) onGameJoined.run();
                });
                break;
            case "ERROR": // Błąd z serwera
                Platform.runLater(() -> showErrorAlert("Błąd serwera", data));
                break;
            default:
                System.out.println("Nieznany typ wiadomości: " + type); // Obsługa nieznanych typów wiadomości
        }
    }

    private void showErrorAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public void setOnGameState(Consumer<String> handler) {
        this.onGameState = handler;
    }

    public void setOnChatMessage(Consumer<String> handler) {
        this.onChatMessage = handler;
    }

    public void setOnGameStart(Runnable handler) {
        this.onGameStart = handler;
    }

    public void setOnGameCreated(Runnable handler) {
        this.onGameCreated = handler;
    }

    public void setOnGameJoined(Runnable handler) {
        this.onGameJoined = handler;
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Połączenie zamknięte: " + reason);
        attemptReconnect(); // Próba ponownego połączenia
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("Błąd WebSocket: " + ex.getMessage());
    }

    public void send(String message) {
        if (this.isOpen()) {
            super.send(message);
            System.out.println("Wysłano wiadomość: " + message);
        } else {
            System.out.println("Połączenie jest zamknięte, próbuję ponownie połączyć...");
            attemptReconnect(); // Próba ponownego połączenia w przypadku zamknięcia połączenia
        }
    }

    private void attemptReconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Czekaj 2 sekundy przed próbą ponownego połączenia
                System.out.println("Próba ponownego połączenia...");
                this.reconnect(); // Ponowne połączenie
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void setReady(boolean isReady) {
        send("READY:" + isReady);
    }

    public void createGame(String gameId) {
        send("CREATE:" + gameId);
    }

    public void joinGame(String gameId) {
        send("JOIN:" + gameId);
    }
}
