package com.app.leelo.util;

/** Ready-to-use configurations so users only need to paste their API key. */
public final class AiProviders {

    public static final class Preset {
        public final String label;
        public final String endpoint;
        public final String model;
        public final String hint;

        Preset(String label, String endpoint, String model, String hint) {
            this.label = label;
            this.endpoint = endpoint;
            this.model = model;
            this.hint = hint;
        }
    }

    public static final Preset[] PRESETS = {
            new Preset(
                    "Groq",
                    "https://api.groq.com/openai/v1",
                    "llama-3.3-70b-versatile",
                    "Groq: llama-3.3-70b-versatile (calidad) o llama-3.1-8b-instant (rápido)."),
            new Preset(
                    "OpenAI",
                    "https://api.openai.com/v1",
                    "gpt-4o-mini",
                    "OpenAI: gpt-4o-mini (económico) o gpt-4o (más preciso)."),
            new Preset(
                    "OpenRouter",
                    "https://openrouter.ai/api/v1",
                    "openai/gpt-4o-mini",
                    "OpenRouter: copia el ID exacto que muestra el proveedor."),
            new Preset(
                    "Personalizado",
                    "",
                    "",
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
