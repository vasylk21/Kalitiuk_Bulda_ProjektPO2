package main;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class WaitingRoom {
    public static Label waitingLabel;
    private static GameClient gameClient;

    public static void showWaitingRoom(Stage primaryStage) {
        waitingLabel = new Label("Oczekiwanie na podłączenie drugiego gracza...");

        StackPane waitingLayout = new StackPane(waitingLabel);
        Scene waitingScene = new Scene(waitingLayout, 800, 600);

        primaryStage.setScene(waitingScene);
        primaryStage.show();

        // Запуск клиента
        new Thread(() -> {
            gameClient = new GameClient();
            gameClient.startClient();
        }).start();
    }

    public static void startGame(Stage primaryStage) {
        System.out.println("Начинаем игру...");
        Platform.runLater(() -> {
            GameApp gameApp = new GameApp();
            gameApp.startGame(primaryStage, gameClient);
        });
    }
}
