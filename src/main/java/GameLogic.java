import java.util.*;

public class GameLogic {
    private Stack<Card> deck;
    private Stack<Card> discardPile;
    private List<Card> playerHand;
    private List<Card> botHand;
    private boolean isPlayerTurn;
    private boolean skipNextTurn;
    private boolean reverseDirection;

    public GameLogic() {
        this.deck = new Stack<>();
        this.discardPile = new Stack<>();
        this.playerHand = new ArrayList<>();
        this.botHand = new ArrayList<>();
        this.isPlayerTurn = true;
        this.skipNextTurn = false;
        this.reverseDirection = false;
        initializeGame();
    }

    private void initializeGame() {
        String[] colors = {"Czerwony", "Żółty", "Zielony", "Niebieski"};
        String[] values = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "Stop", "Zmiana", "Dobierz Dwie"};
        for (String color : colors) {
            for (String value : values) {
                deck.add(new Card(color, value));
                deck.add(new Card(color, value)); // Две карты каждого типа
            }
        }
        Collections.shuffle(deck);

        for (int i = 0; i < 7; i++) {
            playerHand.add(deck.pop());
            botHand.add(deck.pop());
        }

        discardPile.push(deck.pop());
    }

    public Card getTopCard() {
        return discardPile.peek();
    }

    public boolean playCard(Card card, List<Card> hand) {
        if (card.getColor().equals(getTopCard().getColor()) || card.getValue().equals(getTopCard().getValue())) {
            discardPile.push(card);
            hand.remove(card);

            if (card.getValue().equals("Stop")) {
                skipNextTurn = true;
            } else if (card.getValue().equals("Zmiana")) {
                reverseDirection = !reverseDirection;
                skipNextTurn = false;
                isPlayerTurn = !isPlayerTurn;
            } else if (card.getValue().equals("Dobierz Dwie")) {
                if (isPlayerTurn) {
                    drawCard(botHand);
                    drawCard(botHand);
                } else {
                    drawCard(playerHand);
                    drawCard(playerHand);
                }
                skipNextTurn = true; // Пропустить ход следующего игрока
            }

            return true;
        }
        return false;
    }

    public void drawCard(List<Card> hand) {
        if (!deck.isEmpty()) {
            hand.add(deck.pop());
        } else {
            reshuffleDeck();
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

    public boolean isGameOver() {
        return playerHand.isEmpty() || botHand.isEmpty();
    }

    public String getWinner() {
        if (playerHand.isEmpty()) return "Gracz";
        if (botHand.isEmpty()) return "Bot";
        return null;
    }

    public List<Card> getPlayerHand() {
        return playerHand;
    }

    public List<Card> getBotHand() {
        return botHand;
    }

    public boolean isPlayerTurn() {
        return isPlayerTurn;
    }

    public void switchTurn() {
        if (skipNextTurn) {
            skipNextTurn = false;
        } else {
            isPlayerTurn = !isPlayerTurn;
        }

        if (reverseDirection) {
            isPlayerTurn = !isPlayerTurn;
        }
    }

    public void displayBotCardCount() {
        System.out.println("Bot ma " + botHand.size() + " kart(y) w ręce.");
    }
}

