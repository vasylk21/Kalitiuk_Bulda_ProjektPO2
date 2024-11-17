package Dane_ulotne;

import java.util.List;

class Deck {
    private List<Card> cards;

    public void shuffle() {

    }

    public Card draw() {
        return cards.remove(cards.size() - 1);
    }
}