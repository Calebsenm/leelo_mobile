package com.app.leelo.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.app.leelo.data.entity.WordEntity;

import java.util.List;

@Dao
public interface WordDao {

    @Query("SELECT * FROM words ORDER BY updated_at DESC")
    LiveData<List<WordEntity>> getAllWords();

    @Query("SELECT * FROM words WHERE state = :state ORDER BY updated_at DESC")
    LiveData<List<WordEntity>> getWordsByState(int state);

    @Query("SELECT * FROM words WHERE word = :word LIMIT 1")
    LiveData<WordEntity> getWordByText(String word);

    @Query("SELECT * FROM words WHERE word = :word LIMIT 1")
    WordEntity getWordByTextSync(String word);

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    LiveData<WordEntity> getWordById(long id);

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    WordEntity getWordByIdSync(long id);

    @Query("SELECT COUNT(*) FROM words")
    LiveData<Integer> getTotalCount();

    @Query("SELECT COUNT(*) FROM words WHERE state = :state")
    LiveData<Integer> getCountByState(int state);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(WordEntity word);

    @Update
    int update(WordEntity word);

    @Query("DELETE FROM words WHERE id = :id")
    int deleteById(long id);

    @Query("SELECT EXISTS(SELECT 1 FROM words WHERE word = :word)")
    boolean wordExists(String word);
}
