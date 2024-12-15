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
        String[] colors = {"Czerwony", "Zolty", "Zielony", "Niebieski"};
        String[] values = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "Stop", "Zmiana", "Dobierz Dwie"};

        for (String color : colors) {
            for (String value : values) {
                deck.add(new Card(color, value));
                deck.add(new Card(color, value)); // Две карты каждого типа
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

        if (chosenCard.getColor().equals(getTopCard().getColor()) || chosenCard.getValue().equals(getTopCard().getValue())) {
            discardPile.push(chosenCard);
            hand.remove(cardIndex);

            // Обработка спецкарт
            switch (chosenCard.getValue()) {
                case "Stop":
                    skipNextTurn = true;
                    break;
                case "Zmiana":
                    reverseDirection = !reverseDirection;
                    break;
                case "Dobierz Dwie":
                    drawCards((currentPlayerIndex + 1) % 2, 2);
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
                    ? (currentPlayerIndex + 1) % 2
                    : (currentPlayerIndex == 0 ? 1 : 0);
        }
    }

    public boolean isGameOver() {
        return player1Hand.isEmpty() || player2Hand.isEmpty();
    }

    public String getWinner() {
        if (player1Hand.isEmpty()) return "Gracz 1";
        if (player2Hand.isEmpty()) return "Gracz 2";
        return null;
    }

    public String getGameState(int requestingPlayer) {
        JsonObject gameState = new JsonObject();
        gameState.addProperty("top_card", getTopCard().toString());
        gameState.addProperty("current_player", currentPlayerIndex == 0 ? "Gracz 1" : "Gracz 2");

        JsonArray playerHandArray = new JsonArray();
        for (Card card : getPlayerHand(requestingPlayer)) {
            playerHandArray.add(card.toString());
        }
        gameState.add("player_hand", playerHandArray);

        gameState.addProperty("opponent_card_count", getPlayerHand((requestingPlayer + 1) % 2).size());

        return gameState.toString();
    }
}
