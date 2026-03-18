package com.app.leelo.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.app.leelo.data.dao.TextDao;
import com.app.leelo.data.database.AppDatabase;
import com.app.leelo.data.entity.TextEntity;
import com.app.leelo.domain.repository.TextRepository;
import com.app.leelo.model.Text;
import com.app.leelo.model.TextInfo;
import com.app.leelo.utils.AppExecutors;

import java.util.List;

public class TextRepositoryImpl implements TextRepository {

    private static final String TAG = "TextRepositoryImpl";

    private static TextRepositoryImpl instance;
    private final TextDao textDao;
    private final AppExecutors executors;

    private TextRepositoryImpl(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.textDao = db.textDao();
        this.executors = AppExecutors.getInstance();
    }

    public static synchronized TextRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TextRepositoryImpl(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public LiveData<List<TextInfo>> getAllTextInfo() {
        return textDao.getAllTextsInfo();
    }

    @Override
    public LiveData<TextEntity> getTextById(long id) {
        return textDao.getById(id);
    }

    @Override
    public LiveData<List<TextInfo>> searchText(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        return textDao.searchText("%" + normalizedQuery + "%");
    }

    @Override
    public void insertText(Text text, OnOperationCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                validateText(text);
                TextEntity entity = modelToEntity(text);
                long id = textDao.insert(entity);
                postOnMain(() -> callback.onComplete(true, id));
            } catch (Exception e) {
                Log.e(TAG, "Error insertando", e);
                postOnMain(() -> callback.onComplete(false, -1));
            }
        });
    }

    @Override
    public void updateText(Text text, OnOperationCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                validateText(text);
                TextEntity entity = modelToEntity(text);
                int rows = textDao.update(entity);
                postOnMain(() -> callback.onComplete(rows > 0, text.getId() != null ? text.getId() : -1));
            } catch (Exception e) {
                Log.e(TAG, "Error actualizando", e);
                postOnMain(() -> callback.onComplete(false, -1));
            }
        });
    }

    @Override
    public void deleteText(long id, OnOperationCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                int rows = textDao.deleteById(id);
                postOnMain(() -> callback.onComplete(rows > 0, id));
            } catch (Exception e) {
                Log.e(TAG, "Error eliminando", e);
                postOnMain(() -> callback.onComplete(false, id));
            }
        });
    }

    @Override
    public void updateReadingProgress(long id, int currentPage, int totalPages, OnOperationCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                int rows = textDao.updateReadingProgress(id, currentPage, totalPages);
                postOnMain(() -> callback.onComplete(rows > 0, id));
            } catch (Exception e) {
                Log.e(TAG, "Error actualizando progreso de lectura", e);
                postOnMain(() -> callback.onComplete(false, id));
            }
        });
    }

    private void validateText(Text text) {
        if (text.getTitle() == null || text.getTitle().trim().isEmpty() ||
            text.getContent() == null || text.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Título o contenido vacío");
        }
    }

    private TextEntity modelToEntity(Text model) {
        TextEntity entity = new TextEntity();
        if (model.getId() != null) entity.id = model.getId();
        entity.title = model.getTitle().trim();
        entity.content = model.getContent().trim();
        TextEntity existingEntity = model.getId() != null ? textDao.getByIdSync(model.getId()) : null;
        entity.creationDate = model.getCreationDate() != null ?
                model.getCreationDate().atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) * 1000 :
                (existingEntity != null && existingEntity.creationDate != null
                        ? existingEntity.creationDate
                        : System.currentTimeMillis());
        entity.modificationDate = System.currentTimeMillis();
        entity.currentPage = existingEntity != null ? existingEntity.currentPage : 0;
        entity.totalPages = existingEntity != null ? existingEntity.totalPages : 0;
        return entity;
    }

    private void postOnMain(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}
