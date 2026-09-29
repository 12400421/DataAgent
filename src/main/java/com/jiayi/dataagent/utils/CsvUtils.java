package com.jiayi.dataagent.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public final class CsvUtils {

    private CsvUtils() {
    }

    public static CsvSummary readSummary(InputStream inputStream) throws IOException {
        CsvData csvData = read(inputStream, 0);
        return new CsvSummary(csvData.rows(), csvData.columnNames());
    }

    public static CsvData read(InputStream inputStream, int previewLimit) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = csvFormat.parse(reader)) {
            List<String> columnNames = parser.getHeaderNames();
            if (columnNames.isEmpty()) {
                throw new IllegalArgumentException("CSV file must contain a header row");
            }

            columnNames = columnNames.stream()
                    .map(CsvUtils::removeUtf8Bom)
                    .toList();
            validateColumnNames(columnNames);

            long rows = 0;
            List<Map<String, String>> preview = new java.util.ArrayList<>();
            for (CSVRecord record : parser) {
                if (record.size() != columnNames.size()) {
                    throw new IllegalArgumentException(
                            "CSV row " + record.getRecordNumber() + " has a different number of columns");
                }
                rows++;
                if (preview.size() < previewLimit) {
                    Map<String, String> values = new LinkedHashMap<>();
                    for (int index = 0; index < columnNames.size(); index++) {
                        values.put(columnNames.get(index), record.get(index));
                    }
                    preview.add(values);
                }
            }
            return new CsvData(rows, columnNames, List.copyOf(preview));
        }
    }

    private static void validateColumnNames(List<String> columnNames) {
        if (columnNames.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("CSV column names must not be blank");
        }

        Set<String> uniqueNames = new HashSet<>(columnNames);
        if (uniqueNames.size() != columnNames.size()) {
            throw new IllegalArgumentException("CSV column names must be unique");
        }
    }

    private static String removeUtf8Bom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    public record CsvSummary(long rows, List<String> columnNames) {
    }

    public record CsvData(
            long rows,
            List<String> columnNames,
            List<Map<String, String>> preview) {
    }
}
