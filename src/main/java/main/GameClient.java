package main;

import javafx.application.Platform;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import javafx.scene.control.Alert;
import java.util.function.Consumer;
import javafx.scene.control.Alert.AlertType;
// Klasa klienta WebSocket do komunikacji z serwerem gry
public class GameClient extends WebSocketClient {
    private Consumer<String> onGameState;      // Obsługuje stan gry
    private Consumer<String> onChatMessage;    // Obsługuje wiadomości czatu
    private Runnable onGameStart;              // Obsługuje rozpoczęcie gry
    private Runnable onGameCreated;            // Obsługuje utworzenie gry
    private Runnable onGameJoined;             // Obsługuje dołączenie do gry
    private int playerIndex = -1;

    public GameClient() {
        super(URI.create("ws://localhost:12345"));  // URI serwera (należy zastąpić prawdziwym adresem)
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Połączono z serwerem");
    }

    private void handleSpecialCard(String cardData) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Specjalna karta!");
            alert.setHeaderText("Gracz użył: " + cardData);
            alert.showAndWait();
        });
    }

    // Metody obsługi zdarzeń WebSocket
    @Override
    public void onMessage(String message) {
        System.out.println("Otrzymano wiadomość: " + message);
        String[] parts = message.split(":", 2);
        String type = parts[0];  // Typ wiadomości (np. GAME_STATE, CHAT, itp.)
        String data = parts.length > 1 ? parts[1] : "";  // Treść wiadomości (np. dane stanu gry)
        if (message.startsWith("SPECIAL_CARD:")) {
            handleSpecialCard(message.replace("SPECIAL_CARD:", ""));
        }
        if(message.startsWith("PLAYER_INDEX:")) {
            playerIndex = Integer.parseInt(message.split(":")[1]);
            System.out.println("Twój indeks gracza: " + playerIndex);
            return;
        }

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
            case "WINNER":
                handleWinnerMessage(data);
                break;
            case "ERROR": // Błąd z serwera
                Platform.runLater(() -> showErrorAlert("Błąd serwera", data));
                break;
            default:
                System.out.println("Nieznany typ wiadomości: " + type); // Obsługa nieznanych typów wiadomości
        }
    }
    private void handleWinnerMessage(String data) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Koniec gry");

            if (data.equals("YOU_WIN")) {
                alert.setHeaderText("Gratulacje!");
                alert.setContentText("Wygrałeś!");
            } else if (data.equals("YOU_LOSE")) {
                alert.setHeaderText("Koniec gry");
                alert.setContentText("Przegrałeś. Powodzenia następnym razem!");
            }

            alert.showAndWait();
            System.exit(0);
        });
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


    public void setOnGameStart(Runnable handler) {
        this.onGameStart = handler;
    }


    public int getPlayerIndex() {
        return playerIndex;
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
    // Metody do zarządzania grą
    public void createGame(String gameId) {
        send("CREATE:" + gameId);
    }

    public void joinGame(String gameId) {
        send("JOIN:" + gameId);
    }
}
