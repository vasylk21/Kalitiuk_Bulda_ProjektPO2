package main;

import java.util.*;
import com.google.gson.*;

public class GameLogic {
    private Stack<Card> deck;
    private Stack<Card> discardPile;
    private List<Card> player1Hand;
    private List<Card> player2Hand;
    private int currentPlayerIndex; // 0 - игрок 1, 1 - игрок 2
    private boolean skipNextTurn;
    private boolean reverseDirection;

    public GameLogic() {
        this.deck = new Stack<>();
        this.discardPile = new Stack<>();
        this.player1Hand = new ArrayList<>();
        this.player2Hand = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.skipNextTurn = false;
        this.reverseDirection = false;
        initializeGame();
    }

    private void initializeGame() {
        String[] colors = {"RED", "YELLOW", "GREEN", "BLUE"};
        String[] values = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "STOP", "REVERSE", "DRAW_TWO"};

        for (String color : colors) {
            for (String value : values) {
                deck.add(new Card(value, color));
                deck.add(new Card(value, color)); // Две карты каждого типа
            }
        }

        String[] wildCards = {"WILD", "DRAW_FOUR"};
        for (String wild : wildCards) {
            for (int i = 0; i < 4; i++) {
                deck.add(new Card(wild, "BLACK"));
            }
        }

        Collections.shuffle(deck);

        // Раздаем карты игрокам
        for (int i = 0; i < 7; i++) {
            player1Hand.add(deck.pop());
            player2Hand.add(deck.pop());
        }

        // Начальная карта на столе
        discardPile.push(deck.pop());
    }

    public Card getTopCard() {
        return discardPile.peek();
    }

    public List<Card> getPlayerHand(int playerIndex) {
        return playerIndex == 0 ? player1Hand : player2Hand;
    }

    public boolean playCard(int playerIndex, int cardIndex) {
        List<Card> hand = getPlayerHand(playerIndex);
        Card chosenCard = hand.get(cardIndex);

        if (chosenCard.matches(getTopCard())) {
            discardPile.push(chosenCard);
            hand.remove(cardIndex);

            // Обработка спецкарт
            switch (chosenCard.getValue()) {
                case "STOP":
                    skipNextTurn = true;
                    break;
                case "REVERSE":
                    reverseDirection = !reverseDirection;
                    break;
                case "DRAW_TWO":
                    drawCards((currentPlayerIndex + 1) % 2, 2);
                    break;
                case "DRAW_FOUR":
                    drawCards((currentPlayerIndex + 1) % 2, 4);
                    break;
            }
            return true;
        }
        return false;
    }

    public void drawCards(int playerIndex, int count) {
        List<Card> hand = getPlayerHand(playerIndex);

        for (int i = 0; i < count; i++) {
            if (deck.isEmpty()) reshuffleDeck();
            hand.add(deck.pop());
        }
    }

    private void reshuffleDeck() {
        Card topCard = discardPile.pop();
        deck.addAll(discardPile);
        discardPile.clear();
        discardPile.push(topCard);
        Collections.shuffle(deck);
    }

    public void switchTurn() {
        if (skipNextTurn) {
            skipNextTurn = false;
        } else {
            currentPlayerIndex = (reverseDirection)
                    ? (currentPlayerIndex == 0 ? 1 : 0)
                    : (currentPlayerIndex + 1) % 2;
        }
    }

    public boolean isGameOver() {
        return player1Hand.isEmpty() || player2Hand.isEmpty();
    }

    public String getWinner() {
        if (player1Hand.isEmpty()) return "Player 1";
        if (player2Hand.isEmpty()) return "Player 2";
        return null;
    }

    public String getGameState(int requestingPlayer) {
        JsonObject gameState = new JsonObject();
        gameState.addProperty("top_card", getTopCard().toString().toUpperCase());
        gameState.addProperty("current_player", currentPlayerIndex == 0 ? "Player 1" : "Player 2");

        JsonArray playerHandArray = new JsonArray();
        for (Card card : getPlayerHand(requestingPlayer)) {
            playerHandArray.add(card.toString().toUpperCase());
        }
        gameState.add("player_hand", playerHandArray);

        JsonArray opponentHandArray = new JsonArray();
        for (Card card : getPlayerHand((requestingPlayer + 1) % 2)) {
            opponentHandArray.add("CARD_BACK");
        }
        gameState.add("opponent_hand", opponentHandArray);

        gameState.addProperty("opponent_card_count", getPlayerHand((requestingPlayer + 1) % 2).size());

        return gameState.toString();
    }
}
