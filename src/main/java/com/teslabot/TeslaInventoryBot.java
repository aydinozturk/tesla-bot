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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TeslaInventoryBot {
    private static final Logger logger = LoggerFactory.getLogger(TeslaInventoryBot.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TelegramNotifier telegramNotifier;
    private final ScheduledExecutorService scheduler;

    private static final String TESLA_API_BASE_URL = "https://www.tesla.com/coinorder/api/v4/inventory-results";

    private int lastTotalMatches = 0;
    private boolean isErrorState = false;
    private LocalDateTime lastErrorTime = null;
    private static final int ERROR_NOTIFICATION_INTERVAL_MINUTES = 30;

    private List<String> proxyList = new ArrayList<>();
    private final AtomicInteger currentProxyIndex = new AtomicInteger(0); // Proxy sırayla kullanım için
    private final Set<String> sentVins = new HashSet<>(); // Gönderilen VIN'leri takip etmek için
    private static final String SENT_VINS_FILE = "sent_vins.txt"; // VIN'leri saklamak için dosya

    private final String botActiveStart;
    private final String botActiveEnd;
    private final ZoneId istanbulZone = ZoneId.of("Europe/Istanbul");

    public TeslaInventoryBot() {
        this.objectMapper = new ObjectMapper();
        this.telegramNotifier = new TelegramNotifier();
        this.scheduler = Executors.newScheduledThreadPool(1);
        loadProxyList();
        loadSentVins(); // Gönderilen VIN'leri yükle
        this.httpClient = null; // Artık kullanılmayacak, her istekte yeni client oluşturulacak
        this.botActiveStart = System.getenv("BOT_ACTIVE_START");
        this.botActiveEnd = System.getenv("BOT_ACTIVE_END");
    }

    private void loadProxyList() {
        try (BufferedReader br = new BufferedReader(new FileReader("proxy-list.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && line.contains(":")) {
                    proxyList.add(line);
                }
            }
            logger.info("{} adet proxy yüklendi", proxyList.size());
        } catch (Exception e) {
            logger.error("Proxy listesi yüklenemedi: {}", e.getMessage());
        }
    }

    private void loadSentVins() {
        try {
            if (Files.exists(Paths.get(SENT_VINS_FILE))) {
                List<String> lines = Files.readAllLines(Paths.get(SENT_VINS_FILE));
                for (String line : lines) {
                    String vin = line.trim();
                    if (!vin.isEmpty()) {
                        sentVins.add(vin);
                    }
                }
                logger.info("{} adet gönderilmiş VIN yüklendi", sentVins.size());
            } else {
                logger.info("Gönderilmiş VIN dosyası bulunamadı, yeni dosya oluşturulacak");
            }
        } catch (Exception e) {
            logger.error("Gönderilmiş VIN'ler yüklenemedi: {}", e.getMessage());
        }
    }

    private void saveSentVins() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(SENT_VINS_FILE))) {
            for (String vin : sentVins) {
                writer.println(vin);
            }
            logger.info("{} adet VIN dosyaya kaydedildi", sentVins.size());
        } catch (Exception e) {
            logger.error("VIN'ler dosyaya kaydedilemedi: {}", e.getMessage());
        }
    }

    private Proxy getNextProxy() {
        if (proxyList.isEmpty())
            return Proxy.NO_PROXY;

        int index = currentProxyIndex.getAndIncrement() % proxyList.size();
        String proxyStr = proxyList.get(index);
        String[] parts = proxyStr.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        logger.debug("Proxy kullanılıyor: {} (index: {})", proxyStr, index);
        return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }

    private OkHttpClient buildHttpClientWithProxy(Proxy proxy) {
        return new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .protocols(java.util.Arrays.asList(Protocol.HTTP_1_1))
                .proxy(proxy)
                .cookieJar(new okhttp3.CookieJar() {
                    @Override
                    public void saveFromResponse(okhttp3.HttpUrl url, List<okhttp3.Cookie> cookies) {
                    }

                    @Override
                    public List<okhttp3.Cookie> loadForRequest(okhttp3.HttpUrl url) {
                        return new ArrayList<>();
                    }
                })
                .build();
    }

    private boolean isWithinActiveHours() {
        if (botActiveStart == null || botActiveEnd == null) {
            return true; // Saat aralığı tanımlı değilse her zaman çalış
        }
        try {
            LocalTime now = LocalTime.now(istanbulZone);
            LocalTime start = LocalTime.parse(botActiveStart);
            LocalTime end = LocalTime.parse(botActiveEnd);
            if (start.isBefore(end)) {
                return !now.isBefore(start) && !now.isAfter(end);
            } else {
                // Gece yarısı aralığı (örn: 22:00 - 06:00)
                return !now.isBefore(start) || !now.isAfter(end);
            }
        } catch (Exception e) {
            logger.warn("Çalışma saati kontrolünde hata: {}", e.getMessage());
            return true;
        }
    }

    public void start() {
        logger.info("Tesla Envanter Bot başlatılıyor...");

        try {
            // Bot başlatma bildirimi gönder
            telegramNotifier.sendNotification("Tesla Bot Başlatıldı",
                    "🚀 Tesla Envanter Bot başarıyla başlatıldı ve çalışıyor.");

            // İlk kontrolü hemen yap
            checkInventory();

            // Her 10 saniye kontrol et
            scheduler.scheduleAtFixedRate(this::checkInventory, 10, 10, TimeUnit.SECONDS);

            logger.info("Bot başlatıldı. Her 10 saniye Tesla envanteri kontrol edilecek.");

        } catch (Exception e) {
            logger.error("Bot başlatılırken hata oluştu: {}", e.getMessage());
            telegramNotifier.sendErrorNotification("Tesla Bot Başlatma Hatası",
                    "❌ Bot başlatılırken hata oluştu: " + e.getMessage());
            throw e;
        }
    }

    public void stop() {
        logger.info("Bot durduruluyor...");

        try {
            // VIN'leri kaydet
            saveSentVins();

            // Bot kapatma bildirimi gönder
            telegramNotifier.sendNotification("Tesla Bot Durduruldu",
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
                telegramNotifier.sendErrorNotification("Tesla Bot Durdurma Hatası",
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

    private String buildTeslaCarLink(String vin) {
        String market = System.getenv("TESLA_MARKET");
        String language = System.getenv("TESLA_LANGUAGE");

        // Varsayılan değerler
        if (market == null || market.isEmpty()) {
            market = "DE";
        }
        if (language == null || language.isEmpty()) {
            language = "de";
        }

        // Market ve language'i birleştir (örn: DE + de = de_DE)
        String locale = language.toLowerCase() + "_" + market.toUpperCase();

        return String.format(
                "https://www.tesla.com/%s/my/order/%s?titleStatus=new&redirect=no#overview",
                locale, vin);
    }

    private String buildCarDetailsMessage(JsonNode car, int carIndex, int totalCars) {
        StringBuilder message = new StringBuilder();

        // Temel bilgiler
        String vin = car.path("VIN").asText("");
        String model = car.path("Model").asText("");
        String trimName = car.path("TrimName").asText("");
        String year = car.path("Year").asText("");
        String price = car.path("Price").asText("");
        String currency = car.path("CurrencyCode").asText("");

        // Başlık
        message.append(String.format("%s %s %s\n", year, model, trimName));

        // Fiyat bilgisi
        if (price != null && !price.isEmpty()) {
            message.append(String.format("Fiyat: %s %s\n", price, currency));
        }

        // VIN
        if (vin != null && !vin.isEmpty()) {
            message.append(String.format("VIN: %s\n", vin));
        }

        // Renk
        JsonNode paint = car.path("PAINT");
        if (paint.isArray() && paint.size() > 0) {
            String paintColor = paint.get(0).asText();
            if (paintColor != null && !paintColor.isEmpty()) {
                message.append(String.format("Renk: %s\n", paintColor));
            }
        }

        // İç mekan
        JsonNode interior = car.path("INTERIOR");
        if (interior.isArray() && interior.size() > 0) {
            String interiorColor = interior.get(0).asText();
            if (interiorColor != null && !interiorColor.isEmpty()) {
                message.append(String.format("İç Mekan: %s\n", interiorColor));
            }
        }

        // Tesla link
        if (vin != null && !vin.isEmpty()) {
            String carLink = buildTeslaCarLink(vin);
            message.append(String.format("\nTesla'da Görüntüle: %s", carLink));
        }

        logger.info("Araç detayları oluşturuldu: VIN={}, Model={}, Fiyat={}", vin, model, price);

        return message.toString();
    }

    private void checkInventory() {
        if (!isWithinActiveHours()) {
            logger.info("Bot şu an çalışma saatleri dışında. Kontrol atlandı. (Saat aralığı: {} - {})", botActiveStart,
                    botActiveEnd);
            return;
        }
        try {
            logger.info("Tesla envanter kontrol ediliyor...");

            String apiUrl = buildTeslaApiUrl();
            Proxy proxy = getNextProxy();
            OkHttpClient httpClient = buildHttpClientWithProxy(proxy);
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .addHeader("Accept", "application/json")
                    .addHeader("Referer", "https://www.tesla.com/tr_tr/inventory/new/my")
                    .addHeader("priority", "u=1, i")
                    .addHeader("sec-ch-ua",
                            "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"Google Chrome\";v=\"138\"")
                    .addHeader("sec-ch-ua-mobile", "?0")
                    .addHeader("sec-ch-ua-platform", "\"macOS\"")
                    .addHeader("sec-fetch-dest", "empty")
                    .addHeader("sec-fetch-mode", "cors")
                    .addHeader("sec-fetch-site", "same-origin")
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
                if (totalMatches != lastTotalMatches && lastTotalMatches > 0) {
                    int newCars = totalMatches - lastTotalMatches;
                    StringBuilder message = new StringBuilder();
                    message.append(String.format("🎉 Tesla envanterinde %d yeni araç bulundu!\n", newCars));
                    message.append(String.format("Toplam: %d araç\n\n", totalMatches));

                    // Yeni araçların detaylarını tek tek gönder
                    if (results.isArray() && results.size() > 0) {
                        logger.info("{} yeni araç bulundu, detaylı mesajlar gönderiliyor...", results.size());
                        for (int i = 0; i < results.size(); i++) {
                            JsonNode car = results.get(i);
                            String vin = car.path("VIN").asText("");
                            if (vin != null && !vin.isEmpty() && !sentVins.contains(vin)) {
                                String carDetails = buildCarDetailsMessage(car, i + 1, results.size());
                                logger.info("Araç {} için mesaj gönderiliyor...", i + 1);
                                telegramNotifier.sendNewCarNotification("🚗 Yeni Tesla Araç", carDetails);
                                sentVins.add(vin);
                                saveSentVins(); // VIN'i kaydet
                                logger.info("VIN {} gönderildi ve kaydedildi", vin);
                            } else {
                                logger.debug("Araç {} VIN ({}) zaten gönderildi veya VIN yok.", i + 1, vin);
                            }
                        }
                    } else {
                        logger.warn("Results array boş veya null: {}", results);
                    }

                    telegramNotifier.sendInventoryUpdate("Tesla Envanter Güncellemesi", message.toString());
                    logger.info("Yeni araç bildirimi gönderildi");
                }

                // İlk çalıştırma veya araç sayısı değişti
                if (lastTotalMatches == 0 && totalMatches > 0) {
                    StringBuilder message = new StringBuilder();
                    message.append(String.format("📊 Tesla envanterinde %d araç bulundu\n\n", totalMatches));
                    telegramNotifier.sendInventoryUpdate("Tesla Envanter Durumu", message.toString());

                    // Araçların detaylarını tek tek gönder
                    if (results.isArray() && results.size() > 0) {
                        for (int i = 0; i < results.size(); i++) {
                            JsonNode car = results.get(i);
                            String vin = car.path("VIN").asText("");
                            if (vin != null && !vin.isEmpty() && !sentVins.contains(vin)) {
                                String carDetails = buildCarDetailsMessage(car, i + 1, results.size());
                                telegramNotifier.sendNewCarNotification("🚗 Tesla Araç", carDetails);
                                sentVins.add(vin);
                                saveSentVins(); // VIN'i kaydet
                                logger.info("İlk başlatmada VIN {} gönderildi ve kaydedildi", vin);
                            } else {
                                logger.debug("Araç {} VIN ({}) zaten gönderildi veya VIN yok.", i + 1, vin);
                            }
                        }
                    }
                    logger.info("İlk başlatmada araç detayları gönderildi. Toplam: {}", results.size());
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
            telegramNotifier.sendErrorNotification("Tesla Bot Hatası",
                    "❌ Tesla API'sine erişim hatası: " + errorMessage);
            logger.info("İlk hata bildirimi gönderildi");
        } else {
            // Sürekli hata durumu - 30 dakika kontrol et
            if (lastErrorTime != null &&
                    LocalDateTime.now().minusMinutes(ERROR_NOTIFICATION_INTERVAL_MINUTES).isAfter(lastErrorTime)) {

                telegramNotifier.sendErrorNotification("Tesla Bot Sürekli Hata",
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