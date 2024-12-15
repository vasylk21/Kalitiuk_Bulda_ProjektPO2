package main;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.*;

public class GameClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static Scanner scanner = new Scanner(System.in);

    public static void startClient() {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT)) {
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Witamy w grze UNO!");

            while (true) {
                String gameState = input.readLine();
                System.out.println("Otrzymano stan gry: " + gameState);
                if (gameState.contains("Koniec gry")) {
                    System.out.println(gameState);
                    break;
                }

                displayGameState(gameState);

                System.out.print("\nWybierz swoja akcje (lub wpisz 'dobierz'): ");
                String action = scanner.nextLine();
                System.out.println("Wysylam akcje: " + action);
                output.println(action);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void displayGameState(String gameState) {
        JSONObject jsonObject = new JSONObject(gameState);
        System.out.println("\n=== Stan gry ===");
        System.out.println("Karta na stole: " + jsonObject.getString("top_card"));
        System.out.println("Twoje karty: ");
        for (int i = 0; i < jsonObject.getJSONArray("player_hand").length(); i++) {
            System.out.println(i + ": " + jsonObject.getJSONArray("player_hand").getString(i));
        }
        System.out.println("Liczba kart przeciwnika: " + jsonObject.getInt("opponent_card_count"));
    }
}
