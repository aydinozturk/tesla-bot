package com.teslabot;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class PushoverNotifier {
    private static final Logger logger = LoggerFactory.getLogger(PushoverNotifier.class);
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String userKey;
    private final String appToken;
    
    private static final String PUSHOVER_API_URL = "https://api.pushover.net/1/messages.json";
    
    public PushoverNotifier() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        
        // Environment variables'dan Pushover bilgilerini al
        this.userKey = System.getenv("PUSHOVER_USER_KEY");
        this.appToken = System.getenv("PUSHOVER_APP_TOKEN");
        
        if (userKey == null || appToken == null) {
            logger.warn("Pushover bilgileri eksik! PUSHOVER_USER_KEY ve PUSHOVER_APP_TOKEN environment variable'larını ayarlayın.");
        }
    }
    
    public void sendNotification(String title, String message) {
        if (userKey == null || appToken == null) {
            logger.error("Pushover bilgileri eksik olduğu için bildirim gönderilemedi");
            return;
        }
        
        try {
            RequestBody formBody = new FormBody.Builder()
                    .add("token", appToken)
                    .add("user", userKey)
                    .add("title", title)
                    .add("message", message)
                    .add("priority", "1") // Normal priority
                    .add("sound", "cosmic") // Bildirim sesi
                    .build();
            
            Request request = new Request.Builder()
                    .url(PUSHOVER_API_URL)
                    .post(formBody)
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("Pushover bildirimi başarıyla gönderildi: {}", title);
                } else {
                    logger.error("Pushover bildirimi gönderilemedi. HTTP: {} - {}", 
                               response.code(), response.message());
                }
            }
            
        } catch (IOException e) {
            logger.error("Pushover bildirimi gönderilirken hata oluştu: {}", e.getMessage());
        }
    }
    
    public void sendErrorNotification(String title, String message) {
        if (userKey == null || appToken == null) {
            logger.error("Pushover bilgileri eksik olduğu için hata bildirimi gönderilemedi");
            return;
        }
        
        try {
            RequestBody formBody = new FormBody.Builder()
                    .add("token", appToken)
                    .add("user", userKey)
                    .add("title", title)
                    .add("message", message)
                    .add("priority", "2") // Yüksek priority (hata durumu için)
                    .add("sound", "siren") // Acil durum sesi
                    .build();
            
            Request request = new Request.Builder()
                    .url(PUSHOVER_API_URL)
                    .post(formBody)
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("Pushover hata bildirimi başarıyla gönderildi: {}", title);
                } else {
                    logger.error("Pushover hata bildirimi gönderilemedi. HTTP: {} - {}", 
                               response.code(), response.message());
                }
            }
            
        } catch (IOException e) {
            logger.error("Pushover hata bildirimi gönderilirken hata oluştu: {}", e.getMessage());
        }
    }
} 