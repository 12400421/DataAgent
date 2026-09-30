package com.jiayi.dataagent.utils;

import com.jiayi.dataagent.utils.CsvQualityAnalyzer.ColumnType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public final class CsvProfileAnalyzer {

    private static final int TOP_VALUE_LIMIT = 10;
    private static final Pattern INTEGER_PATTERN = Pattern.compile("[+-]?\\d+");
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("uuuu/MM/dd").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT));

    private CsvProfileAnalyzer() {
    }

    public static DatasetProfile analyze(InputStream inputStream) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {
            List<String> columnNames = parser.getHeaderNames().stream()
                    .map(CsvProfileAnalyzer::removeUtf8Bom)
                    .toList();
            if (columnNames.isEmpty()) {
                throw new IllegalArgumentException("CSV file must contain a header row");
            }

            List<MutableColumnProfile> columns = columnNames.stream()
                    .map(MutableColumnProfile::new)
                    .toList();
            long rows = 0;

            for (CSVRecord record : parser) {
                if (record.size() != columnNames.size()) {
                    throw new IllegalArgumentException(
                            "CSV row " + record.getRecordNumber() + " has a different number of columns");
                }
                for (int index = 0; index < record.size(); index++) {
                    columns.get(index).accept(record.get(index));
                }
                rows++;
            }

            List<ColumnProfile> results = columns.stream()
                    .map(MutableColumnProfile::toResult)
                    .toList();
            return new DatasetProfile(rows, results);
        }
    }

    private static String removeUtf8Bom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private static boolean isInteger(String value) {
        return INTEGER_PATTERN.matcher(value).matches();
    }

    private static BigDecimal parseDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
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

    public record DatasetProfile(long rows, List<ColumnProfile> columns) {
    }

    public record ColumnProfile(String name, ColumnType type, Object statistics) {
    }

    public record NumericStatistics(
            long count,
            BigDecimal min,
            BigDecimal max,
            BigDecimal mean,
            BigDecimal median) {
    }

    public record CategoricalStatistics(long uniqueCount, List<ValueFrequency> topValues) {
    }

    public record ValueFrequency(String value, long count) {
    }

    private static final class MutableColumnProfile {

        private final String name;
        private final List<BigDecimal> numericValues = new ArrayList<>();
        private final Map<String, Long> frequencies = new HashMap<>();
        private long nonMissingCount;
        private boolean allIntegers = true;
        private boolean allDecimals = true;
        private boolean allBooleans = true;
        private boolean allDates = true;

        private MutableColumnProfile(String name) {
            this.name = name;
        }

        private void accept(String rawValue) {
            if (rawValue == null || rawValue.isBlank()) {
                return;
            }

            String value = rawValue.trim();
            nonMissingCount++;
            frequencies.merge(value, 1L, Long::sum);

            boolean integer = isInteger(value);
            BigDecimal decimal = parseDecimal(value);
            allIntegers &= integer;
            allDecimals &= decimal != null;
            allBooleans &= isBoolean(value);
            allDates &= isDate(value);

            if (decimal != null && allDecimals) {
                numericValues.add(decimal);
            } else if (!allDecimals) {
                numericValues.clear();
            }
        }

        private ColumnProfile toResult() {
            ColumnType type = inferType();
            Object statistics = switch (type) {
                case INTEGER, DECIMAL -> numericStatistics();
                case BOOLEAN, DATE, STRING -> categoricalStatistics();
            };
            return new ColumnProfile(name, type, statistics);
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

        private NumericStatistics numericStatistics() {
            numericValues.sort(Comparator.naturalOrder());
            BigDecimal min = numericValues.getFirst();
            BigDecimal max = numericValues.getLast();
            BigDecimal sum = numericValues.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mean = sum.divide(
                    BigDecimal.valueOf(numericValues.size()), MathContext.DECIMAL128);

            int middle = numericValues.size() / 2;
            BigDecimal median = numericValues.size() % 2 == 1
                    ? numericValues.get(middle)
                    : numericValues.get(middle - 1)
                            .add(numericValues.get(middle))
                            .divide(BigDecimal.valueOf(2), MathContext.DECIMAL128);
            return new NumericStatistics(nonMissingCount, min, max, mean, median);
        }

        private CategoricalStatistics categoricalStatistics() {
            List<ValueFrequency> topValues = frequencies.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .limit(TOP_VALUE_LIMIT)
                    .map(entry -> new ValueFrequency(entry.getKey(), entry.getValue()))
                    .toList();
            return new CategoricalStatistics(frequencies.size(), topValues);
        }
    }
}
