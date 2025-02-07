package main;

import com.google.gson.*;
import java.io.*;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

public class GameLogic implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<List<Card>> playerHands;
    private Stack<Card> deck;
    private Stack<Card> discardPile;
    private int currentPlayer;
    private boolean direction;
    private Card topCard;

    public GameLogic() {
        playerHands = new ArrayList<>();
        deck = new Stack<>();
        discardPile = new Stack<>();
        direction = true;
    }

    public void initialize(int players) {
        playerHands.clear();
        initializeDeck();
        for (int i = 0; i < players; i++) {
            List<Card> hand = new ArrayList<>();
            for (int j = 0; j < 7; j++) {
                hand.add(deck.pop());
            }
            playerHands.add(hand);
        }
        do {
            topCard = deck.pop();
        } while (topCard.value.startsWith("DRAW")); // Wykluczamy "DRAW_TWO" i "DRAW_FOUR"

        discardPile.push(topCard);
        currentPlayer = 0;
    }

    private void initializeDeck() {
        deck.clear();
        discardPile.clear();

        String[] colors = {"RED", "YELLOW", "GREEN", "BLUE"};
        String[] values = {"ZERO", "ONE", "TWO", "THREE", "FOUR", "FIVE",
                "SIX", "SEVEN", "EIGHT", "NINE"};

        for (String color : colors) {
            for (String value : values) {
                deck.add(new Card(value, color));
                if (!value.equals("ZERO")) {
                    deck.add(new Card(value, color));
                }
                deck.add(new Card("DRAW_TWO", color));
            }
        }

        for (int i = 0; i < 4; i++) {
            deck.add(new Card("DRAW_FOUR", "BLACK"));
        }

        Collections.shuffle(deck);
    }

    public void handleAction(int playerIndex, String action) {
        if (playerIndex != currentPlayer) return;

        if (action.equals("DRAW")) {
            drawCard(playerIndex);
            nextTurn();
        } else {
            try {
                playCard(playerIndex, Integer.parseInt(action));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Niepoprawne działanie: " + action);
            }
        }
    }

    private void drawCard(int playerIndex) {
        if (deck.isEmpty()) {
            reshuffleDiscardPile();
        }
        if (!deck.isEmpty()) {
            playerHands.get(playerIndex).add(deck.pop());
        }
    }

    private void playCard(int playerIndex, int cardIndex) {
        List<Card> hand = playerHands.get(playerIndex);
        if (cardIndex < 0 || cardIndex >= hand.size()) {
            throw new IllegalArgumentException("Błąd: niepoprawny indeks karty");
        }

        Card played = hand.get(cardIndex);
        if (isValidPlay(played)) {
            discardPile.push(played);
            hand.remove(cardIndex);
            topCard = played;
            applyCardEffect(played);
            nextTurn();
        } else {
            throw new IllegalArgumentException("Nie można zagrać tej karty");
        }
    }

    private boolean isValidPlay(Card played) {
        if (discardPile.isEmpty()) return true;

        Card top = discardPile.peek();
        return played.color.equals(top.color) ||
                played.value.equals(top.value) ||
                played.color.equals("BLACK");
    }

    private void applyCardEffect(Card card) {
        switch (card.value) {
            case "DRAW_TWO":
                drawCards(nextPlayer(), 2);
                break;
            case "DRAW_FOUR":
                drawCards(nextPlayer(), 4);
                break;
        }
        nextTurn();
    }

    private void nextTurn() {
        currentPlayer = (currentPlayer + 1) % playerHands.size();
    }

    private int nextPlayer() {
        return (currentPlayer + 1) % playerHands.size();
    }

    private void drawCards(int player, int count) {
        for (int i = 0; i < count; i++) {
            if (deck.isEmpty()) reshuffleDiscardPile();
            if (!deck.isEmpty()) playerHands.get(player).add(deck.pop());
        }
    }

    private void reshuffleDiscardPile() {
        if (discardPile.size() <= 1) return;
        Card top = discardPile.pop();
        List<Card> newDeck = new ArrayList<>(discardPile);
        Collections.shuffle(newDeck);
        deck.addAll(newDeck);
        discardPile.clear();
        discardPile.push(top);
    }

    public String getGameState(int playerIndex) {
        JSONObject state = new JSONObject();

        try {
            // 1. Aktualna karta
            if (!discardPile.isEmpty()) {
                Card top = discardPile.peek();
                state.put("topCardValue", top.value);
                state.put("topCardColor", top.color);
            }

            // 2. Karty gracza
            JSONArray playerCards = new JSONArray();
            if (playerIndex >= 0 && playerIndex < playerHands.size()) {
                for (Card card : playerHands.get(playerIndex)) {
                    JSONObject cardJson = new JSONObject();
                    cardJson.put("value", card.value);
                    cardJson.put("color", card.color);
                    playerCards.put(cardJson);
                }
            }
            state.put("playerCards", playerCards);

            // 3. Karty przeciwników (tylko ilość)
            JSONArray opponents = new JSONArray();
            for (int i = 0; i < playerHands.size(); i++) {
                if (i != playerIndex) {
                    opponents.put(playerHands.get(i).size());
                }
            }
            state.put("opponentsCards", opponents);

            // 4. Aktualny gracz
            state.put("currentPlayer", currentPlayer);

        } catch (JSONException e) {
            System.err.println("Błąd podczas formowania stanu gry: " + e.getMessage());
        }

        return state.toString();
    }

    public void copyFrom(GameLogic other) {
        this.playerHands = new ArrayList<>(other.playerHands);
        this.deck = (Stack<Card>) other.deck.clone();
        this.discardPile = (Stack<Card>) other.discardPile.clone();
        this.currentPlayer = other.currentPlayer;
        this.direction = other.direction;
        this.topCard = other.topCard;
    }

}
