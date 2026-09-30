package com.jiayi.dataagent.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;

import com.jiayi.dataagent.utils.CsvProfileAnalyzer.CategoricalStatistics;
import com.jiayi.dataagent.utils.CsvProfileAnalyzer.DatasetProfile;
import com.jiayi.dataagent.utils.CsvProfileAnalyzer.NumericStatistics;
import com.jiayi.dataagent.utils.CsvProfileAnalyzer.ValueFrequency;
import com.jiayi.dataagent.utils.CsvQualityAnalyzer.ColumnType;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class CsvProfileAnalyzerTests {

    @Test
    void profilesNumericAndCategoricalColumns() throws Exception {
        String csv = "id,score,name,active,birthday\n"
                + "1,10.5,Alice,true,2024-01-01\n"
                + "2,20.5,Bob,false,2024-01-02\n"
                + "3,,Alice,true,2024-01-03\n"
                + "4,30.0,Carol,true,2024-01-04\n";

        DatasetProfile profile = CsvProfileAnalyzer.analyze(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(profile.rows()).isEqualTo(4);
        assertThat(profile.columns())
                .extracting(column -> column.type())
                .containsExactly(
                        ColumnType.INTEGER,
                        ColumnType.DECIMAL,
                        ColumnType.STRING,
                        ColumnType.BOOLEAN,
                        ColumnType.DATE);

        NumericStatistics id = (NumericStatistics) profile.columns().get(0).statistics();
        assertThat(id.count()).isEqualTo(4);
        assertThat(id.min()).isEqualByComparingTo("1");
        assertThat(id.max()).isEqualByComparingTo("4");
        assertThat(id.mean()).isEqualByComparingTo("2.5");
        assertThat(id.median()).isEqualByComparingTo("2.5");

        NumericStatistics score = (NumericStatistics) profile.columns().get(1).statistics();
        assertThat(score.count()).isEqualTo(3);
        assertThat(score.min()).isEqualByComparingTo("10.5");
        assertThat(score.max()).isEqualByComparingTo("30.0");
        assertThat(score.mean().doubleValue()).isCloseTo(20.333333, within(0.000001));
        assertThat(score.median()).isEqualByComparingTo("20.5");

        CategoricalStatistics name =
                (CategoricalStatistics) profile.columns().get(2).statistics();
        assertThat(name.uniqueCount()).isEqualTo(3);
        assertThat(name.topValues())
                .extracting(ValueFrequency::value, ValueFrequency::count)
                .containsExactly(
                        tuple("Alice", 2L),
                        tuple("Bob", 1L),
                        tuple("Carol", 1L));

        CategoricalStatistics active =
                (CategoricalStatistics) profile.columns().get(3).statistics();
        assertThat(active.uniqueCount()).isEqualTo(2);
        assertThat(active.topValues())
                .extracting(ValueFrequency::value, ValueFrequency::count)
                .containsExactly(tuple("true", 3L), tuple("false", 1L));
    }

    @Test
    void limitsFrequentValuesToTopTenWithStableOrdering() throws Exception {
        String csv = "category\n"
                + "value-10\nvalue-09\nvalue-08\nvalue-07\nvalue-06\nvalue-05\n"
                + "value-04\nvalue-03\nvalue-02\nvalue-01\nvalue-00\n";

        DatasetProfile profile = CsvProfileAnalyzer.analyze(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        CategoricalStatistics category =
                (CategoricalStatistics) profile.columns().getFirst().statistics();
        assertThat(category.uniqueCount()).isEqualTo(11);
        assertThat(category.topValues()).hasSize(10);
        assertThat(category.topValues())
                .extracting(ValueFrequency::value)
                .containsExactly(
                        "value-00", "value-01", "value-02", "value-03", "value-04",
                        "value-05", "value-06", "value-07", "value-08", "value-09");
    }
}
