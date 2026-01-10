package com.app.leelo.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.OnConflictStrategy;

import com.app.leelo.data.entity.TextEntity;

import java.util.List;

/**
 * DAO = Data Access Object
 * Acá están todas las operaciones de la base de datos
 * ROOM traduce esto a SQL automáticamente
 */
@Dao
public interface TextDao {

    /**
     * Guardar un nuevo texto
     * Si hay conflicto (mismo ID), lo reemplaza
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TextEntity text);

    /**
     * Actualizar un texto existente
     */
    @Update
    int update(TextEntity text);

    /**
     * Eliminar un texto
     */
    @Delete
    int delete(TextEntity text);

    /**
     * Eliminar por ID (más directo)
     */
    @Query("DELETE FROM texts WHERE id = :id")
    int deleteById(long id);

    /**
     * Obtener todos los textos
     * Ordenados por fecha de creación (más nuevos primero)
     */
    @Query("SELECT * FROM texts ORDER BY creation_date DESC")
    List<TextEntity> getAll();

    /**
     * Obtener un texto por su ID
     */
    @Query("SELECT * FROM texts WHERE id = :id")
    TextEntity getById(long id);

    /**
     * Buscar textos por título
     * El % busca texto que contenga la búsqueda
     */
    @Query("SELECT * FROM texts WHERE title LIKE '%' || :query || '%' ORDER BY creation_date DESC")
    List<TextEntity> searchByTitle(String query);

    /**
     * Contar cuántos textos hay
     */
    @Query("SELECT COUNT(*) FROM texts")
    int getCount();

    /**
     * Verificar si un título ya existe
     */
    @Query("SELECT COUNT(*) FROM texts WHERE title = :title")
    int existsByTitle(String title);

    /**
     * Obtener textos más recientes (últimos 10)
     */
    @Query("SELECT * FROM texts ORDER BY creation_date DESC LIMIT 10")
    List<TextEntity> getRecent();

    /**
     * Eliminar todos los textos (para testing)
     */
    @Query("DELETE FROM texts")
    void deleteAll();
}