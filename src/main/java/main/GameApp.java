package main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.FileNotFoundException;

public class GameApp extends Application {
    private Stage primaryStage;
    private GameClient client;
    private ImageView[] opponentViews; // Widoki kart przeciwników
    private HBox playerHand = new HBox(10); // Kontener na karty gracza
    private Label currentPlayerLabel = new Label(); // Etykieta z informacją o obecnym graczu
    private ImageView topCard = new ImageView(); // Widok karty na wierzchu stosu
    private Button btnDraw = new Button("Weź kartę"); // Przycisk do dobierania kart
    private HBox opponentsBox = new HBox(10); // Kontener na karty przeciwników

    @Override
    public void start(Stage stage) {
        primaryStage = stage;

        // Główna struktura UI
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-image: url('/background.png'); -fx-background-size: cover;");

        // Logo gry
        ImageView logo = new ImageView(loadImage("/logo.png"));
        logo.setFitWidth(200);
        logo.setPreserveRatio(true);

        // Przyciski do tworzenia gry lub dołączania do istniejącej
        Button btnCreate = new Button("Utwórz grę");
        Button btnJoin = new Button("Dołącz do gry");
        TextField txtGameId = new TextField();
        txtGameId.setPromptText("ID gry");

        // Menu główne
        HBox menu = new HBox(10, btnCreate, txtGameId, btnJoin);
        menu.setAlignment(Pos.CENTER);

        root.getChildren().addAll(logo, menu);

        // Akcja po kliknięciu przycisku "Utwórz grę"
        btnCreate.setOnAction(e -> {
            client.createGame(txtGameId.getText());
            showWaitingScreen(stage);
        });

        // Akcja po kliknięciu przycisku "Dołącz do gry"
        btnJoin.setOnAction(e -> {
            client.joinGame(txtGameId.getText());
            showWaitingScreen(stage);
        });

        // Inicjalizacja klienta
        client = new GameClient();
        client.setOnGameStart(() -> Platform.runLater(() -> showGameScreen(primaryStage))); // Obsługuje początek gry
        client.setOnGameState(this::updateUI); // Aktualizacja UI na podstawie stanu gry
        client.connect(); // Połączenie z serwerem

        // Ustawienia sceny
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("UNO");
        stage.setScene(scene);
        stage.show();
    }

    // Wyświetlenie ekranu oczekiwania na graczy
    private void showWaitingScreen(Stage stage) {
        VBox waiting = new VBox(20);
        waiting.setAlignment(Pos.CENTER);

        ProgressIndicator progress = new ProgressIndicator();
        Label label = new Label("Oczekiwanie na graczy...");

        waiting.getChildren().addAll(progress, label);
        stage.setScene(new Scene(waiting, 800, 600));
    }

    // Wyświetlenie ekranu gry
    private void showGameScreen(Stage stage) {
        VBox gameRoot = new VBox(20);
        gameRoot.setAlignment(Pos.CENTER);

        // Ustawienie rozmiaru karty na wierzchu stosu
        topCard.setFitWidth(150);
        topCard.setPreserveRatio(true);

        // Etykieta z obecnym graczem
        currentPlayerLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: white;");

        // Kontener na karty przeciwników
        opponentsBox.getChildren().clear();
        opponentsBox.setAlignment(Pos.CENTER);

        // Gwarantujemy utworzenie widoków przeciwników przed aktualizacją UI
        opponentViews = new ImageView[3];
        for (int i = 0; i < opponentViews.length; i++) {
            opponentViews[i] = createCardView("/card_back.png");
            opponentsBox.getChildren().add(opponentViews[i]);
        }

        // Ustawienie kontenera na karty gracza
        playerHand.setAlignment(Pos.CENTER);

        // Akcja po kliknięciu przycisku "Weź kartę"
        btnDraw.setOnAction(e -> client.send("ACTION:DRAW"));

        // Dodanie wszystkich elementów do głównego kontenera
        gameRoot.getChildren().addAll(currentPlayerLabel, opponentsBox, topCard, playerHand, btnDraw);

        // Ustawienie sceny gry
        Scene gameScene = new Scene(gameRoot, 800, 600);
        stage.setScene(gameScene);
        stage.show();
    }

    // Aktualizacja UI na podstawie stanu gry
    private void updateUI(String jsonState) {
        Platform.runLater(() -> {
            try {
                // Parsowanie stanu gry z JSON-a
                JSONObject state = new JSONObject(jsonState);

                // 1. Aktualizacja karty na wierzchu
                if (state.has("topCardValue") && !state.isNull("topCardValue")
                        && state.has("topCardColor") && !state.isNull("topCardColor")) {
                    String topCardValue = state.getString("topCardValue");
                    String topCardColor = state.getString("topCardColor");
                    topCard.setImage(loadImage(getCardImagePath(topCardValue, topCardColor)));
                } else {
                    topCard.setImage(loadImage("/card_back.png"));
                }

                // 2. Aktualizacja kart przeciwników
                JSONArray opponentsCards = state.optJSONArray("opponentsCards");
                if (opponentsCards != null) {
                    opponentsBox.getChildren().clear();
                    for (int i = 0; i < opponentsCards.length(); i++) {
                        int cardCount = opponentsCards.getInt(i);
                        HBox opponentHand = new HBox(2);
                        for (int j = 0; j < cardCount; j++) {
                            opponentHand.getChildren().add(createCardView("/card_back.png"));
                        }
                        opponentsBox.getChildren().add(opponentHand);
                    }
                }

                // 3. Aktualizacja kart gracza
                playerHand.getChildren().clear();
                if (state.has("playerCards")) {
                    JSONArray playerCards = state.getJSONArray("playerCards");
                    for (int i = 0; i < playerCards.length(); i++) {
                        JSONObject cardJson = playerCards.getJSONObject(i);
                        String value = cardJson.getString("value");
                        String color = cardJson.getString("color");
                        ImageView cardView = createCardView(getCardImagePath(value, color));
                        int cardIndex = i;
                        cardView.setOnMouseClicked(e -> client.send("ACTION:PLAY:" + cardIndex));
                        playerHand.getChildren().add(cardView);
                    }
                }

                // 4. Aktualizacja obecnego gracza
                if (state.has("currentPlayer")) {
                    int currentPlayerIndex = state.getInt("currentPlayer");
                    currentPlayerLabel.setText("Teraz gra: Gracz " + (currentPlayerIndex + 1));
                    btnDraw.setDisable(currentPlayerIndex != 0);
                }

            } catch (Exception e) {
                System.err.println("Błąd aktualizacji UI: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // Generowanie ścieżki do obrazu karty
    private String getCardImagePath(String value, String color) {
        return "/" + value + "-" + color + ".png";
    }

    // Ładowanie obrazu z zasobów
    private Image loadImage(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new FileNotFoundException("Nie znaleziono zasobu: " + path);
            return new Image(is);
        } catch (Exception e) {
            System.err.println("Błąd ładowania obrazu: " + path);
            return new Image(getClass().getResourceAsStream("/card_back.png"));
        }
    }

    // Tworzenie widoku karty na podstawie ścieżki
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
