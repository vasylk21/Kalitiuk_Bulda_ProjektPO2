package Dane_ulotne;

import java.util.List;

public class GameState {
    private List<Player>;
    private Deck drawPile;
    private Deck discardPile;
    private int currentPlayerIndex;
    private boolean directionClockwise;
    private Card currentCard;


    public void nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + (directionClockwise ? 1 : -1)) % players.size();
    }
}
