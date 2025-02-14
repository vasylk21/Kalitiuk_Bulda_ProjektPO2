package main;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    private final String filePath;

    public FileManager(String filePath) {
        this.filePath = filePath;
        initFile();
    }

    private void initFile() {
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
                saveResult("Zwycięzca, Data, Użyte karty specjalne");
            } catch (IOException e) {
                System.err.println("Błąd tworzenia pliku: " + e.getMessage());
            }
        }
    }

    public void saveResult(String data) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {
            pw.println(data);
        } catch (IOException e) {
            System.err.println("Ошибка записи: " + e.getMessage());
        }
    }


    public List<String> getResults() {
        List<String> results = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                results.add(line);
            }
        } catch (IOException e) {
            System.err.println("Ошибка чтения: " + e.getMessage());
        }
        return results;
    }
}