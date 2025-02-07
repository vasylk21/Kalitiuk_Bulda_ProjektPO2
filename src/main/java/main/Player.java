package main;

import org.java_websocket.WebSocket;

public class Player {
    public WebSocket connection; // Połączenie gracza z serwerem
    public String name; // Nazwa gracza
    public GameRoom currentGame; // Aktualna gra, w której bierze udział gracz
    private boolean isReady = false; // Flaga gotowości gracza

    // Konstruktor gracza, inicjuje połączenie
    public Player(WebSocket connection) {
        this.connection = connection;
    }

    // Metoda do ustawienia gotowości gracza
    public void setReady(boolean isReady) {
        this.isReady = isReady;
        if (currentGame != null) {
            currentGame.checkIfAllPlayersReady(); // Sprawdzamy gotowość wszystkich graczy
        }
    }

    // Sprawdza, czy gracz jest gotowy
    public boolean isReady() {
        return isReady;
    }

    // Wysyła wiadomość do gracza, jeśli połączenie jest otwarte
    public void send(String message) {
        if (connection.isOpen()) {
            connection.send(message);
        }
    }
}
