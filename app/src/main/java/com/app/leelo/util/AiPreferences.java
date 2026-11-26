package com.app.leelo.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Local-only configuration for the AI dictionary mode. */
public class AiPreferences {

    private static final String PREFS_NAME = "leelo_ai_prefs";
    private static final String KEY_MODE = "mode";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MODEL = "model";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private static volatile AiPreferences instance;
    private final SharedPreferences prefs;

    private AiPreferences(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static AiPreferences getInstance(Context context) {
        if (instance == null) {
            synchronized (AiPreferences.class) {
                if (instance == null) {
                    instance = new AiPreferences(context);
                }
            }
        }
        return instance;
    }

    public boolean isAiMode() {
        return "ai".equals(prefs.getString(KEY_MODE, "manual"));
    }

    public void setAiMode(boolean enabled) {
        prefs.edit().putString(KEY_MODE, enabled ? "ai" : "manual").apply();
    }

    public String getApiKey() {
        String key = prefs.getString(KEY_API_KEY, "");
        return key == null ? "" : key.trim();
    }

    public void setApiKey(String key) {
        prefs.edit().putString(KEY_API_KEY, key == null ? "" : key.trim()).apply();
    }

    public String getModel() {
        String model = prefs.getString(KEY_MODEL, DEFAULT_MODEL);
        return model == null || model.trim().isEmpty() ? DEFAULT_MODEL : model.trim();
    }

    public void setModel(String model) {
        prefs.edit().putString(KEY_MODEL, model == null ? "" : model.trim()).apply();
    }
}
