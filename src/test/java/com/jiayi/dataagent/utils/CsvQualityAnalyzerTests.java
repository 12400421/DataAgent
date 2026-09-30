package com.jiayi.dataagent.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.jiayi.dataagent.utils.CsvQualityAnalyzer.ColumnQuality;
import com.jiayi.dataagent.utils.CsvQualityAnalyzer.ColumnType;
import com.jiayi.dataagent.utils.CsvQualityAnalyzer.DatasetQuality;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class CsvQualityAnalyzerTests {

    @Test
    void analyzesMissingValuesTypesUniqueValuesAndDuplicateRows() throws Exception {
        String csv = "id,price,active,birthday,name\n"
                + "1,10.5,true,2024-01-01,Alice\n"
                + "2,,false,2024-01-02,\n"
                + "2,,false,2024-01-02,\n";

        DatasetQuality quality = CsvQualityAnalyzer.analyze(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(quality.rows()).isEqualTo(3);
        assertThat(quality.duplicateRowCount()).isEqualTo(1);
        assertThat(quality.columns())
                .extracting(ColumnQuality::type)
                .containsExactly(
                        ColumnType.INTEGER,
                        ColumnType.DECIMAL,
                        ColumnType.BOOLEAN,
                        ColumnType.DATE,
                        ColumnType.STRING);

        ColumnQuality price = quality.columns().get(1);
        assertThat(price.missingCount()).isEqualTo(2);
        assertThat(price.missingRate()).isCloseTo(2.0 / 3.0, within(0.000_001));
        assertThat(price.uniqueCount()).isEqualTo(1);

        ColumnQuality name = quality.columns().get(4);
        assertThat(name.missingCount()).isEqualTo(2);
        assertThat(name.uniqueCount()).isEqualTo(1);
    }
}
