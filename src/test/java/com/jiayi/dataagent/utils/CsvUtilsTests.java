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
}
