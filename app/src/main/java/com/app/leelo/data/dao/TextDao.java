package com.app.leelo.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.app.leelo.data.entity.TextEntity;
import com.app.leelo.model.TextInfo;

import java.util.List;

@Dao
public interface TextDao {

    @Query("SELECT id, title FROM texts ORDER BY modification_date DESC")
    LiveData<List<TextInfo>> getAllTextsInfo();

    @Query("SELECT * FROM texts WHERE id = :id")
    LiveData<TextEntity> getById(long id);

    @Query("SELECT id, title FROM texts WHERE title LIKE :query OR content LIKE :query ORDER BY modification_date DESC")
    LiveData<List<TextInfo>> searchText(String query);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TextEntity entity);

    @Update
    int update(TextEntity entity);

    @Query("DELETE FROM texts WHERE id = :id")
    int deleteById(long id);
}