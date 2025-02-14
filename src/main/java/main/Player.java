package main;

import org.java_websocket.WebSocket;

// Klasa reprezentująca pojedynczego gracza w systemie
public class Player {
    public WebSocket connection; // Połączenie gracza z serwerem
    public String name; // Nazwa gracza
    public GameRoom currentGame; // Aktualna gra, w której bierze udział gracz

    // Konstruktor gracza, inicjuje połączenie
    public Player(WebSocket connection) {
        this.connection = connection;
    }

    // Wysyła wiadomość do gracza, jeśli połączenie jest otwarte
    public void send(String message) {
        if (connection.isOpen()) {
            connection.send(message);
        }
    }
}
