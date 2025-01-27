package main;

public class Card {
    private String value;
    private String color;

    public Card(String value, String color) {
        this.value = value;
        this.color = color;
    }

    public String getValue() {
        return value;
    }

    public String getColor() {
        return color;
    }

    public boolean matches(Card other) {
        return this.color.equals(other.color) || this.value.equals(other.value);
    }

    @Override
    public String toString() {
        // Сопоставляем числа с полными словами
        switch (value) {
            case "1":
                value = "ONE";
                break;
            case "2":
                value = "TWO";
                break;
            case "3":
                value = "THREE";
                break;
            case "4":
                value = "FOUR";
                break;
            case "5":
                value = "FIVE";
                break;
            case "6":
                value = "SIX";
                break;
            case "7":
                value = "SEVEN";
                break;
            case "8":
                value = "EIGHT";
                break;
            case "9":
                value = "NINE";
                break;
            case "0":
                value = "ZERO";
                break;
            // Добавьте другие специальные карты, например, "DRAW_TWO", "STOP", "REVERSE" и т.д.
        }

        return value + "-" + color; // Например: "THREE-YELLOW", "EIGHT-GREEN"
    }
}
