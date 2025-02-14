package main;
import java.io.Serializable;

// Klasa reprezentująca kartę do gry, implementuje Serializable do zapisu stanu
public class Card implements Serializable {
    private static final long serialVersionUID = 1L;  // Wersja do serializacji
    public String value;       // Wartość karty (np. "DRAW_TWO", "FIVE")
    public String color;       // Kolor karty (np. "RED", "BLACK")
    // Konstruktor tworzący nową kartę
    public Card(String value, String color) {
        this.value = value;
        this.color = color;
    }

}