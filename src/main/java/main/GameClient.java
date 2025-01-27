package main;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.stage.Stage;

public class GameClient {
    private Socket socket;
    private PrintWriter output;
    private BufferedReader input;
    private Consumer<String> onGameStateReceived; // Callback для обновления UI

    public void startClient() {
        try {
            socket = new Socket("localhost", 12345); // Подключаемся к серверу
            output = new PrintWriter(socket.getOutputStream(), true);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Поток для чтения данных от сервера
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = input.readLine()) != null) {
                        System.out.println("Получено сообщение от сервера: " + serverMessage); // Добавлено логирование
                        if (serverMessage.equals("START_GAME")) {
                            Platform.runLater(() -> {
                                Stage primaryStage = (Stage) WaitingRoom.waitingLabel.getScene().getWindow();
                                GameApp gameApp = new GameApp();
                                gameApp.startGame(primaryStage, this);
                            });
                        } else if (onGameStateReceived != null) {
                            onGameStateReceived.accept(serverMessage);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для отправки хода игрока (например, выбрать карту или взять карту из колоды)
    public void sendAction(String action) {
        if (output != null) {
            output.println(action);
            System.out.println("Отправлено действие на сервер: " + action); // Добавлено логирование
        }
    }

    // Устанавливаем обработчик для обновления интерфейса на основе состояния игры
    public void setOnGameStateReceived(Consumer<String> callback) {
        this.onGameStateReceived = callback;
    }
}
