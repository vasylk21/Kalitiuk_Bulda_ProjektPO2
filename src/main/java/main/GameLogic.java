package main;

import java.io.*;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;
// Główna logika gry - zasady, stan gry, mechanika
public class GameLogic implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<List<Card>> playerHands;
    private List<String> usedSpecialCards = new ArrayList<>();
    private Stack<Card> deck;
    private Stack<Card> discardPile;
    private int currentPlayer;
    private boolean direction;
    private Card topCard;
    private boolean waitingForColor = false;
    /* Inicjalizacja */
    public GameLogic() {
        playerHands = new ArrayList<>();
        deck = new Stack<>();
        discardPile = new Stack<>();
        direction = true;
    }
    /* Przygotowanie nowej gry */
    public void initialize(int players) {
        playerHands.clear();
        initializeDeck();
        currentPlayer = 0;
        System.out.println("[GAME] Game initialized. First player: " + currentPlayer);

        int requiredCards = players * 7 + 1;
        if (deck.size() < requiredCards) {
            throw new IllegalStateException("Not enough cards in deck. Required: " + requiredCards + ", available: " + deck.size());
        }

        for (int i = 0; i < players; i++) {
            List<Card> hand = new ArrayList<>();
            for (int j = 0; j < 7; j++) {
                hand.add(deck.pop());
            }
            playerHands.add(hand);
        }

        while (true) {
            Card potentialTop = deck.pop();
            if (!potentialTop.value.startsWith("DRAW")) {
                topCard = potentialTop;
                discardPile.push(topCard);
                break;
            }
            deck.push(potentialTop);
        }

        currentPlayer = 0;
    }
    /* Tworzenie i tasowanie talii */
    private void initializeDeck() {
        deck.clear();
        discardPile.clear();

        String[] colors = {"RED", "YELLOW", "GREEN", "BLUE"};
        String[] numbers = {"ZERO", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE"};
        String[] actions = {"DRAW_TWO"};

        for (String color : colors) {
            deck.add(new Card("ZERO", color));

            for (String number : numbers) {
                if (number.equals("ZERO")) continue;
                deck.add(new Card(number, color));
                deck.add(new Card(number, color));
            }

            for (String action : actions) {
                deck.add(new Card(action, color));
                deck.add(new Card(action, color));
            }
        }

        for (int i = 0; i < 4; i++) {
            deck.add(new Card("DRAW_FOUR", "BLACK"));
        }

        Collections.shuffle(deck);
    }
    // Metody obsługi akcji gracza
    public void handleAction(int playerIndex, String action) {
        if (playerIndex != currentPlayer) {
            throw new IllegalStateException("Nie twoja kolej!");
        }

        if (action.equals("DRAW")) {
            drawCard(playerIndex);
            nextTurn();
        } else {
            try {
                int cardIndex = Integer.parseInt(action);
                playCard(playerIndex, cardIndex);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Nieprawidłowa akcja: " + action);
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

    public void playCard(int playerIndex, int cardIndex) {
        List<Card> hand = playerHands.get(playerIndex);
        if (cardIndex < 0 || cardIndex >= hand.size()) {
            throw new IllegalArgumentException("Błąd: nieprawidłowy indeks karty");
        }

        Card played = hand.get(cardIndex);
        if (isValidPlay(played)) {
            discardPile.push(played);
            hand.remove(cardIndex);
            topCard = played;

            applyCardEffect(played);

            if (!waitingForColor) {
                nextTurn();
            }
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
    public void chooseColor(String color) {
        if (!waitingForColor) {
            throw new IllegalStateException("Wybór koloru nie jest teraz wymagany");
        }
        if (!Arrays.asList("RED", "YELLOW", "GREEN", "BLUE").contains(color)) {
            throw new IllegalArgumentException("Nieprawidłowy kolor: " + color);
        }
        topCard.color = color;
        waitingForColor = false;
        nextTurn();
    }

    // Efekty specjalne kart
    private void applyCardEffect(Card card) {

        if (card.value.equals("DRAW_TWO") || card.value.equals("DRAW_FOUR")) {
            usedSpecialCards.add(card.value);
        }
            switch (card.value) {
            case "DRAW_TWO":
                int targetPlayer = nextPlayer();
                drawCards(targetPlayer, 2);
                skipNextPlayer();
                break;
            case "DRAW_FOUR":
                waitingForColor = true;
                int targetPlayerFour = nextPlayer();
                drawCards(targetPlayerFour, 4);
                skipNextPlayer();
                break;
            default:
                break;

        }


    }
    public String getUsedSpecialCards() {
        return String.join(", ", usedSpecialCards);
    }


    // GameLogic.java
    public void nextTurn() {
        currentPlayer = (currentPlayer + 1) % playerHands.size();
        System.out.println("[GAME] Turn passed to player: " + currentPlayer);
    }

    public int nextPlayer() {
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
    // Zarządzanie stanem gry
    public String getGameState(int playerIndex) {
        /* Generowanie JSON ze stanem */
        JSONObject state = new JSONObject();

        try {
            if (!discardPile.isEmpty()) {
                Card top = discardPile.peek();
                state.put("topCardValue", top.value);
                state.put("topCardColor", top.color);
            }

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

            JSONArray opponents = new JSONArray();
            for (int i = 0; i < playerHands.size(); i++) {
                if (i != playerIndex) {
                    opponents.put(playerHands.get(i).size());
                }
            }
            state.put("opponentsCards", opponents);

            state.put("currentPlayer", currentPlayer);

        } catch (JSONException e) {
            System.err.println("Błąd podczas formowania stanu gry: " + e.getMessage());
        }

        return state.toString();
    }
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(int index) {
        currentPlayer = index % playerHands.size();
    }
    private void skipNextPlayer() {
        currentPlayer = (currentPlayer + 1) % playerHands.size();
        System.out.println("[GRA] Pomijamy następnego gracza, aktualny gracz: " + currentPlayer);
    }

    public boolean hasWinner() {
        return playerHands.stream().anyMatch(List::isEmpty);
    }
    public Optional<Integer> getWinningPlayerIndex() {
        for (int i = 0; i < playerHands.size(); i++) {
            if (playerHands.get(i).isEmpty()) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
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
