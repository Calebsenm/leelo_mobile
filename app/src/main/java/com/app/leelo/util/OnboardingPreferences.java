package com.app.leelo.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Remembers whether the first-run introduction was already shown. */
public class OnboardingPreferences {

    private static final String PREFS_NAME = "leelo_onboarding";
    private static final String KEY_DONE = "done";

    private OnboardingPreferences() {
    }

    public static boolean isDone(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_DONE, false);
    }

    public static void setDone(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_DONE, true).apply();
    }
}
