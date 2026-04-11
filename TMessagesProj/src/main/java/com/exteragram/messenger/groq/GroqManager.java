package com.exteragram.messenger.groq;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GroqManager {

    private static final String TAG = "GroqManager";
    private static final String PREFS_NAME = "zerogram_groq";
    private static final String KEY_API_KEY       = "groq_api_key";
    private static final String KEY_MODEL         = "groq_model";
    private static final String KEY_ENABLED       = "groq_enabled";
    private static final String KEY_CUSTOM_PROMPT = "groq_custom_prompt";

    private static volatile GroqManager instance;

    private final OkHttpClient httpClient;
    private final Handler mainHandler;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private SharedPreferences prefs;
    private String apiKey      = "";
    private String model       = GroqConfig.MODEL_LLAMA_70B;
    private boolean enabled    = false;
    private String customPrompt = GroqConfig.DEFAULT_PROMPT;

    private Call activeCall;

    private GroqManager() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(GroqConfig.CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(GroqConfig.READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(GroqConfig.WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
                .build();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static GroqManager getInstance() {
        if (instance == null) {
            synchronized (GroqManager.class) {
                if (instance == null) {
                    instance = new GroqManager();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        apiKey       = prefs.getString(KEY_API_KEY, "");
        model        = prefs.getString(KEY_MODEL, GroqConfig.MODEL_LLAMA_70B);
        enabled      = prefs.getBoolean(KEY_ENABLED, false);
        customPrompt = prefs.getString(KEY_CUSTOM_PROMPT, GroqConfig.DEFAULT_PROMPT);
        FileLog.d(TAG + " initialized. enabled=" + enabled);
    }

    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isEmpty();
    }

    public boolean isProcessing() {
        return isProcessing.get();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    public String getCustomPrompt() {
        return customPrompt;
    }

    public void setApiKey(String key) {
        this.apiKey = key != null ? key.trim() : "";
        if (prefs != null) {
            prefs.edit().putString(KEY_API_KEY, this.apiKey).apply();
        }
        if (!this.apiKey.isEmpty() && !enabled) {
            setEnabled(true);
        }
    }

    public void setModel(String modelId) {
        this.model = modelId;
        if (prefs != null) {
            prefs.edit().putString(KEY_MODEL, modelId).apply();
        }
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
        if (prefs != null) {
            prefs.edit().putBoolean(KEY_ENABLED, value).apply();
        }
    }

    public void setCustomPrompt(String prompt) {
        this.customPrompt = (prompt != null && !prompt.trim().isEmpty())
                ? prompt.trim()
                : GroqConfig.DEFAULT_PROMPT;
        if (prefs != null) {
            prefs.edit().putString(KEY_CUSTOM_PROMPT, this.customPrompt).apply();
        }
    }

    public void resetPromptToDefault() {
        setCustomPrompt(GroqConfig.DEFAULT_PROMPT);
    }

    public void restructureText(String text, GroqCallback callback) {
        if (!isEnabled()) {
            deliverError(callback, "Groq не активен. Укажите API-ключ в настройках.");
            return;
        }
        if (text == null || text.trim().isEmpty()) {
            deliverError(callback, "Текст пуст.");
            return;
        }
        if (text.trim().length() < 10) {
            deliverSuccess(callback, text);
            return;
        }
        if (isProcessing.getAndSet(true)) {
            deliverError(callback, "Обработка уже идёт. Подождите.");
            return;
        }
        deliverProcessing(callback);
        sendRequest(customPrompt + text.trim(), callback, false);
    }

    public void testConnection(GroqCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            deliverError(callback, "API-ключ не указан.");
            return;
        }
        if (isProcessing.getAndSet(true)) {
            deliverError(callback, "Обработка уже идёт.");
            return;
        }
        deliverProcessing(callback);
        sendRequest("Ответь одним словом: работает", callback, true);
    }

    private void sendRequest(String prompt, GroqCallback callback, boolean isTest) {
        try {
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);

            JSONArray messages = new JSONArray();
            messages.put(message);

            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("messages", messages);
            body.put("temperature", GroqConfig.TEMPERATURE);
            body.put("max_tokens", isTest ? 10 : GroqConfig.MAX_TOKENS);
            body.put("stream", false);

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(GroqConfig.API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            activeCall = httpClient.newCall(request);
            activeCall.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    isProcessing.set(false);
                    if (!call.isCanceled()) {
                        FileLog.e(TAG, e);
                        deliverError(callback, "Ошибка соединения: " + e.getMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    isProcessing.set(false);
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody == null) {
                            deliverError(callback, "Пустой ответ от сервера.");
                            return;
                        }
                        String responseStr = responseBody.string();
                        if (!response.isSuccessful()) {
                            String errMsg = parseErrorMessage(responseStr, response.code());
                            deliverError(callback, errMsg);
                            return;
                        }
                        String result = parseSuccessResponse(responseStr, isTest);
                        deliverSuccess(callback, result);
                    } catch (Exception e) {
                        FileLog.e(TAG, e);
                        deliverError(callback, "Ошибка разбора ответа: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            isProcessing.set(false);
            FileLog.e(TAG, e);
            deliverError(callback, "Ошибка запроса: " + e.getMessage());
        }
    }

    private String parseSuccessResponse(String json, boolean isTest) throws Exception {
        JSONObject obj = new JSONObject(json);
        JSONArray choices = obj.getJSONArray("choices");
        if (choices.length() == 0) throw new Exception("Пустой список choices");
        JSONObject choice = choices.getJSONObject(0);
        JSONObject msgObj = choice.getJSONObject("message");
        String content = msgObj.getString("content").trim();
        if (isTest) {
            return "Подключение успешно! (" + content + ")";
        }
        return content;
    }

    private String parseErrorMessage(String json, int httpCode) {
        try {
            JSONObject obj = new JSONObject(json);
            if (obj.has("error")) {
                JSONObject errObj = obj.getJSONObject("error");
                return errObj.optString("message", "HTTP " + httpCode);
            }
        } catch (Exception ignored) {
        }
        return "HTTP " + httpCode;
    }

    public void cancelRequest() {
        if (activeCall != null && !activeCall.isCanceled()) {
            activeCall.cancel();
        }
        isProcessing.set(false);
    }

    private void deliverSuccess(GroqCallback callback, String text) {
        if (callback == null) return;
        mainHandler.post(() -> callback.onSuccess(text));
    }

    private void deliverError(GroqCallback callback, String msg) {
        if (callback == null) return;
        mainHandler.post(() -> callback.onError(msg));
    }

    private void deliverProcessing(GroqCallback callback) {
        if (callback == null) return;
        mainHandler.post(callback::onProcessing);
    }
}
