package com.app.leelo.data.local.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import com.app.leelo.data.local.entity.TextEntity;
import com.app.leelo.data.local.entity.WordEntity;
import com.app.leelo.data.local.dao.TextDao;
import com.app.leelo.data.local.dao.WordDao;


@Database(
    entities = {TextEntity.class, WordEntity.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract TextDao textDao();
    public abstract WordDao wordDao();
    
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
}
