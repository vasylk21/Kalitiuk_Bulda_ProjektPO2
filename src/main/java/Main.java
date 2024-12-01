import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        FileManager fileManager = new FileManager("results.csv");
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Menu ===");
            System.out.println("1. Start gry");
            System.out.println("2. Lista zwycięzców");
            System.out.println("3. Wyjście");
            System.out.print("Wybierz opcję: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    startGame(fileManager, scanner);
                    break;
                case "2":
                    System.out.println("\n=== Lista zwycięzców ===");
                    fileManager.readResults();
                    break;
                case "3":
                    System.out.println("Do widzenia!");
                    return;
                default:
                    System.out.println("Nieprawidłowy wybór. Spróbuj ponownie.");
            }
        }
    }

    private static void startGame(FileManager fileManager, Scanner scanner) {
        GameLogic game = new GameLogic();

        System.out.println("Witamy w grze UNO!");
        while (!game.isGameOver()) {
            System.out.println("\n=== Stan gry ===");
            delay(1000);

            System.out.println("Karta na stole: " + game.getTopCard());
            delay(1000);

            if (game.isPlayerTurn()) {
                System.out.println("Twoje karty: ");
                for (int i = 0; i < game.getPlayerHand().size(); i++) {
                    System.out.println(i + ": " + game.getPlayerHand().get(i));
                }

                System.out.println("Wybierz numer karty, którą chcesz zagrać (lub wpisz 'dobierz', aby dobrać kartę):");

                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("dobierz")) {
                    game.drawCard(game.getPlayerHand());
                } else {
                    try {
                        int cardIndex = Integer.parseInt(input);
                        if (cardIndex >= 0 && cardIndex < game.getPlayerHand().size()) {
                            Card chosenCard = game.getPlayerHand().get(cardIndex);
                            if (!game.playCard(chosenCard, game.getPlayerHand())) {
                                System.out.println("Nie możesz zagrać tej karty! Spróbuj ponownie.");
                            } else {
                                System.out.println("Zagrałeś: " + chosenCard);
                            }
                        } else {
                            System.out.println("Nieprawidłowy numer karty. Spróbuj ponownie.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Nieprawidłowe dane. Wprowadź numer karty lub 'dobierz'.");
                    }
                }
            } else {
                System.out.println("Ruch bota...");
                delay(2000);
                boolean played = false;
                for (Card card : game.getBotHand()) {
                    if (game.playCard(card, game.getBotHand())) {
                        System.out.println("Bot zagrał: " + card);
                        played = true;
                        break;
                    }
                }
                if (!played) {
                    game.drawCard(game.getBotHand());
                    System.out.println("Bot dobrał kartę.");
                }
            }
            game.displayBotCardCount();

            game.switchTurn();
        }

        String winner = game.getWinner();
        System.out.println("Koniec gry! Zwycięzca: " + winner);
        fileManager.saveResult(winner);
        System.out.println("Wyniki zapisane.");
    }

    private static void delay(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            System.err.println("Błąd podczas opóźnienia: " + e.getMessage());
        }
    }
}

