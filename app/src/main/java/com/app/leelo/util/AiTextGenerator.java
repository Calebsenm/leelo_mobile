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

/** Generates a short graded English reading text from a topic and CEFR level. */
public final class AiTextGenerator {

    public static class Result {
        public final String title;
        public final String content;

        Result(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }

    public interface Callback {
        void onResult(Result result, String error);
    }

    private static final String DEFAULT_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final String SYSTEM_PROMPT =
            "You write a short English reading text for a Spanish learner. "
                    + "Reply with ONLY a JSON object, no markdown: "
                    + "{\"title\": string, \"content\": string}. "
                    + "The content must be 120-180 words in English, appropriate to the requested CEFR level, "
                    + "with varied vocabulary and no word lists or glossaries.";

    private AiTextGenerator() {
    }

    public static void generate(String topic, String level,
                                String apiKey, String model, String endpoint, Callback callback) {
        String userPrompt = "Topic: " + topic + "\nCEFR level: " + level;

        String url = normalizeEndpoint(endpoint);
        String error = request(userPrompt, apiKey, model, url, true, callback);
        if (error == null) {
            return;
        }

        if (needsFallback(error)) {
            error = request(userPrompt, apiKey, model, url, false, callback);
            if (error == null) {
                return;
            }
        }

        // Every failure path must notify the caller, otherwise the Generate button stays disabled.
        callback.onResult(null, error);
    }

    private static boolean needsFallback(String error) {
        String lower = error.toLowerCase();
        return lower.contains("response_format")
                || lower.contains("json_object")
                || lower.contains("http 400")
                || lower.contains("respuesta inesperada");
    }

    /** Returns an error description, or null when the callback was invoked with a result. */
    private static String request(String userPrompt, String apiKey, String model,
                                  String url, boolean jsonMode, Callback callback) {
        HttpURLConnection connection = null;
        try {
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("temperature", 0.4);
            if (jsonMode) {
                body.put("response_format", new JSONObject().put("type", "json_object"));
            }

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", SYSTEM_PROMPT));
            messages.put(new JSONObject().put("role", "user").put("content", userPrompt));
            body.put("messages", messages);

            connection = (HttpURLConnection) new URL(url).openConnection();
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

            int status = connection.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                Result result = parse(read(connection.getInputStream()));
                if (result == null) {
                    return "respuesta inesperada del proveedor";
                }
                callback.onResult(result, null);
                return null;
            }

            InputStream errorStream = connection.getErrorStream();
            String detail = errorStream == null ? "" : read(errorStream).replaceAll("\\s+", " ").trim();
            if (detail.length() > 180) {
                detail = detail.substring(0, 180) + "…";
            }
            return "HTTP " + status + (detail.isEmpty() ? "" : ": " + detail);
        } catch (Exception e) {
            String message = e.getMessage();
            return message == null ? "error de conexión" : message;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

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
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }

        JSONObject data = new JSONObject(content.substring(start, end + 1));
        String title = data.optString("title", "").trim();
        String text = data.optString("content", "").trim();
        if (title.isEmpty() || text.isEmpty()) {
            return null;
        }
        return new Result(title, text);
    }
}
