package com.jiayi.dataagent.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public final class CsvUtils {

    private CsvUtils() {
    }

    public static CsvSummary readSummary(InputStream inputStream) throws IOException {
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

            long rows = 0;
            for (CSVRecord ignored : parser) {
                rows++;
            }
            return new CsvSummary(rows, columnNames);
        }
    }

    private static String removeUtf8Bom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    public record CsvSummary(long rows, List<String> columnNames) {
    }
}
