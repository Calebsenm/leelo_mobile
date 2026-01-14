package com.app.leelo.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.OnConflictStrategy;

import com.app.leelo.data.entity.TextEntity;

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
}