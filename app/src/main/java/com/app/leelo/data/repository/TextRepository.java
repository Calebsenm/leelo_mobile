package com.app.leelo.data.repository;

import android.content.Context;
import android.util.Log;

import com.app.leelo.data.entity.TextEntity;
import com.app.leelo.data.dao.TextDao;
import com.app.leelo.data.database.AppDatabase;
import com.app.leelo.model.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository = El cerebro que conecta todo
 * La UI habla con esto, esto habla con la base de datos
 * Convierte Entity (BD) <-> Model (UI)
 */
public class TextRepository {
    
    private static final String TAG = "TextRepository";
    private static TextRepository instance;
    
    private final TextDao textDao;
    private final ExecutorService executor; // Para no bloquear el UI
    
    // Singleton: solo una instancia en toda la app
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
    
    // ========== OPERACIONES PRINCIPALES ==========
    
    /**
     * Guardar un nuevo texto
     */
    public void insertText(Text text, OnInsertCallback callback) {
        executor.execute(() -> {
            try {
                TextEntity entity = modelToEntity(text);
                long id = textDao.insert(entity);
                
                if (callback != null) {
                    callback.onInsertComplete(id != -1, id);
                }
                
                Log.d(TAG, "Texto guardado con ID: " + id);
            } catch (Exception e) {
                Log.e(TAG, "Error guardando texto", e);
                if (callback != null) {
                    callback.onInsertComplete(false, -1);
                }
            }
        });
    }
    
    /**
     * Obtener todos los textos
     */
    public void getAllTexts(OnGetAllCallback callback) {
        executor.execute(() -> {
            try {
                List<TextEntity> entities = textDao.getAll();
                List<Text> texts = new ArrayList<>();
                
                for (TextEntity entity : entities) {
                    texts.add(entityToModel(entity));
                }
                
                if (callback != null) {
                    callback.onGetAllComplete(texts);
                }
                
                Log.d(TAG, "Se cargaron " + texts.size() + " textos");
            } catch (Exception e) {
                Log.e(TAG, "Error obteniendo textos", e);
                if (callback != null) {
                    callback.onGetAllComplete(new ArrayList<>());
                }
            }
        });
    }
    
    /**
     * Actualizar un texto existente
     */
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
    
    /**
     * Eliminar un texto
     */
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
    
    /**
     * Buscar textos por título
     */
    public void searchTexts(String query, OnSearchCallback callback) {
        executor.execute(() -> {
            try {
                List<TextEntity> entities = textDao.searchByTitle(query);
                List<Text> texts = new ArrayList<>();
                
                for (TextEntity entity : entities) {
                    texts.add(entityToModel(entity));
                }
                
                if (callback != null) {
                    callback.onSearchComplete(texts);
                }
                
                Log.d(TAG, "Búsqueda '" + query + "' encontró " + texts.size() + " resultados");
            } catch (Exception e) {
                Log.e(TAG, "Error buscando textos", e);
                if (callback != null) {
                    callback.onSearchComplete(new ArrayList<>());
                }
            }
        });
    }
    
    // ========== CONVERSIONES ==========
    
    /**
     * Convertir Model (UI) a Entity (BD)
     */
    private TextEntity modelToEntity(Text model) {
        TextEntity entity = new TextEntity();
        
        if (model.getIdText() != null) {
            entity.id = model.getIdText();
        }
        
        entity.title = model.getTitle();
        entity.content = model.getText();
        
        // Si el modelo no tiene fecha de creación, usa ahora
        if (model.getCreationDate() != null) {
            entity.creationDate = java.sql.Timestamp.valueOf(
                model.getCreationDate().atStartOfDay().toString()
            ).getTime();
        } else {
            entity.creationDate = System.currentTimeMillis();
        }
        
        entity.modificationDate = System.currentTimeMillis();
        
        return entity;
    }
    
    /**
     * Convertir Entity (BD) a Model (UI)
     */
    private Text entityToModel(TextEntity entity) {
        Text model = new Text();
        
        model.setIdText(entity.id);
        model.setTitle(entity.title);
        model.setText(entity.content);
        model.setCreationDate(entity.getCreationDateAsLocalDate());
        
        return model;
    }
    
    // ========== INTERFACES PARA CALLBACKS ==========
    
    public interface OnInsertCallback {
        void onInsertComplete(boolean success, long id);
    }
    
    public interface OnGetAllCallback {
        void onGetAllComplete(List<Text> texts);
    }
    
    public interface OnUpdateCallback {
        void onUpdateComplete(boolean success);
    }
    
    public interface OnDeleteCallback {
        void onDeleteComplete(boolean success);
    }
    
    public interface OnSearchCallback {
        void onSearchComplete(List<Text> texts);
    }
    
    /**
     * Cerrar el executor cuando se cierra la app
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}