package com.jiayi.dataagent.mapper;

import com.jiayi.dataagent.model.DatasetMetadata;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DatasetMapper {

    @Insert("""
            INSERT INTO datasets (
                original_filename, stored_filename, file_path, file_size,
                row_count, column_count, status
            ) VALUES (
                #{originalFilename}, #{storedFilename}, #{filePath}, #{fileSize},
                #{rowCount}, #{columnCount}, #{status}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DatasetMetadata dataset);

    @Insert("""
            INSERT INTO dataset_columns (dataset_id, column_position, column_name)
            VALUES (#{datasetId}, #{position}, #{columnName})
            """)
    int insertColumn(
            @Param("datasetId") long datasetId,
            @Param("position") int position,
            @Param("columnName") String columnName);

    @Select("""
            SELECT id, original_filename, stored_filename, file_path, file_size,
                   row_count, column_count, status, created_at, updated_at
            FROM datasets
            WHERE id = #{id}
            """)
    DatasetMetadata findById(long id);

    @Select("""
            SELECT id, original_filename, stored_filename, file_path, file_size,
                   row_count, column_count, status, created_at, updated_at
            FROM datasets
            ORDER BY created_at DESC, id DESC
            """)
    List<DatasetMetadata> findAll();

    @Select("""
            SELECT column_name
            FROM dataset_columns
            WHERE dataset_id = #{datasetId}
            ORDER BY column_position
            """)
    List<String> findColumnNames(long datasetId);

    @Delete("DELETE FROM dataset_columns WHERE dataset_id = #{datasetId}")
    int deleteColumns(long datasetId);

    @Delete("DELETE FROM datasets WHERE id = #{id}")
    int deleteById(long id);
}
