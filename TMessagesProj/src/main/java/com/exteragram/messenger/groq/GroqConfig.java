package com.exteragram.messenger.groq;

public class GroqConfig {

    public static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public static final String DEFAULT_PROMPT =
            "Rewrite the following text to improve its structure and clarity without changing the meaning. " +
            "Focus only on organization, flow, and readability. " +
            "Do not add new information, opinions, suggestions, or commentary. " +
            "Preserve all original content but reorganize sentences and paragraphs if needed for logical order. " +
            "Provide the rewritten text only, with no extra explanation: ";

    public static final String MODEL_LLAMA_70B    = "llama-3.3-70b-versatile";
    public static final String MODEL_LLAMA_8B     = "llama-3.1-8b-instant";
    public static final String MODEL_LLAMA_3B     = "llama-3.2-3b-preview";
    public static final String MODEL_GEMMA_9B     = "gemma2-9b-it";
    public static final String MODEL_MIXTRAL_8X7B = "mixtral-8x7b-32768";

    public static final String[] MODEL_IDS = {
            MODEL_LLAMA_70B,
            MODEL_LLAMA_8B,
            MODEL_LLAMA_3B,
            MODEL_GEMMA_9B,
            MODEL_MIXTRAL_8X7B
    };

    public static final String[] MODEL_NAMES = {
            "Llama 3.3 70B (лучшее качество)",
            "Llama 3.1 8B (быстрый)",
            "Llama 3.2 3B (самый быстрый)",
            "Gemma 2 9B (Google)",
            "Mixtral 8x7B (длинный контекст)"
    };

    public static final int CONNECT_TIMEOUT_SEC = 15;
    public static final int READ_TIMEOUT_SEC    = 30;
    public static final int WRITE_TIMEOUT_SEC   = 15;

    public static final float TEMPERATURE = 0.3f;
    public static final int   MAX_TOKENS  = 4096;
}
