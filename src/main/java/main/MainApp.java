package main;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Gra UNO");

        // Логотип
        String logoPath = "file:src/resources/logo.png";
        ImageView logoImageView = new ImageView(new Image(logoPath));
        logoImageView.setFitWidth(200);  // Устанавливаем ширину логотипа
        logoImageView.setPreserveRatio(true);  // Сохраняем пропорции логотипа

        // Кнопки
        Button playButton = new Button("Graj");
        Button rulesButton = new Button("Zasady");
        Button exitButton = new Button("Wyjście");

        // Стиль кнопок
        String buttonStyle = "-fx-font-size: 20px; -fx-padding: 10px 20px; -fx-background-color: #4CAF50; -fx-text-fill: white;";

        playButton.setStyle(buttonStyle);
        rulesButton.setStyle(buttonStyle);
        exitButton.setStyle(buttonStyle);

        // Обработчики событий для кнопок
        playButton.setOnAction(e -> WaitingRoom.showWaitingRoom(primaryStage));
        rulesButton.setOnAction(e -> showRules(primaryStage));
        exitButton.setOnAction(e -> System.exit(0));

        // Размещение элементов
        VBox menuLayout = new VBox(20, logoImageView, playButton, rulesButton, exitButton);
        menuLayout.setStyle("-fx-alignment: center; -fx-background-image: url('/background.png'); -fx-background-size: cover;");

        // Сцена
        Scene menuScene = new Scene(new StackPane(menuLayout), 800, 600);
        primaryStage.setScene(menuScene);
        primaryStage.show();
    }

    private void showRules(Stage primaryStage) {
        Stage rulesStage = new Stage();
        rulesStage.setTitle("Zasady Gry UNO");

        // Текст правил
        Label rulesLabel = new Label("Tu znajdziesz zasady gry UNO.\n\n" +
                "1. Każdy gracz otrzymuje 7 kart.\n" +
                "2. Celem gry jest pozbycie się wszystkich kart.\n" +
                "3. Karta może być zagrana, jeśli pasuje kolorem lub wartością do karty na stole.\n" +
                "4. Specjalne karty: Stop, Zmiana, Dobierz Dwie.\n" +
                "5. Wygrywa gracz, który pierwszy pozbędzie się wszystkich kart.");

        // Кнопка назад
        Button backButton = new Button("Wróć");
        backButton.setStyle("-fx-font-size: 20px; -fx-padding: 10px 20px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        backButton.setOnAction(e -> rulesStage.close());

        VBox rulesLayout = new VBox(20, rulesLabel, backButton);
        rulesLayout.setStyle("-fx-alignment: center; -fx-padding: 20px; -fx-background-color: white;");

        Scene rulesScene = new Scene(rulesLayout, 400, 300);
        rulesStage.setScene(rulesScene);
        rulesStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
