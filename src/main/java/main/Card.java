package main;
import java.io.Serializable;

public class Card implements Serializable {
    public final String value;
    public final String color;

    public Card(String value, String color) {
        this.value = value;
        this.color = color;
    }

}