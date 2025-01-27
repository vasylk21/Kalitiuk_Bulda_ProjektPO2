package main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.json.JSONObject;

import java.net.URL;

public class GameApp extends Application {
    private ImageView topCardImageView;
    private HBox playerHandBox;
    private HBox opponentHandBox;
    private ImageView drawPileImageView;
    private GameClient gameClient;
    private Button drawCardButton;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("UNO Game");

        // Верхняя карта на столе
        topCardImageView = new ImageView();
        topCardImageView.setFitWidth(100);
        topCardImageView.setPreserveRatio(true);

        // Карты в руках игрока
        playerHandBox = new HBox(10);
        playerHandBox.setStyle("-fx-padding: 10px; -fx-alignment: center;");
        playerHandBox.setAlignment(Pos.CENTER);

        // Карты в руках оппонента
        opponentHandBox = new HBox(10);
        opponentHandBox.setStyle("-fx-padding: 10px; -fx-alignment: center;");
        opponentHandBox.setAlignment(Pos.CENTER);
        updateOpponentHand(7); // Начальное количество карт оппонента

        // Колода карт
        drawPileImageView = loadImageView("/card_back.png", 80);

        // Кнопка для взятия карты
        drawCardButton = new Button("Draw a card");
        drawCardButton.setStyle("-fx-font-size: 16px; -fx-padding: 10px 20px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        drawCardButton.setOnAction(e -> gameClient.sendAction("dobierz"));

        VBox layout = new VBox(10);
        layout.setStyle("-fx-alignment: center; -fx-background-image: url('/background.png'); -fx-background-size: cover;");
        layout.setAlignment(Pos.CENTER);

        HBox middleBox = new HBox(10, topCardImageView, drawPileImageView);
        middleBox.setAlignment(Pos.CENTER);

        layout.getChildren().addAll(opponentHandBox, middleBox, playerHandBox, drawCardButton);

        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        gameClient = new GameClient();
        gameClient.setOnGameStateReceived(this::updateGameState); // Устанавливаем обработчик для обновления состояния игры
        new Thread(() -> gameClient.startClient()).start();
    }

    public void updateGameState(String gameState) {
        System.out.println("Обновление состояния игры: " + gameState);

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(gameState);
        } catch (org.json.JSONException e) {
            System.out.println("Invalid JSON: " + gameState);
            return;
        }

        Platform.runLater(() -> {
            String topCard = jsonObject.getString("top_card").toUpperCase();
            topCardImageView.setImage(loadImage("/" + topCard + ".png")); // Показываем карту на столе

            // Отображаем карты игрока
            playerHandBox.getChildren().clear();
            jsonObject.getJSONArray("player_hand").forEach(card -> {
                String cardImageName = card.toString().toUpperCase();
                ImageView cardView = new ImageView(loadImage("/" + cardImageName + ".png"));
                cardView.setFitWidth(80);
                cardView.setPreserveRatio(true);

                // Обработчик клика по карте
                cardView.setOnMouseClicked(e -> gameClient.sendAction(String.valueOf(jsonObject.getJSONArray("player_hand").toList().indexOf(card))));
                playerHandBox.getChildren().add(cardView);
            });

            // Отображаем карты противника как скрытые карты
            updateOpponentHand(jsonObject.getInt("opponent_card_count"));
        });
    }

    // Отображаем карты противника как скрытые карты
    private void updateOpponentHand(int cardCount) {
        opponentHandBox.getChildren().clear();
        for (int i = 0; i < cardCount; i++) {
            ImageView cardBackView = loadImageView("/card_back.png", 80);
            opponentHandBox.getChildren().add(cardBackView);
        }
    }

    private Image loadImage(String path) {
        URL resource = getClass().getResource(path);
        if (resource == null) {
            System.err.println("Файл ресурса не найден: " + path);
            return null;
        }
        return new Image(resource.toExternalForm());
    }

    private ImageView loadImageView(String path, double fitWidth) {
        Image image = loadImage(path);
        if (image == null) return new ImageView(); // Вернуть пустой ImageView, если ресурс не найден

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(fitWidth);
        imageView.setPreserveRatio(true);
        imageView.setMouseTransparent(true); // Отключаем клики для рубашек
        return imageView;
    }

    public void startGame(Stage primaryStage, GameClient client) {
        System.out.println("Игра начинается...");
        gameClient = client;
        Platform.runLater(() -> start(primaryStage));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
