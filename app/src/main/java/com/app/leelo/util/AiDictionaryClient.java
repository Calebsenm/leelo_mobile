package com.app.leelo.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** OpenAI-compatible dictionary lookup: English meaning + Spanish translation + context. */
public final class AiDictionaryClient {

    private static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 20_000;
    private static final String SYSTEM_PROMPT =
            "You are a precise bilingual dictionary for Spanish readers of English. "
                    + "Reply with ONLY a JSON object, no markdown and no extra text. "
                    + "Keys: meaning_en (clear definition of the word in English), "
                    + "translation_es (its most natural Spanish translation), "
                    + "context (one short example sentence in English followed by its Spanish translation in parentheses).";

    public static class Result {
        public final String meaningEn;
        public final String translationEs;
        public final String context;

        Result(String meaningEn, String translationEs, String context) {
            this.meaningEn = meaningEn;
            this.translationEs = translationEs;
            this.context = context;
        }
    }

    public interface Callback {
        void onResult(Result result);
    }

    private AiDictionaryClient() {
    }

    public static void fetch(String word, String apiKey, String model, String endpoint, Callback callback) {
        HttpURLConnection connection = null;
        Result result = null;
        try {
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("temperature", 0.1);
            body.put("response_format", new JSONObject().put("type", "json_object"));

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", SYSTEM_PROMPT));
            messages.put(new JSONObject().put("role", "user").put("content", "Word: " + word));
            body.put("messages", messages);

            connection = (HttpURLConnection) new URL(normalizeEndpoint(endpoint)).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(payload.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(payload);
            }

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                result = parse(read(connection.getInputStream()));
            }
        } catch (Exception ignored) {
            result = null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        callback.onResult(result);
    }

    /** Accepts either a base URL or a full /chat/completions URL. */
    private static String normalizeEndpoint(String endpoint) {
        String value = endpoint == null || endpoint.trim().isEmpty()
                ? DEFAULT_ENDPOINT : endpoint.trim();
        if (value.endsWith("/chat/completions")) {
            return value;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value + "/chat/completions";
    }

    private static String read(InputStream input) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    private static Result parse(String response) throws Exception {
        JSONObject root = new JSONObject(response);
        JSONArray choices = root.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            return null;
        }
        JSONObject message = choices.getJSONObject(0).optJSONObject("message");
        if (message == null) {
            return null;
        }

        String content = message.optString("content", "").trim();
        if (content.contains("```")) {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) {
                content = content.substring(start, end + 1);
            }
        }

        JSONObject data = new JSONObject(content);
        return new Result(
                data.optString("meaning_en", ""),
                data.optString("translation_es", ""),
                data.optString("context", "")
        );
    }
}
