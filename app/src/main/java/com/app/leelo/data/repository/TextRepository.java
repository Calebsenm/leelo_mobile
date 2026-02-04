package com.app.leelo.data.repository;

import android.content.Context;
import android.util.Log;

import com.app.leelo.data.entity.TextEntity;
import com.app.leelo.data.dao.TextDao;
import com.app.leelo.data.database.AppDatabase;
import com.app.leelo.model.Text;
import com.app.leelo.model.TextInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TextRepository {
    
    private static final String TAG = "TextRepository";
    private static TextRepository instance;
    private final TextDao textDao;
    private final ExecutorService executor;

    private TextRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.textDao = database.textDao();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    public static synchronized TextRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TextRepository(context.getApplicationContext());
        }
        return instance;
    }

    public interface OnInsertCallback {
        void onInsertComplete(boolean success, long id);
    }

    public interface OnUpdateCallback {
        void onUpdateComplete(boolean success);
    }

    public interface OnDeleteCallback {
        void onDeleteComplete(boolean success);
    }

    public interface OnGetTextCallback {
        void onGetTextComplete(Text text);
    }

    public interface OnGetAllTextInfoCallback {
        void onGetAllTextInfoComplete(List<TextInfo> texts);
    }

    public void insertText(Text text, OnInsertCallback callback) {
        executor.execute(() -> {
            try {

                if (text.getTitle() == null || text.getTitle().trim().isEmpty()) {
                    Log.e(TAG, "Error: Título vacío");
                    if (callback != null) callback.onInsertComplete(false, -1);
                    return;
                }
                
                if (text.getText() == null || text.getText().trim().isEmpty()) {
                    Log.e(TAG, "Error: Contenido vacío");
                    if (callback != null) callback.onInsertComplete(false, -1);
                    return;
                }
                
                TextEntity entity = modelToEntity(text);
                Log.d(TAG, "Guardando texto: " + entity.title + " (longitud: " + (entity.content != null ? entity.content.length() : 0) + ")");
                
                long id = textDao.insert(entity);
                
                boolean success = id != -1;
                Log.d(TAG, "Texto guardado: " + (success ? "OK con ID " + id : "ERROR"));
                
                if (callback != null) {
                    callback.onInsertComplete(success, id);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error guardando texto", e);
                e.printStackTrace(); // Mostrar stack trace completo
                if (callback != null) {
                    callback.onInsertComplete(false, -1);
                }
            }
        });
    }

    public void getAllTextInfoAsync(OnGetAllTextInfoCallback callback) {
        executor.execute(() -> {
            try {
                List<TextInfo> entities = textDao.getAllTextsInfo();
                if (callback != null) {
                    callback.onGetAllTextInfoComplete(entities);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error obteniendo textos info", e);
                if (callback != null) {
                    callback.onGetAllTextInfoComplete(new ArrayList<>());
                }
            }
        });
    }

    public void updateText(Text text, OnUpdateCallback callback) {
        executor.execute(() -> {
            try {
                TextEntity entity = modelToEntity(text);
                int rowsAffected = textDao.update(entity);
                
                if (callback != null) {
                    callback.onUpdateComplete(rowsAffected > 0);
                }
                
                Log.d(TAG, "Texto actualizado: " + (rowsAffected > 0 ? "OK" : "ERROR"));
            } catch (Exception e) {
                Log.e(TAG, "Error actualizando texto", e);
                if (callback != null) {
                    callback.onUpdateComplete(false);
                }
            }
        });
    }

    public void deleteText(long id, OnDeleteCallback callback) {
        executor.execute(() -> {
            try {
                int rowsDeleted = textDao.deleteById(id);
                
                if (callback != null) {
                    callback.onDeleteComplete(rowsDeleted > 0);
                }
                
                Log.d(TAG, "Texto eliminado: " + (rowsDeleted > 0 ? "OK" : "ERROR"));
            } catch (Exception e) {
                Log.e(TAG, "Error eliminando texto", e);
                if (callback != null) {
                    callback.onDeleteComplete(false);
                }
            }
        });
    }

    private TextEntity modelToEntity(Text model) {
        TextEntity entity = new TextEntity();
        
        if (model.getIdText() != null) {
            entity.id = model.getIdText();
        }


        String title = model.getTitle();
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("El título no puede estar vacío");
        }
        entity.title = title.trim();

        String content = model.getText();
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("El contenido no puede estar vacío");
        }
        entity.content = content.trim();
        

        if (model.getCreationDate() != null) {
            try {
                entity.creationDate = java.sql.Timestamp.valueOf(
                    model.getCreationDate().atStartOfDay().toString()
                ).getTime();
            } catch (Exception e) {
                Log.e(TAG, "Error convirtiendo fecha, usando fecha actual", e);
                entity.creationDate = System.currentTimeMillis();
            }
        } else {
            entity.creationDate = System.currentTimeMillis();
        }
        
        entity.modificationDate = System.currentTimeMillis();
        
        Log.d(TAG, "TextEntity creada: " + entity.title + " (ID: " + entity.id + ")");
        return entity;
    }

    private Text entityToModel(TextEntity entity) {
        Text model = new Text();
        
        model.setIdText(entity.id);
        model.setTitle(entity.title);
        model.setText(entity.content);
        model.setCreationDate(entity.getCreationDateAsLocalDate());
        
        return model;
    }

    public void getTextById(long id, OnGetTextCallback callback) {
        executor.execute(() -> {
            try {
                TextEntity entity = textDao.getById(id);
                Text text = entity != null ? entityToModel(entity) : null;
                
                if (callback != null) {
                    callback.onGetTextComplete(text);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error obteniendo texto por ID", e);
                if (callback != null) {
                    callback.onGetTextComplete(null);
                }
            }
        });
    }

}