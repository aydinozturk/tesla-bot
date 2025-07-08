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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TeslaInventoryBot {
    private static final Logger logger = LoggerFactory.getLogger(TeslaInventoryBot.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final PushoverNotifier pushoverNotifier;
    private final ScheduledExecutorService scheduler;

    private static final String TESLA_API_BASE_URL = "https://www.tesla.com/coinorder/api/v4/inventory-results";

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
                .protocols(java.util.Arrays.asList(Protocol.HTTP_1_1))
                .cookieJar(new okhttp3.CookieJar() {
                    @Override
                    public void saveFromResponse(okhttp3.HttpUrl url, List<okhttp3.Cookie> cookies) {
                        // Cookie'leri kaydetme - her istek için temiz başla
                    }

                    @Override
                    public List<okhttp3.Cookie> loadForRequest(okhttp3.HttpUrl url) {
                        // Boş cookie listesi döndür - her istek için temiz başla
                        return new ArrayList<>();
                    }
                })
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

    private String buildTeslaApiUrl() {
        String market = System.getenv("TESLA_MARKET");
        String language = System.getenv("TESLA_LANGUAGE");

        // Varsayılan değerler
        if (market == null || market.isEmpty()) {
            market = "DE";
        }
        if (language == null || language.isEmpty()) {
            language = "de";
        }

        // Super region belirleme
        String superRegion = "europe";
        if ("US".equals(market) || "CA".equals(market)) {
            superRegion = "north america";
        }

        String query = String.format(
                "{\"query\":{\"model\":\"my\",\"condition\":\"new\",\"options\":{},\"arrangeby\":\"Price\",\"order\":\"asc\",\"market\":\"%s\",\"language\":\"%s\",\"super_region\":\"%s\",\"lng\":\"\",\"lat\":\"\",\"zip\":\"\",\"range\":0},\"offset\":0,\"count\":24,\"outsideOffset\":0,\"outsideSearch\":false,\"isFalconDeliverySelectionEnabled\":true,\"version\":\"v2\"}",
                market, language, superRegion);

        return TESLA_API_BASE_URL + "?query="
                + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
    }

    private void checkInventory() {
        try {
            logger.info("Tesla envanter kontrol ediliyor...");

            String apiUrl = buildTeslaApiUrl();
            Request request = new Request.Builder()
                    .url(apiUrl)
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
                JsonNode results = jsonNode.path("results");

                // results bir array ise doğrudan kullan, değilse exact/approximate alt
                // yollarını dene
                int resultsCount = 0;
                if (results.isArray()) {
                    resultsCount = results.size();
                    logger.info("Toplam eşleşme: {}, Results (array): {}", totalMatches, resultsCount);
                } else {
                    JsonNode exactResults = results.path("exact");
                    JsonNode approximateResults = results.path("approximate");
                    resultsCount = exactResults.size() + approximateResults.size();
                    logger.info("Toplam eşleşme: {}, Exact: {}, Approximate: {}",
                            totalMatches, exactResults.size(), approximateResults.size());
                }

                // Hata durumunu temizle
                if (isErrorState) {
                    isErrorState = false;
                    lastErrorTime = null;
                    logger.info("API hatası düzeldi. Normal kontroller devam ediyor.");
                }

                // Yeni araç geldi mi kontrol et
                if (totalMatches > lastTotalMatches && lastTotalMatches > 0) {
                    int newCars = totalMatches - lastTotalMatches;
                    StringBuilder message = new StringBuilder();
                    message.append(String.format("🎉 Tesla envanterinde %d yeni araç bulundu!\n", newCars));
                    message.append(String.format("Toplam: %d araç\n\n", totalMatches));

                    // Yeni araçların VIN linklerini ekle
                    if (results.isArray() && results.size() > 0) {
                        message.append("🔗 Yeni Araç Linkleri:\n");
                        int maxCars = Math.min(results.size(), 5); // En fazla 5 araç göster

                        for (int i = 0; i < maxCars; i++) {
                            JsonNode car = results.get(i);
                            String vin = car.path("VIN").asText();
                            if (vin != null && !vin.isEmpty()) {
                                String carLink = String.format(
                                        "https://www.tesla.com/de_DE/my/order/%s?titleStatus=new&redirect=no#overview",
                                        vin);
                                message.append(String.format("%d. %s\n", i + 1, carLink));
                            }
                        }

                        if (results.size() > 5) {
                            message.append(String.format("... ve %d araç daha\n", results.size() - 5));
                        }
                    }

                    pushoverNotifier.sendNotification("Tesla Envanter Güncellemesi", message.toString());
                    logger.info("Yeni araç bildirimi gönderildi");
                }

                // İlk çalıştırma veya araç sayısı değişti
                if (lastTotalMatches == 0 && totalMatches > 0) {
                    StringBuilder message = new StringBuilder();
                    message.append(String.format("📊 Tesla envanterinde %d araç bulundu\n\n", totalMatches));

                    // İlk 5 aracın VIN linklerini ekle
                    if (results.isArray() && results.size() > 0) {
                        message.append("🔗 Araç Linkleri:\n");
                        int maxCars = Math.min(results.size(), 5); // En fazla 5 araç göster

                        for (int i = 0; i < maxCars; i++) {
                            JsonNode car = results.get(i);
                            String vin = car.path("VIN").asText();
                            if (vin != null && !vin.isEmpty()) {
                                String carLink = String.format(
                                        "https://www.tesla.com/de_DE/my/order/%s?titleStatus=new&redirect=no#overview",
                                        vin);
                                message.append(String.format("%d. %s\n", i + 1, carLink));
                            }
                        }

                        if (results.size() > 5) {
                            message.append(String.format("... ve %d araç daha\n", results.size() - 5));
                        }
                    }

                    pushoverNotifier.sendNotification("Tesla Envanter Durumu", message.toString());
                    logger.info("İlk envanter durumu bildirimi gönderildi");
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