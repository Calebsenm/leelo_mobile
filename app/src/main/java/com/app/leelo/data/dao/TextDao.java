package com.app.leelo.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.OnConflictStrategy;

import com.app.leelo.data.entity.TextEntity;
import com.app.leelo.model.TextInfo;

import java.util.List;

@Dao
public interface TextDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TextEntity text);

    @Update
    int update(TextEntity text);

    @Delete
    int delete(TextEntity text);

    @Query("DELETE FROM texts WHERE id = :id")
    int deleteById(long id);

    @Query("SELECT * FROM texts ORDER BY creation_date DESC")
    List<TextEntity> getAll();

    @Query("SELECT id, title FROM texts")
    List<TextInfo> getAllTextsInfo();

    @Query("SELECT * FROM texts WHERE id = :id")
    TextEntity getById(long id);

    @Query("SELECT * FROM texts WHERE title LIKE '%' || :query || '%' ORDER BY creation_date DESC")
    List<TextEntity> searchByTitle(String query);

    @Query("SELECT COUNT(*) FROM texts")
    int getCount();

    @Query("SELECT COUNT(*) FROM texts WHERE title = :title")
    int existsByTitle(String title);


    @Query("SELECT * FROM texts ORDER BY creation_date DESC LIMIT 10")
    List<TextEntity> getRecent();


    @Query("DELETE FROM texts")
    void deleteAll();

    @Query("SELECT SUBSTR(content, :offset, :length) FROM texts WHERE id = :id")
    String getTextChunk(long id, int offset, int length);

    @Query("SELECT LENGTH(content) FROM texts WHERE id = :id")
    int getTextLength(long id);

    @Query("SELECT SUBSTR(content, :start, :end - :start + 1) FROM texts WHERE id = :id")
    String getTextSegment(long id, int start, int end);
}