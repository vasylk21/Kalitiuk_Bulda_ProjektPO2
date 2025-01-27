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

            // Отправляем сообщение о старте игры обоим игрокам
            for (PrintWriter output : outputs) {
                output.println("START_GAME");
            }

            // Отправляем начальное состояние игры обоим игрокам
            for (int i = 0; i < 2; i++) {
                sendGameState(i);
            }

            // Основной игровой цикл
            while (!game.isGameOver()) {
                for (int i = 0; i < 2; i++) {
                    // Ждем действия от игрока
                    String action = inputs.get(i).readLine();
                    if (action == null) continue;
                    System.out.println("Игрок " + (i + 1) + " выбрал действие: " + action);

                    if (action.equals("dobierz")) {
                        game.drawCards(i, 1);
                    } else {
                        try {
                            int cardIndex = Integer.parseInt(action);
                            if (!game.playCard(i, cardIndex)) {
                                outputs.get(i).println("Неправильный ход.");
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            outputs.get(i).println("Неправильные данные.");
                            continue;
                        }
                    }

                    // Проверка окончания игры
                    if (game.isGameOver()) {
                        break;
                    }

                    // Переключение хода
                    game.switchTurn();

                    // Отправляем обновленное состояние игры обоим игрокам
                    for (int j = 0; j < 2; j++) {
                        sendGameState(j);
                    }
                }
            }

            // Уведомляем о завершении игры
            String winner = game.getWinner();
            for (PrintWriter output : outputs) {
                output.println("Koniec gry! Победитель: " + winner);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendGameState(int playerIndex) {
        String gameState = game.getGameState(playerIndex);
        outputs.get(playerIndex).println(gameState);
        System.out.println("Отправлено состояние игры игроку " + (playerIndex + 1) + ": " + gameState); // Добавлено логирование
    }

    public static void main(String[] args) {
        startServer();
    }
}
