package com.app.leelo.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.app.leelo.data.dao.WordDao;
import com.app.leelo.data.database.AppDatabase;
import com.app.leelo.data.entity.WordEntity;
import com.app.leelo.domain.repository.WordRepository;
import com.app.leelo.model.Word;
import com.app.leelo.utils.AppExecutors;

import java.util.ArrayList;
import java.util.List;

public class WordRepositoryImpl implements WordRepository {

    private static final String TAG = "WordRepositoryImpl";
    private static volatile WordRepositoryImpl instance;

    private final WordDao wordDao;
    private final AppExecutors executors;
    private final Handler mainHandler;

    private WordRepositoryImpl(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.wordDao = database.wordDao();
        this.executors = AppExecutors.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static WordRepositoryImpl getInstance(Context context) {
        if (instance == null) {
            synchronized (WordRepositoryImpl.class) {
                if (instance == null) {
                    instance = new WordRepositoryImpl(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    @Override
    public LiveData<List<Word>> getAllWords() {
        return Transformations.map(wordDao.getAllWords(), this::entitiesToWords);
    }

    @Override
    public LiveData<List<Word>> getWordsByState(Word.State state) {
        return Transformations.map(wordDao.getWordsByState(state.getValue()), this::entitiesToWords);
    }

    @Override
    public LiveData<Word> getWordByText(String word) {
        return Transformations.map(wordDao.getWordByText(word), this::entityToWord);
    }

    @Override
    public LiveData<Word> getWordById(long id) {
        return Transformations.map(wordDao.getWordById(id), this::entityToWord);
    }

    @Override
    public LiveData<Integer> getTotalCount() {
        return wordDao.getTotalCount();
    }

    @Override
    public LiveData<Integer> getCountByState(Word.State state) {
        return wordDao.getCountByState(state.getValue());
    }

    @Override
    public void insertWord(Word word, OnOperationCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                WordEntity entity = wordToEntity(word);
                long id = wordDao.insert(entity);
                postOnMain(() -> callback.onComplete(true, id));
            } catch (Exception e) {
                Log.e(TAG, "Error inserting word", e);
                postOnMain(() -> callback.onComplete(false, -1));
            }
        });
    }

    @Override
    public void updateWord(Word word, OnOperationCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                WordEntity entity = wordToEntity(word);
                entity.updatedAt = System.currentTimeMillis();
                int rows = wordDao.update(entity);
                postOnMain(() -> callback.onComplete(rows > 0, word.getId() != null ? word.getId() : -1));
            } catch (Exception e) {
                Log.e(TAG, "Error updating word", e);
                postOnMain(() -> callback.onComplete(false, -1));
            }
        });
    }

    @Override
    public void deleteWord(long id, OnOperationCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                int rows = wordDao.deleteById(id);
                postOnMain(() -> callback.onComplete(rows > 0, id));
            } catch (Exception e) {
                Log.e(TAG, "Error deleting word", e);
                postOnMain(() -> callback.onComplete(false, -1));
            }
        });
    }

    @Override
    public boolean wordExistsSync(String word) {
        return wordDao.wordExists(word.toLowerCase().trim().replaceAll("[^a-zA-ZáéíóúñÁÉÍÓÚÑ]", ""));
    }

    private WordEntity wordToEntity(Word word) {
        WordEntity entity = new WordEntity();
        if (word.getId() != null) {
            entity.id = word.getId();
        }
        entity.word = word.getWord() != null ? word.getWord().toLowerCase().trim().replaceAll("[^a-zA-ZáéíóúñÁÉÍÓÚÑ]", "") : "";
        entity.meaning = word.getMeaning() != null ? word.getMeaning().trim() : "";
        entity.state = word.getState() != null ? word.getState().getValue() : Word.State.LEARNING.getValue();
        entity.createdAt = word.getCreatedAt() != null ? word.getCreatedAt() : System.currentTimeMillis();
        entity.updatedAt = System.currentTimeMillis();
        return entity;
    }

    private Word entityToWord(WordEntity entity) {
        if (entity == null) return null;
        
        Word word = new Word();
        word.setId(entity.id);
        word.setWord(entity.word);
        word.setMeaning(entity.meaning);
        word.setState(Word.State.fromValue(entity.state));
        word.setCreatedAt(entity.createdAt);
        word.setUpdatedAt(entity.updatedAt);
        return word;
    }

    private List<Word> entitiesToWords(List<WordEntity> entities) {
        List<Word> words = new ArrayList<>();
        if (entities != null) {
            for (WordEntity entity : entities) {
                words.add(entityToWord(entity));
            }
        }
        return words;
    }

    private void postOnMain(Runnable runnable) {
        mainHandler.post(runnable);
    }
}
