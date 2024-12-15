package main;

import java.io.*;
import java.time.LocalDate;

public class FileManager {
    private final String filePath;

    public FileManager(String filePath) {
        this.filePath = filePath;
    }

    public void saveResult(String winner) {
        try (FileWriter writer = new FileWriter(filePath, true)) {
            writer.write(winner + "," + LocalDate.now() + "\n");
        } catch (IOException e) {
            System.err.println("Blad podczas zapisywania wyniku: " + e.getMessage());
    }
    }

    public void readResults() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.err.println("Blad podczas odczytywania wynikow: " + e.getMessage());
        }
    }
}
