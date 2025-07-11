package com.teslabot;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TelegramNotifier {
    private static final Logger logger = LoggerFactory.getLogger(TelegramNotifier.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String botToken;
    private final String chatId;
    private final String newCarsChatId; // Yeni araçlar için ayrı chat ID

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    public TelegramNotifier() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();

        // Environment variables'dan Telegram bilgilerini al
        this.botToken = System.getenv("TELEGRAM_BOT_TOKEN");
        this.chatId = System.getenv("TELEGRAM_CHAT_ID");
        this.newCarsChatId = System.getenv("TELEGRAM_NEW_CARS_CHAT_ID"); // Yeni araçlar için chat ID

        if (botToken == null || chatId == null) {
            logger.warn(
                    "Telegram bilgileri eksik! TELEGRAM_BOT_TOKEN ve TELEGRAM_CHAT_ID environment variable'larını ayarlayın.");
        }

        if (newCarsChatId == null) {
            logger.info("TELEGRAM_NEW_CARS_CHAT_ID ayarlanmamış. Yeni araçlar ana chat ID'ye gönderilecek.");
        }
    }

    public void sendNotification(String title, String message) {
        if (botToken == null || chatId == null) {
            logger.error("Telegram bilgileri eksik olduğu için bildirim gönderilemedi");
            return;
        }

        try {
            String fullMessage = String.format("🔔 *%s*\n\n%s", title, message);

            RequestBody formBody = new FormBody.Builder()
                    .add("chat_id", chatId)
                    .add("text", fullMessage)
                    .add("parse_mode", "Markdown")
                    .add("disable_web_page_preview", "true")
                    .build();

            Request request = new Request.Builder()
                    .url(TELEGRAM_API_URL + botToken + "/sendMessage")
                    .post(formBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("Telegram bildirimi başarıyla gönderildi: {}", title);
                } else {
                    logger.error("Telegram bildirimi gönderilemedi. HTTP: {} - {}",
                            response.code(), response.message());
                }
            }

        } catch (IOException e) {
            logger.error("Telegram bildirimi gönderilirken hata oluştu: {}", e.getMessage());
        }
    }

    public void sendErrorNotification(String title, String message) {
        if (botToken == null || chatId == null) {
            logger.error("Telegram bilgileri eksik olduğu için hata bildirimi gönderilemedi");
            return;
        }

        try {
            String fullMessage = String.format("🚨 *%s*\n\n%s", title, message);

            RequestBody formBody = new FormBody.Builder()
                    .add("chat_id", chatId)
                    .add("text", fullMessage)
                    .add("parse_mode", "Markdown")
                    .add("disable_web_page_preview", "true")
                    .build();

            Request request = new Request.Builder()
                    .url(TELEGRAM_API_URL + botToken + "/sendMessage")
                    .post(formBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("Telegram hata bildirimi başarıyla gönderildi: {}", title);
                } else {
                    logger.error("Telegram hata bildirimi gönderilemedi. HTTP: {} - {}",
                            response.code(), response.message());
                }
            }

        } catch (IOException e) {
            logger.error("Telegram hata bildirimi gönderilirken hata oluştu: {}", e.getMessage());
        }
    }

    public void sendInventoryUpdate(String title, String message) {
        if (botToken == null || chatId == null) {
            logger.error("Telegram bilgileri eksik olduğu için envanter güncellemesi gönderilemedi");
            return;
        }

        try {
            String fullMessage = title + "\n\n" + message;

            RequestBody formBody = new FormBody.Builder()
                    .add("chat_id", chatId)
                    .add("text", fullMessage)
                    // .add("parse_mode", "Markdown") // KALDIRILDI
                    .add("disable_web_page_preview", "false")
                    .build();

            Request request = new Request.Builder()
                    .url(TELEGRAM_API_URL + botToken + "/sendMessage")
                    .post(formBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("Telegram envanter güncellemesi başarıyla gönderildi: {}", title);
                } else {
                    logger.error("Telegram envanter güncellemesi gönderilemedi. HTTP: {} - {}",
                            response.code(), response.message());
                }
            }

        } catch (IOException e) {
            logger.error("Telegram envanter güncellemesi gönderilirken hata oluştu: {}", e.getMessage());
        }
    }

    // Yeni araçlar için ayrı chat ID'ye gönderme metodu
    public void sendNewCarNotification(String title, String message) {
        if (botToken == null) {
            logger.error("Telegram bot token eksik olduğu için yeni araç bildirimi gönderilemedi");
            return;
        }

        // Eğer yeni araçlar için ayrı chat ID varsa onu kullan, yoksa ana chat ID'yi
        // kullan
        String targetChatId = (newCarsChatId != null && !newCarsChatId.isEmpty()) ? newCarsChatId : chatId;

        if (targetChatId == null) {
            logger.error("Chat ID eksik olduğu için yeni araç bildirimi gönderilemedi");
            return;
        }

        try {
            String fullMessage = title + "\n\n" + message;

            RequestBody formBody = new FormBody.Builder()
                    .add("chat_id", targetChatId)
                    .add("text", fullMessage)
                    .add("disable_web_page_preview", "false")
                    .build();

            Request request = new Request.Builder()
                    .url(TELEGRAM_API_URL + botToken + "/sendMessage")
                    .post(formBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("Yeni araç bildirimi başarıyla gönderildi (Chat ID: {}): {}", targetChatId, title);
                } else {
                    logger.error("Yeni araç bildirimi gönderilemedi. HTTP: {} - {}",
                            response.code(), response.message());
                }
            }

        } catch (IOException e) {
            logger.error("Yeni araç bildirimi gönderilirken hata oluştu: {}", e.getMessage());
        }
    }
}