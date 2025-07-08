package com.teslabot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TeslaInventoryBot {
    private static final Logger logger = LoggerFactory.getLogger(TeslaInventoryBot.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final PushoverNotifier pushoverNotifier;
    private final ScheduledExecutorService scheduler;

    private static final String TESLA_API_URL = "https://www.tesla.com/coinorder/api/v4/inventory-results?query=%7B%22query%22%3A%7B%22model%22%3A%22my%22%2C%22condition%22%3A%22new%22%2C%22options%22%3A%7B%7D%2C%22arrangeby%22%3A%22Price%22%2C%22order%22%3A%22asc%22%2C%22market%22%3A%22TR%22%2C%22language%22%3A%22tr%22%2C%22super_region%22%3A%22north%20america%22%2C%22lng%22%3A%22%22%2C%22lat%22%3A%22%22%2C%22zip%22%3A%22%22%2C%22range%22%3A0%7D%2C%22offset%22%3A0%2C%22count%22%3A24%2C%22outsideOffset%22%3A0%2C%22outsideSearch%22%3Afalse%2C%22isFalconDeliverySelectionEnabled%22%3Atrue%2C%22version%22%3A%22v2%22%7D";

    private int lastTotalMatches = 0;
    private boolean isErrorState = false;
    private LocalDateTime lastErrorTime = null;
    private static final int ERROR_NOTIFICATION_INTERVAL_MINUTES = 30;

    public TeslaInventoryBot() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .protocols(java.util.Arrays.asList(Protocol.HTTP_1_1)) // HTTP/2 sorunlarını önle
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) // Bağlantı havuzu
                .build();
        this.objectMapper = new ObjectMapper();
        this.pushoverNotifier = new PushoverNotifier();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        logger.info("Tesla Envanter Bot başlatılıyor...");

        try {
            // Bot başlatma bildirimi gönder
            pushoverNotifier.sendNotification("Tesla Bot Başlatıldı",
                    "🚀 Tesla Envanter Bot başarıyla başlatıldı ve çalışıyor.");

            // İlk kontrolü hemen yap
            checkInventory();

            // Her dakika kontrol et
            scheduler.scheduleAtFixedRate(this::checkInventory, 1, 1, TimeUnit.MINUTES);

            logger.info("Bot başlatıldı. Her dakika Tesla envanteri kontrol edilecek.");

        } catch (Exception e) {
            logger.error("Bot başlatılırken hata oluştu: {}", e.getMessage());
            pushoverNotifier.sendErrorNotification("Tesla Bot Başlatma Hatası",
                    "❌ Bot başlatılırken hata oluştu: " + e.getMessage());
            throw e;
        }
    }

    public void stop() {
        logger.info("Bot durduruluyor...");

        try {
            // Bot kapatma bildirimi gönder
            pushoverNotifier.sendNotification("Tesla Bot Durduruldu",
                    "🛑 Tesla Envanter Bot durduruldu.");

            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                    logger.warn("Bot zorla durduruldu.");
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
                logger.warn("Bot durdurulurken interrupt edildi.");
            }

            logger.info("Bot durduruldu.");

        } catch (Exception e) {
            logger.error("Bot durdurulurken hata oluştu: {}", e.getMessage());
            // Durdurma sırasında hata olsa bile bildirim göndermeye çalış
            try {
                pushoverNotifier.sendErrorNotification("Tesla Bot Durdurma Hatası",
                        "⚠️ Bot durdurulurken hata oluştu: " + e.getMessage());
            } catch (Exception notificationError) {
                logger.error("Durdurma hatası bildirimi gönderilemedi: {}", notificationError.getMessage());
            }
        }
    }

    private void checkInventory() {
        try {
            logger.debug("Tesla envanter kontrol ediliyor...");

            Request request = new Request.Builder()
                    .url(TESLA_API_URL)
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .addHeader("Accept", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    handleError("HTTP Hatası: " + response.code() + " " + response.message());
                    return;
                }

                String responseBody = response.body().string();
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                int totalMatches = jsonNode.path("total_matches_found").asInt();
                JsonNode exactResults = jsonNode.path("results").path("exact");
                JsonNode approximateResults = jsonNode.path("results").path("approximate");

                logger.debug("Toplam eşleşme: {}, Exact: {}, Approximate: {}",
                        totalMatches, exactResults.size(), approximateResults.size());

                // Hata durumunu temizle
                if (isErrorState) {
                    isErrorState = false;
                    lastErrorTime = null;
                    logger.info("API hatası düzeldi. Normal kontroller devam ediyor.");
                }

                // Yeni araç geldi mi kontrol et
                if (totalMatches > lastTotalMatches && lastTotalMatches > 0) {
                    int newCars = totalMatches - lastTotalMatches;
                    String message = String.format("🎉 Tesla envanterinde %d yeni araç bulundu! " +
                            "Toplam: %d araç", newCars, totalMatches);

                    pushoverNotifier.sendNotification("Tesla Envanter Güncellemesi", message);
                    logger.info("Yeni araç bildirimi gönderildi: {}", message);
                }

                // İlk çalıştırma veya araç sayısı değişti
                if (lastTotalMatches == 0 && totalMatches > 0) {
                    String message = String.format("📊 Tesla envanterinde %d araç bulundu", totalMatches);
                    pushoverNotifier.sendNotification("Tesla Envanter Durumu", message);
                    logger.info("İlk envanter durumu bildirimi: {}", message);
                }

                lastTotalMatches = totalMatches;

            } catch (IOException e) {
                handleError("API isteği hatası: " + e.getMessage());
            }

        } catch (Exception e) {
            handleError("Beklenmeyen hata: " + e.getMessage());
        }
    }

    private void handleError(String errorMessage) {
        logger.error("Hata: {}", errorMessage);

        if (!isErrorState) {
            // İlk hata
            isErrorState = true;
            lastErrorTime = LocalDateTime.now();
            pushoverNotifier.sendNotification("Tesla Bot Hatası",
                    "❌ Tesla API'sine erişim hatası: " + errorMessage);
            logger.info("İlk hata bildirimi gönderildi");
        } else {
            // Sürekli hata durumu - 30 dakika kontrol et
            if (lastErrorTime != null &&
                    LocalDateTime.now().minusMinutes(ERROR_NOTIFICATION_INTERVAL_MINUTES).isAfter(lastErrorTime)) {

                pushoverNotifier.sendNotification("Tesla Bot Sürekli Hata",
                        "⚠️ Tesla API hatası 30 dakikadır devam ediyor: " + errorMessage);
                lastErrorTime = LocalDateTime.now();
                logger.info("Sürekli hata bildirimi gönderildi");
            }
        }
    }

    public static void main(String[] args) {
        TeslaInventoryBot bot = new TeslaInventoryBot();

        // Graceful shutdown için shutdown hook ekle
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook tetiklendi...");
            bot.stop();
        }));

        try {
            bot.start();
        } catch (Exception e) {
            logger.error("Bot başlatılamadı: {}", e.getMessage());
            System.exit(1);
        }
    }
}