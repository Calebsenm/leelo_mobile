package com.app.leelo.util;

import java.text.Normalizer;
import java.util.Locale;

public final class WordTextUtils {

    private WordTextUtils() {
    }

    public static String normalizeWord(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();

        return normalized.replaceAll("[^\\p{L}]", "");
    }
}
