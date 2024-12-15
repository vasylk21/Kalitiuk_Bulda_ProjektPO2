package main;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0) {
            String mode = args[0];

            if ("server".equalsIgnoreCase(mode)) {
                // Запуск сервера
                GameServer.startServer();
            } else if ("client".equalsIgnoreCase(mode)) {
                // Запуск клиента
                GameClient.startClient();
            } else {
                System.out.println("Nieprawidlowy argument. Uzyj 'server' lub 'client'.");
            }
        } else {
            System.out.println("Prosze podac 'server' lub 'client' jako argument.");
        }
    }
}
