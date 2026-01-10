package com.app.leelo.data.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.app.leelo.data.entity.TextEntity;
import com.app.leelo.data.dao.TextDao;

/**
 * Database = La base de datos principal
 * ROOM crea el archivo y administra todo esto
 */
@Database(
    entities = {TextEntity.class},  // Tablas que tendrá la BD
    version = 1,                    // Versión de la BD
    exportSchema = false            // Para no generar archivos JSON
)
public abstract class AppDatabase extends RoomDatabase {

    // ROOM crea la implementación automáticamente
    public abstract TextDao textDao();

    // Nombre del archivo de la base de datos
    private static final String DATABASE_NAME = "leelo_database";

    // Singleton para tener una sola instancia
    private static AppDatabase INSTANCE;

    /**
     * Obtener instancia única de la base de datos
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration() // Para desarrollo: si cambia la versión, borra y recrea
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Cerrar la base de datos (cuando se cierra la app)
     */
    public static void destroyInstance() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }

    /**
     * Obtener información de la base de datos (para debug)
     */
    public String getDatabasePath(Context context) {
        return context.getDatabasePath(DATABASE_NAME).getAbsolutePath();
    }

    /**
     * Verificar si la base de datos existe
     */
    public boolean databaseExists(Context context) {
        return context.getDatabasePath(DATABASE_NAME).exists();
    }
}