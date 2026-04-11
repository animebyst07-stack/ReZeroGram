package com.exteragram.messenger.groq;

public interface GroqCallback {
    void onSuccess(String restructuredText);
    void onError(String errorMessage);
    void onProcessing();
}
