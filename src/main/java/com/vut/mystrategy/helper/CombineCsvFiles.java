package com.vut.mystrategy.helper;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CombineCsvFiles {
    public static void main(String[] args) {
        String folderPath1 = "/Users/vutran/Downloads/abc001";
        String folderPath2 = "/Users/vutran/Downloads/abc002";
        String folderPath3 = "/Users/vutran/Downloads/abc003";
        String fileName = "/Users/vutran/Downloads/abc_result/combined_btcusdt_10m"; // File đầu ra

        String[] folderPaths = new String[]{folderPath1, folderPath2, folderPath3};

        try {
            for(int index = 0; index < folderPaths.length; index++) {
                String path = folderPaths[index];
                String outputFile = fileName + "_" + index + ".csv";
                System.out.println("Bắt đầu gộp tất cả file CSV từ: " + path + " thành: " + outputFile);
                combineCsvFiles(path, outputFile);
                System.out.println("Đã gộp tất cả file CSV từ: " + path + " thành: " + outputFile);
            }
        }
        catch (IOException e) {
            System.err.println("Lỗi khi gộp file: " + e.getMessage());
        }
    }

    public static void combineCsvFiles(String folderPath, String outputFile) throws IOException {
        File folder = new File(folderPath);
        File[] csvFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

        if (csvFiles == null || csvFiles.length == 0) {
            throw new IOException("Không tìm thấy file CSV trong folder: " + folderPath);
        }

        // Định dạng timestamp
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        String header = "event_time,open,high,low,close,volume,exchange_name,symbol,kline_interval";
        Set<String[]> allRecords = new HashSet<>(); // dùng hashset để loại duplicate

        // Đọc và gộp tất cả file CSV
        for (File csvFile : csvFiles) {
            try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    if (isFirstLine) {
                        isFirstLine = false;
                        continue; // Bỏ qua header
                    }

                    String[] columns = line.split(",");
                    if (columns.length < 6) {
                        System.out.println("Dòng lỗi trong file " + csvFile.getName() + ": " + line);
                        continue;
                    }

                    allRecords.add(columns);
                }
            }
            System.out.println("Loaded file " + csvFile.getName());
        }
        System.out.println("Loaded total " + allRecords.size() + " records");

        // Sắp xếp theo event_time
        allRecords.stream().sorted((a, b) -> {
            LocalDateTime timeA = LocalDateTime.parse(a[0], inputFormatter);
            LocalDateTime timeB = LocalDateTime.parse(b[0], inputFormatter);
            return timeA.compareTo(timeB);
        });
        System.out.println("Sorted total " + allRecords.size() + " records");

        System.out.println("Writing output file...");
        // Ghi file đầu ra
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(header);
            writer.newLine();

            for (String[] record : allRecords) {
                String newTimestamp = LocalDateTime.parse(record[0], inputFormatter).format(outputFormatter);
                String newLine = String.join(",",
                        newTimestamp,
                        record[1], record[2], record[3], record[4],
                        record[5], "binance", "btcusdt", "5m"
                );
                writer.write(newLine);
                writer.newLine();
            }
        }
        System.out.println("Wrote output file at: " + outputFile);
    }
}
