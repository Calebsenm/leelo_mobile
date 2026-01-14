package com.app.leelo.data.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.app.leelo.data.entity.TextEntity;
import com.app.leelo.data.dao.TextDao;


@Database(
    entities = {TextEntity.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TextDao textDao();

    private static final String DATABASE_NAME = "leelo_database";

    private static AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        if (INSTANCE != null) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }

    public String getDatabasePath(Context context) {
        return context.getDatabasePath(DATABASE_NAME).getAbsolutePath();
    }


    public boolean databaseExists(Context context) {
        return context.getDatabasePath(DATABASE_NAME).exists();
    }
}