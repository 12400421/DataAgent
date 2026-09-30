package com.jiayi.dataagent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.jiayi.dataagent.exception.DatasetException;
import com.jiayi.dataagent.mapper.DatasetMapper;
import com.jiayi.dataagent.model.DatasetMetadata;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class DatasetPersistenceServiceTests {

    @Mock
    private DatasetMapper datasetMapper;

    @Mock
    private DatasetFileStorage fileStorage;

    private DatasetPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new DatasetPersistenceService(datasetMapper, new DatasetService(), fileStorage);
    }

    @Test
    void listsDatasetsUsingManagementFields() {
        DatasetMetadata dataset = dataset(1L, "students.csv", "C:/data/stored.csv");
        when(datasetMapper.findAll()).thenReturn(List.of(dataset));

        List<Map<String, Object>> result = service.findAll();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.get("id")).isEqualTo(1L);
            assertThat(item.get("filename")).isEqualTo("students.csv");
            assertThat(item.get("fileSize")).isEqualTo(128L);
            assertThat(item.get("rows")).isEqualTo(2L);
            assertThat(item.get("columns")).isEqualTo(3);
            assertThat(item.get("status")).isEqualTo("READY");
            assertThat(item.get("createdAt")).isNotNull();
            assertThat(item.get("updatedAt")).isNotNull();
        });
    }

    @Test
    void deletesColumnsDatasetAndStoredFile() {
        DatasetMetadata dataset = dataset(1L, "students.csv", "C:/data/stored.csv");
        when(datasetMapper.findById(1L)).thenReturn(dataset);
        when(datasetMapper.deleteById(1L)).thenReturn(1);

        service.delete(1L);

        InOrder order = inOrder(datasetMapper, fileStorage);
        order.verify(datasetMapper).findById(1L);
        order.verify(datasetMapper).deleteColumns(1L);
        order.verify(datasetMapper).deleteById(1L);
        order.verify(fileStorage).deleteRequired("C:/data/stored.csv");
    }

    @Test
    void returnsNotFoundWhenDeletingUnknownDataset() {
        when(datasetMapper.findById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(DatasetException.class)
                .satisfies(exception -> assertThat(((DatasetException) exception).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(fileStorage);
    }

    @Test
    void profilesTheStoredDatasetFile() {
        DatasetMetadata dataset = dataset(1L, "students.csv", "C:/data/stored.csv");
        String csv = "age,name\n20,Alice\n30,Bob\n";
        when(datasetMapper.findById(1L)).thenReturn(dataset);
        when(fileStorage.open("C:/data/stored.csv")).thenReturn(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        Map<String, Object> result = service.profile(1L);

        assertThat(result.get("id")).isEqualTo(1L);
        assertThat(result.get("filename")).isEqualTo("students.csv");
        assertThat(result.get("rows")).isEqualTo(2L);
        assertThat((List<?>) result.get("columnProfiles")).hasSize(2);
    }

    private DatasetMetadata dataset(long id, String filename, String filePath) {
        DatasetMetadata dataset = new DatasetMetadata();
        dataset.setId(id);
        dataset.setOriginalFilename(filename);
        dataset.setStoredFilename("stored.csv");
        dataset.setFilePath(filePath);
        dataset.setFileSize(128L);
        dataset.setRowCount(2L);
        dataset.setColumnCount(3);
        dataset.setStatus("READY");
        dataset.setCreatedAt(LocalDateTime.of(2026, 9, 29, 10, 0));
        dataset.setUpdatedAt(LocalDateTime.of(2026, 9, 29, 10, 0));
        return dataset;
    }
}
