package com.app.leelo.util;

/** Ready-to-use configurations so users only need to paste their API key. */
public final class AiProviders {

    public static final class Preset {
        public final String label;
        public final String endpoint;
        public final String model;
        public final String[] models;
        public final String hint;

        Preset(String label, String endpoint, String model, String[] models, String hint) {
            this.label = label;
            this.endpoint = endpoint;
            this.model = model;
            this.models = models;
            this.hint = hint;
        }
    }

    public static final Preset[] PRESETS = {
            new Preset(
                    "Groq",
                    "https://api.groq.com/openai/v1",
                    "llama-3.3-70b-versatile",
                    new String[]{
                            "llama-3.1-8b-instant",
                            "llama-3.3-70b-versatile",
                            "openai/gpt-oss-20b",
                            "openai/gpt-oss-120b"
                    },
                    "Groq (chat): llama-3.1-8b-instant ~560 t/s · llama-3.3-70b-versatile ~280 t/s · "
                            + "gpt-oss-20b $0.075/$0.30 · gpt-oss-120b $0.15/$0.60 por 1M tokens. "
                            + "Whisper y prompt-guard no son modelos de chat."),
            new Preset(
                    "OpenAI",
                    "https://api.openai.com/v1",
                    "gpt-4o-mini",
                    new String[]{"gpt-4o-mini", "gpt-4o"},
                    "OpenAI: gpt-4o-mini (económico) o gpt-4o (más preciso)."),
            new Preset(
                    "OpenRouter",
                    "https://openrouter.ai/api/v1",
                    "openai/gpt-4o-mini",
                    new String[]{"openai/gpt-4o-mini"},
                    "OpenRouter: copia el ID exacto que muestra el proveedor."),
            new Preset(
                    "Personalizado",
                    "",
                    "",
                    new String[0],
                    "Escribe la URL base y el ID exacto del modelo de tu proveedor.")
    };

    public static final int CUSTOM_INDEX = PRESETS.length - 1;

    private AiProviders() {
    }

    /** Selects the saved provider, falling back to Personalizado for unknown endpoints. */
    public static int indexForEndpoint(String endpoint) {
        String saved = normalize(endpoint);
        if (saved.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < CUSTOM_INDEX; i++) {
            if (saved.equals(normalize(PRESETS[i].endpoint))) {
                return i;
            }
        }
        return CUSTOM_INDEX;
    }

    /** Resolves a base URL or a full /chat/completions URL to the same value. */
    private static String normalize(String endpoint) {
        String value = endpoint == null ? "" : endpoint.trim().toLowerCase();
        if (value.endsWith("/chat/completions")) {
            value = value.substring(0, value.length() - "/chat/completions".length());
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
