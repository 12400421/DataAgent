package com.jiayi.dataagent.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public final class CsvQualityAnalyzer {

    private static final Pattern INTEGER_PATTERN = Pattern.compile("[+-]?\\d+");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("uuuu/MM/dd").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT));

    private CsvQualityAnalyzer() {
    }

    public static DatasetQuality analyze(InputStream inputStream) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            List<String> columnNames = parser.getHeaderNames().stream()
                    .map(CsvQualityAnalyzer::removeUtf8Bom)
                    .toList();
            if (columnNames.isEmpty()) {
                throw new IllegalArgumentException("CSV file must contain a header row");
            }

            List<MutableColumnQuality> columns = columnNames.stream()
                    .map(MutableColumnQuality::new)
                    .toList();
            Set<List<String>> uniqueRows = new HashSet<>();
            long rows = 0;
            long duplicateRows = 0;

            for (CSVRecord record : parser) {
                if (record.size() != columnNames.size()) {
                    throw new IllegalArgumentException(
                            "CSV row " + record.getRecordNumber() + " has a different number of columns");
                }

                List<String> row = new ArrayList<>(record.size());
                for (int index = 0; index < record.size(); index++) {
                    String value = record.get(index);
                    row.add(value);
                    columns.get(index).accept(value);
                }
                if (!uniqueRows.add(List.copyOf(row))) {
                    duplicateRows++;
                }
                rows++;
            }

            long totalRows = rows;
            List<ColumnQuality> results = columns.stream()
                    .map(column -> column.toResult(totalRows))
                    .toList();
            return new DatasetQuality(rows, duplicateRows, results);
        }
    }

    private static String removeUtf8Bom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private static boolean isInteger(String value) {
        return INTEGER_PATTERN.matcher(value).matches();
    }

    private static boolean isDecimal(String value) {
        try {
            new BigDecimal(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private static boolean isDate(String value) {
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                formatter.parse(value);
                return true;
            } catch (DateTimeParseException ignored) {
                // Try the next supported date format.
            }
        }
        return false;
    }

    public enum ColumnType {
        INTEGER,
        DECIMAL,
        BOOLEAN,
        DATE,
        STRING
    }

    public record ColumnQuality(
            String name,
            ColumnType type,
            long missingCount,
            double missingRate,
            long uniqueCount) {
    }

    public record DatasetQuality(
            long rows,
            long duplicateRowCount,
            List<ColumnQuality> columns) {
    }

    private static final class MutableColumnQuality {

        private final String name;
        private final Set<String> uniqueValues = new HashSet<>();
        private long missingCount;
        private long nonMissingCount;
        private boolean allIntegers = true;
        private boolean allDecimals = true;
        private boolean allBooleans = true;
        private boolean allDates = true;

        private MutableColumnQuality(String name) {
            this.name = name;
        }

        private void accept(String rawValue) {
            if (rawValue == null || rawValue.isBlank()) {
                missingCount++;
                return;
            }

            String value = rawValue.trim();
            nonMissingCount++;
            uniqueValues.add(value);
            allIntegers &= isInteger(value);
            allDecimals &= isDecimal(value);
            allBooleans &= isBoolean(value);
            allDates &= isDate(value);
        }

        private ColumnQuality toResult(long totalRows) {
            double missingRate = totalRows == 0 ? 0.0 : (double) missingCount / totalRows;
            return new ColumnQuality(
                    name, inferType(), missingCount, missingRate, uniqueValues.size());
        }

        private ColumnType inferType() {
            if (nonMissingCount == 0) {
                return ColumnType.STRING;
            }
            if (allIntegers) {
                return ColumnType.INTEGER;
            }
            if (allDecimals) {
                return ColumnType.DECIMAL;
            }
            if (allBooleans) {
                return ColumnType.BOOLEAN;
            }
            if (allDates) {
                return ColumnType.DATE;
            }
            return ColumnType.STRING;
        }
    }
}
