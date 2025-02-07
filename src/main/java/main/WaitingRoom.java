package main;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class WaitingRoom {
    public static Label waitingLabel; // Etykieta z informacją o oczekiwaniu
    private static GameClient gameClient; // Obiekt klienta gry

    // Metoda do wyświetlenia pokoju oczekiwania
    public static void showWaitingRoom(Stage primaryStage) {
        // Tworzenie etykiety oczekiwania
        waitingLabel = new Label("Oczekiwanie na podłączenie drugiego gracza...");

        // Tworzenie układu
        StackPane waitingLayout = new StackPane(waitingLabel);
        Scene waitingScene = new Scene(waitingLayout, 800, 600);

        // Ustawienie sceny
        primaryStage.setScene(waitingScene);
        primaryStage.show();

        // Uruchomienie klienta gry i połączenie z serwerem
        gameClient = new GameClient();

        // Ustawienie obsługi zdarzenia rozpoczęcia gry
        gameClient.setOnGameStart(() -> startGame(primaryStage));

        // Połączenie z serwerem w osobnym wątku
        new Thread(() -> {
            gameClient.connect();  // Łączenie z serwerem WebSocket
        }).start();
    }

    // Metoda uruchamiająca grę po jej rozpoczęciu
    public static void startGame(Stage primaryStage) {
        System.out.println("Rozpoczynamy grę...");
        Platform.runLater(() -> {
            // Po połączeniu i rozpoczęciu gry, przechodzimy do ekranu gry
            GameApp gameApp = new GameApp();
            gameApp.start(primaryStage);
        });
    }
}
