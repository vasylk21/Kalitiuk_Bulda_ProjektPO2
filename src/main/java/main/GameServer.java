package main;

import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final int PORT = 12345;
    private static GameLogic game = new GameLogic();
    private static List<Socket> playerSockets = new ArrayList<>();
    private static List<BufferedReader> inputs = new ArrayList<>();
    private static List<PrintWriter> outputs = new ArrayList<>();

    public static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serwer oczekuje na graczy...");

            // Подключаем двух игроков
            for (int i = 0; i < 2; i++) {
                Socket playerSocket = serverSocket.accept();
                playerSockets.add(playerSocket);
                inputs.add(new BufferedReader(new InputStreamReader(playerSocket.getInputStream())));
                outputs.add(new PrintWriter(playerSocket.getOutputStream(), true));
                System.out.println("Gracz " + (i + 1) + " polaczony!");
            }

            // Основной игровой цикл
            while (!game.isGameOver()) {
                for (int i = 0; i < 2; i++) {
                    sendGameState(i);

                    // Ожидаем действия игрока
                    String action = inputs.get(i).readLine();
                    System.out.println("Otrzymano akcje od gracza " + (i + 1) + ": " + action);
                    if (action.equals("dobierz")) {
                        game.drawCards(i, 1);
                    } else {
                        try {
                            int cardIndex = Integer.parseInt(action);
                            if (!game.playCard(i, cardIndex)) {
                                outputs.get(i).println("Nieprawidlowy ruch.");
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            outputs.get(i).println("Nieprawidlowe dane.");
                            continue;
                        }
                    }

                    if (game.isGameOver()) break;

                    game.switchTurn();
                }
            }

            // Игра закончена, отправляем результат
            String winner = game.getWinner();
            for (PrintWriter output : outputs) {
                output.println("Koniec gry! Zwyciezca: " + winner);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendGameState(int playerIndex) {
        String gameState = game.getGameState(playerIndex);
        outputs.get(playerIndex).println(gameState);
    }
}
