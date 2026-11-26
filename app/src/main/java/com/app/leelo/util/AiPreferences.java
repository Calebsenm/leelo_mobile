package com.app.leelo.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

/** Local-only AI configuration. The API key is encrypted at rest when the device supports it. */
public class AiPreferences {

    private static final String PREFS_NAME = "leelo_ai_secure_prefs";
    private static final String LEGACY_PREFS_NAME = "leelo_ai_prefs";
    private static final String KEY_MODE = "mode";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_MODEL = "model";
    private static final String KEY_ENDPOINT = "endpoint";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions";

    private static volatile AiPreferences instance;
    private final SharedPreferences prefs;

    private AiPreferences(Context context) {
        Context app = context.getApplicationContext();
        SharedPreferences secure;
        try {
            MasterKey masterKey = new MasterKey.Builder(app)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            secure = EncryptedSharedPreferences.create(app, PREFS_NAME, masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (Exception ignored) {
            secure = app.getSharedPreferences(PREFS_NAME + "_plain", Context.MODE_PRIVATE);
        }
        prefs = secure;
        migrateLegacy(app);
    }

    private void migrateLegacy(Context app) {
        if (prefs.contains(KEY_MODE) || prefs.contains(KEY_API_KEY)) {
            return;
        }
        SharedPreferences legacy = app.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (legacy.contains(KEY_MODE)) {
            editor.putString(KEY_MODE, legacy.getString(KEY_MODE, "manual"));
        }
        if (legacy.contains(KEY_API_KEY)) {
            editor.putString(KEY_API_KEY, legacy.getString(KEY_API_KEY, ""));
        }
        if (legacy.contains(KEY_MODEL)) {
            editor.putString(KEY_MODEL, legacy.getString(KEY_MODEL, DEFAULT_MODEL));
        }
        editor.apply();
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

    public boolean hasApiKey() {
        return !getApiKey().isEmpty();
    }

    public void setApiKey(String key) {
        SharedPreferences.Editor editor = prefs.edit();
        if (key == null || key.trim().isEmpty()) {
            editor.remove(KEY_API_KEY);
        } else {
            editor.putString(KEY_API_KEY, key.trim());
        }
        editor.apply();
    }

    public void clearApiKey() {
        prefs.edit().remove(KEY_API_KEY).apply();
    }

    public String getModel() {
        String model = prefs.getString(KEY_MODEL, DEFAULT_MODEL);
        return model == null || model.trim().isEmpty() ? DEFAULT_MODEL : model.trim();
    }

    public void setModel(String model) {
        prefs.edit().putString(KEY_MODEL, model == null ? "" : model.trim()).apply();
    }

    public String getEndpoint() {
        String endpoint = prefs.getString(KEY_ENDPOINT, DEFAULT_ENDPOINT);
        return endpoint == null || endpoint.trim().isEmpty() ? DEFAULT_ENDPOINT : endpoint.trim();
    }

    public void setEndpoint(String endpoint) {
        SharedPreferences.Editor editor = prefs.edit();
        if (endpoint == null || endpoint.trim().isEmpty()) {
            editor.remove(KEY_ENDPOINT);
        } else {
            editor.putString(KEY_ENDPOINT, endpoint.trim());
        }
        editor.apply();
    }
}
