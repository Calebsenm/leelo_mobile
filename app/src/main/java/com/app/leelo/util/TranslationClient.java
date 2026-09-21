package com.app.leelo.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Small, best-effort provider used only for the reader tooltip. */
public final class TranslationClient {

    public interface Callback {
        void onResult(List<String> meanings);
    }

    private TranslationClient() {
    }

    public static void fetch(String word, Callback callback) {
        List<String> meanings = new ArrayList<>();
        HttpURLConnection connection = null;
        try {
            String encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8.name());
            URL url = new URL(
                    "https://api.mymemory.translated.net/get?q=" + encodedWord + "&langpair=en|es"
            );
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(2500);
            connection.setReadTimeout(3500);
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                StringBuilder body = new StringBuilder();
                try (InputStream input = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                }

                JSONObject root = new JSONObject(body.toString());
                JSONObject responseData = root.optJSONObject("responseData");
                if (responseData != null) {
                    addMeaning(meanings, responseData.optString("translatedText", ""));
                }

                JSONArray matches = root.optJSONArray("matches");
                if (matches != null) {
                    for (int i = 0; i < matches.length() && meanings.size() < 3; i++) {
                        JSONObject match = matches.optJSONObject(i);
                        if (match != null) {
                            addMeaning(meanings, match.optString("translation", ""));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // The tooltip remains usable for manually entered meanings offline.
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        callback.onResult(meanings);
    }

    private static void addMeaning(List<String> meanings, String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isEmpty() || meanings.contains(cleaned)) {
            return;
        }
        meanings.add(cleaned);
    }
}
