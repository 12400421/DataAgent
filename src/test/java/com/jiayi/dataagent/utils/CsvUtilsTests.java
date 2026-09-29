package com.jiayi.dataagent.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class CsvUtilsTests {

    @Test
    void readsHeaderAndCountsDataRows() throws Exception {
        String csv = "\uFEFFname,age,city\n"
                + "Alice,25,Shanghai\n"
                + "Bob,30,\"Beijing, China\"\n";

        CsvUtils.CsvSummary summary = CsvUtils.readSummary(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(summary.rows()).isEqualTo(2);
        assertThat(summary.columnNames()).containsExactly("name", "age", "city");
    }

    @Test
    void returnsOnlyRequestedPreviewRows() throws Exception {
        String csv = "name,age\nTom,20\nJack,21\nRose,22\n";

        CsvUtils.CsvData data = CsvUtils.read(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), 2);

        assertThat(data.rows()).isEqualTo(3);
        assertThat(data.preview()).containsExactly(
                java.util.Map.of("name", "Tom", "age", "20"),
                java.util.Map.of("name", "Jack", "age", "21"));
    }
}
