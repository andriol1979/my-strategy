package com.vut.mystrategy.helper;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CombineAndFilterCsvFiles {
    public static void main(String[] args) {
        String folderPath1 = "/Users/vutran/Downloads/abc001";
        String folderPath2 = "/Users/vutran/Downloads/abc002";
        String folderPath3 = "/Users/vutran/Downloads/abc003";
        String fileName = "/Users/vutran/Downloads/abc_result/combined_btcusdt_15m"; // File đầu ra

        String[] folderPaths = new String[]{folderPath1, folderPath2, folderPath3};

        try {
            for(int index = 0; index < folderPaths.length; index++) {
                String path = folderPaths[index];
                String outputFile = fileName + "_" + index + ".csv";
                System.out.println("Bắt đầu gộp tất cả file CSV từ: " + path + " thành: " + outputFile);
                combineAndFilterCsvFiles(path, outputFile, 900);
                System.out.println("Đã gộp tất cả file CSV từ: " + path + " thành: " + outputFile);
            }
        }
        catch (IOException e) {
            System.err.println("Lỗi khi gộp file: " + e.getMessage());
        }
    }

    public static void combineAndFilterCsvFiles(String folderPath, String outputFile, long targetIntervalSeconds) throws IOException {
        File folder = new File(folderPath);
        File[] csvFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

        if (csvFiles == null || csvFiles.length == 0) {
            throw new IOException("Không tìm thấy file CSV trong folder: " + folderPath);
        }

        // Định dạng timestamp
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        String header = "event_time,open,high,low,close,volume,exchange_name,symbol,kline_interval";
        List<String[]> allRecords = new ArrayList<>(); // Lưu tất cả bản ghi trước khi lọc

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
        Collections.sort(allRecords, (a, b) -> {
            LocalDateTime timeA = LocalDateTime.parse(a[0], inputFormatter);
            LocalDateTime timeB = LocalDateTime.parse(b[0], inputFormatter);
            return timeA.compareTo(timeB);
        });
        System.out.println("Sorted total " + allRecords.size() + " records");

        // Lọc dữ liệu cách nhau ~5 phút
        List<String[]> filteredRecords = new ArrayList<>();
        LocalDateTime lastKept = null;
        long minInterval = targetIntervalSeconds - 3; // Dung sai: 290 giây
        long maxInterval = targetIntervalSeconds + 3; // Dung sai: 310 giây

        System.out.println("Starting filter data...");
        int counter = 0;
        int totalRecords = 0;
        for (int index = 0; index < allRecords.size(); index ++) {
            String[] record = allRecords.get(index);
            LocalDateTime currentTime = LocalDateTime.parse(record[0], inputFormatter);

            if (lastKept == null) {
                // Giữ bản ghi đầu tiên
                filteredRecords.add(record);
                lastKept = currentTime;
            }
            else {
                long secondsDiff = ChronoUnit.SECONDS.between(lastKept, currentTime);
                if (secondsDiff >= minInterval && secondsDiff <= maxInterval) {
                    // Giữ bản ghi nếu cách ~5 phút
                    filteredRecords.add(record);
                    lastKept = currentTime;
                }
                else if (secondsDiff > maxInterval) {
                    // Nếu vượt quá 5 phút, tìm bản ghi gần nhất trong khoảng trước đó
                    //lấy sublist để improve performance: index - 5
                    List<String[]> subList = allRecords.subList(index < 100 ? 0 : index - 100, index > allRecords.size() - 100 ? allRecords.size() : index + 100);
                    String[] closest = findClosestRecord(subList, lastKept, record, targetIntervalSeconds);
                    if (closest != null && !containsRecord(filteredRecords, closest)) {
                        filteredRecords.add(closest);
                        lastKept = LocalDateTime.parse(closest[0], inputFormatter);
                    }
                }
            }
            counter++;
            if (counter % 1000 == 0) {
                totalRecords += counter;
                System.out.println("Processed " + totalRecords + " records in " + allRecords.size() + " records");
                //reset counter
                counter = 0;
            }
        }

        System.out.println("Writing output file...");
        // Ghi file đầu ra
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(header);
            writer.newLine();

            for (String[] record : filteredRecords) {
                String newTimestamp = LocalDateTime.parse(record[0], inputFormatter).format(outputFormatter);
                String newLine = String.join(",",
                        newTimestamp,
                        record[1], record[2], record[3], record[4],
                        record[5], "binance", "btcusdt", "15m"
                );
                writer.write(newLine);
                writer.newLine();
            }
        }
        System.out.println("Wrote output file at: " + outputFile);
    }

    // Tìm bản ghi gần 5 phút nhất
    private static String[] findClosestRecord(List<String[]> records, LocalDateTime lastKept, String[] current, long targetIntervalSeconds) {
        String[] closest = null;
        long minDiff = Long.MAX_VALUE;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS");

        for (String[] record : records) {
            LocalDateTime candidateTime = LocalDateTime.parse(record[0], formatter);
            if (candidateTime.isAfter(lastKept) && candidateTime.isBefore(LocalDateTime.parse(current[0], formatter))) {
                long secondsDiff = ChronoUnit.SECONDS.between(lastKept, candidateTime);
                long diffFromTarget = Math.abs(secondsDiff - targetIntervalSeconds);
                if (diffFromTarget < minDiff) {
                    minDiff = diffFromTarget;
                    closest = record;
                    break;
                }
            }
        }
        return closest;
    }

    // Kiểm tra xem bản ghi đã có trong danh sách chưa
    private static boolean containsRecord(List<String[]> records, String[] target) {
        for (String[] record : records) {
            if (Arrays.equals(record, target)) {
                return true;
            }
        }
        return false;
    }
}
