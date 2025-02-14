package main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.json.JSONArray;
import javafx.scene.control.ScrollPane;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.FileNotFoundException;
// Główna klasa aplikacji klienckiej implementująca interfejs graficzny
public class GameApp extends Application {
    // Komponenty interfejsu użytkownika
    private Stage primaryStage;           // Główne okno aplikacji
    private GameClient client;            // Klient do komunikacji z serwerem
    private HBox playerHand = new HBox(10); // Kontener na karty gracza
    private Label currentPlayerLabel = new Label(); // Etykieta aktualnego gracza
    private ImageView topCardView;        // Widok górnej karty na stosie
    private Button drawButton;            // Przycisk dobierania kart
    private HBox opponentsContainer;      // Kontener na ręce przeciwników
    private boolean gameScreenInitialized = false; // Flaga inicjalizacji ekranu gry
    private boolean waitingForColor = false;      // Flaga oczekiwania na wybór koloru
    private boolean colorPickerShown = false;     // Flaga czy pokazano wybór koloru


    @Override
    public void start(Stage stage) {
        // Inicjalizacja głównego okna i komponentów UI
        primaryStage = stage;

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-image: url('/background.png'); -fx-background-size: cover;");

        ImageView logo = new ImageView(loadImage("/logo.png"));
        logo.setFitWidth(200);
        logo.setPreserveRatio(true);

        Button btnCreate = new Button("Utwórz grę");
        Button btnJoin = new Button("Dołącz do gry");
        TextField txtGameId = new TextField();
        txtGameId.setPromptText("ID gry");

        HBox menu = new HBox(10, btnCreate, txtGameId, btnJoin);
        menu.setAlignment(Pos.CENTER);
        root.getChildren().addAll(logo, menu);

        btnCreate.setOnAction(e -> {
            client.createGame(txtGameId.getText());
            showWaitingScreen(stage);
        });

        btnJoin.setOnAction(e -> {
            client.joinGame(txtGameId.getText());
            showWaitingScreen(stage);
        });

        client = new GameClient();
        client.setOnGameStart(() -> Platform.runLater(() -> showGameScreen(primaryStage)));
        client.setOnGameState(this::updateUI);
        client.connect();

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("UNO");
        stage.setScene(scene);
        stage.show();
    }
    // Metoda pokazująca ekran oczekiwania na graczy
    private void showWaitingScreen(Stage stage) {
        VBox waiting = new VBox(20);
        waiting.setAlignment(Pos.CENTER);

        ProgressIndicator progress = new ProgressIndicator();
        Label label = new Label("Oczekiwanie na graczy...");

        waiting.getChildren().addAll(progress, label);
        stage.setScene(new Scene(waiting, 800, 600));
    }
    // Metoda inicjalizująca ekran gry
    private void showGameScreen(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #2e8b57;");

        opponentsContainer = new HBox(20);
        opponentsContainer.setAlignment(Pos.CENTER);
        opponentsContainer.setPadding(new Insets(10));
        root.setTop(opponentsContainer);

        HBox centerPane = new HBox(30);
        centerPane.setAlignment(Pos.CENTER);

        ImageView deckView = new ImageView(loadImage("/card_back.png"));
        deckView.setFitWidth(100);
        deckView.setPreserveRatio(true);
        deckView.setOnMouseClicked(e -> client.send("ACTION:DRAW"));

        topCardView = new ImageView();
        topCardView.setFitWidth(120);
        topCardView.setPreserveRatio(true);

        centerPane.getChildren().addAll(deckView, topCardView);
        root.setCenter(centerPane);

        ScrollPane scrollPane = new ScrollPane(playerHand);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent;");
        playerHand.setSpacing(10);
        playerHand.setPadding(new Insets(15));
        root.setBottom(scrollPane);

        VBox infoPanel = new VBox(20);
        infoPanel.setPadding(new Insets(20));
        infoPanel.setStyle("-fx-background-color: #3cb371; -fx-border-color: white;");

        drawButton = new Button("Weź kartę");
        drawButton.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
        drawButton.setOnAction(e -> client.send("ACTION:DRAW"));

        currentPlayerLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");

        infoPanel.getChildren().addAll(drawButton, currentPlayerLabel);
        root.setRight(infoPanel);

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        gameScreenInitialized = true;
    }
    // Okno dialogowe do wyboru koloru dla kart specjalnych
    private void showColorPicker() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Wybór koloru");
        alert.setHeaderText("Proszę, wybierz kolor:");

        ButtonType red = new ButtonType("RED");
        ButtonType blue = new ButtonType("BLUE");
        ButtonType green = new ButtonType("GREEN");
        ButtonType yellow = new ButtonType("YELLOW");

        alert.getButtonTypes().setAll(red, blue, green, yellow);

        alert.showAndWait().ifPresent(type -> {
            String color = type.getText();
            client.send("COLOR:" + color);
            colorPickerShown = false; // Сбрасываем флаг после выбора цвета
        });
    }

    // Aktualizacja interfejsu użytkownika na podstawie stanu gry z serwera
    private void updateUI(String jsonState) {
        Platform.runLater(() -> {
            try {
                if (!gameScreenInitialized) {
                    showGameScreen(primaryStage);
                    gameScreenInitialized = true;
                }

                JSONObject state = new JSONObject(jsonState);

                if (state.has("topCardValue") && state.has("topCardColor")) {
                    String value = state.getString("topCardValue");
                    String color = state.getString("topCardColor");
                    Image img = loadImage(getCardImagePath(value, color));
                    if (img != null) topCardView.setImage(img);
                }
                if (state.has("topCardColor") && state.getString("topCardColor").equals("BLACK")) {
                    showColorPicker();
                }
                // Обновление карт игрока
                playerHand.getChildren().clear();
                if (state.has("playerCards")) {
                    JSONArray cards = state.getJSONArray("playerCards");
                    for (int i = 0; i < cards.length(); i++) {
                        JSONObject card = cards.getJSONObject(i);
                        ImageView cardView = createCardView(
                                getCardImagePath(card.getString("value"), card.getString("color"))
                        );
                        if (cardView != null) {
                            int index = i;
                            cardView.setOnMouseClicked(e -> client.send("ACTION:" + index));
                            playerHand.getChildren().add(cardView);
                        }
                    }
                }

                if (state.has("opponentsCards")) {
                    JSONArray opponents = state.getJSONArray("opponentsCards");
                    opponentsContainer.getChildren().clear();
                    for (int i = 0; i < opponents.length(); i++) {
                        int count = opponents.getInt(i);
                        HBox hand = new HBox(3);
                        hand.setAlignment(Pos.CENTER);
                        for (int j = 0; j < count; j++) {
                            ImageView card = new ImageView(loadImage("/card_back.png"));
                            if (card != null) {
                                card.setFitWidth(60);
                                card.setPreserveRatio(true);
                                hand.getChildren().add(card);
                            }
                        }
                        opponentsContainer.getChildren().add(hand);
                    }
                }


                if (state.has("currentPlayer")) {
                    int serverCurrent = state.getInt("currentPlayer");
                    boolean isMyTurn = (serverCurrent == client.getPlayerIndex());
                    System.out.println("[CLIENT] Current player: " + serverCurrent
                            + " | My index: " + client.getPlayerIndex()
                            + " | My turn: " + isMyTurn);
                    currentPlayerLabel.setText("Aktualny: Gracz " + (serverCurrent + 1));
                    drawButton.setDisable(!isMyTurn);
                    currentPlayerLabel.setStyle(isMyTurn ? "-fx-text-fill: green;" : "-fx-text-fill: red;");

                    // Блокировка карт если не наш ход
                    playerHand.getChildren().forEach(node -> {
                        node.setDisable(!isMyTurn);
                    });
                }
                if (state.has("waitingForColor")) {
                    waitingForColor = state.getBoolean("waitingForColor");
                } else {
                    waitingForColor = false;
                }

                if (waitingForColor && !colorPickerShown) {
                    colorPickerShown = true;
                    showColorPicker();
                } else if (!waitingForColor) {
                    colorPickerShown = false;
                }


            } catch (Exception e) {
                System.err.println("Critical UI error: " + e.getMessage());
            }
        });
    }
    // Pomocnicze metody do ładowania obrazów i tworzenia widoków kart
    private String getCardImagePath(String value, String color) {
        switch (value) {
            case "DRAW_FOUR":
                return "/DRAW_FOUR-BLACK.png";
            case "DRAW_TWO":
                return "/DRAW_TWO-" + color + ".png";
            default:
                return "/" + value + "-" + color + ".png";
        }
    }


    private Image loadImage(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new FileNotFoundException("Nie znaleziono zasobu: " + path);
            return new Image(is);
        } catch (Exception e) {
            System.err.println("Błąd ładowania obrazu: " + path);
            return new Image(getClass().getResourceAsStream("/card_back.png"));
        }
    }

    private ImageView createCardView(String imagePath) {
        ImageView view = new ImageView(loadImage(imagePath));
        view.setFitWidth(100);
        view.setPreserveRatio(true);
        return view;
    }

    public static void main(String[] args) {
        launch(args);
    }
}