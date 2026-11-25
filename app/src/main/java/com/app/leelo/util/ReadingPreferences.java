package com.app.leelo.util;

import android.content.Context;
import android.content.SharedPreferences;

public class ReadingPreferences {

    private static final String PREFS_NAME = "leelo_reading_prefs";
    private static final String KEY_TEXT_SIZE = "text_size";
    private static final String KEY_SERIF = "use_serif";
    private static final float DEFAULT_TEXT_SIZE = 16f;

    private static volatile ReadingPreferences instance;
    private final SharedPreferences prefs;

    private ReadingPreferences(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static ReadingPreferences getInstance(Context context) {
        if (instance == null) {
            synchronized (ReadingPreferences.class) {
                if (instance == null) {
                    instance = new ReadingPreferences(context);
                }
            }
        }
        return instance;
    }

    public float getTextSize() {
        return prefs.getFloat(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE);
    }

    public void setTextSize(float textSize) {
        prefs.edit().putFloat(KEY_TEXT_SIZE, textSize).apply();
    }

    /** Serif is the default because the reader is meant to feel like a book. */
    public boolean isSerif() {
        return prefs.getBoolean(KEY_SERIF, true);
    }

    public void setSerif(boolean useSerif) {
        prefs.edit().putBoolean(KEY_SERIF, useSerif).apply();
    }
}
