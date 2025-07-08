# 🚗 Tesla Envanter Kontrol Botu

Bu bot, Tesla'nın resmi envanter API'sini düzenli olarak kontrol eder ve yeni araç geldiğinde veya hata durumunda Pushover üzerinden bildirim gönderir.

## ✨ Özellikler

- 🕐 **Her dakika otomatik kontrol** - Tesla envanterini sürekli izler
- 📱 **Pushover bildirimleri** - Yeni araç ve hata durumlarında anında bildirim
- ⚠️ **Akıllı hata yönetimi** - İlk hata ve 30 dakika sürekli hata bildirimleri
- 🔄 **Otomatik yeniden deneme** - Bağlantı sorunlarında otomatik retry
- 📊 **Detaylı loglama** - Tüm işlemler loglanır ve saklanır
- 🛡️ **Graceful shutdown** - Güvenli kapatma desteği
- 🐳 **Docker desteği** - Container ortamında çalıştırma
- 🔧 **Kolay kurulum** - Tek komutla başlatma

## 🚀 Hızlı Başlangıç

### Gereksinimler

- Java 11 veya üzeri
- Maven 3.6+
- Pushover hesabı

### 1. Pushover Kurulumu

1. [Pushover](https://pushover.net/) sitesine gidin ve hesap oluşturun
2. Bir uygulama oluşturun ve API token alın
3. User key'inizi not edin

### 2. Environment Variables

```bash
export PUSHOVER_USER_KEY="your_user_key_here"
export PUSHOVER_APP_TOKEN="your_app_token_here"
```

### 3. Botu Başlatma

```bash
# Projeyi derle
mvn clean package

# Botu çalıştır
java -jar target/tesla-inventory-bot-1.0.0.jar
```

**Veya otomatik başlatma scripti kullanın:**

```bash
chmod +x start.sh
./start.sh
```

## 📋 Kullanım

### Normal Çalışma

Bot başlatıldığında:

1. İlk olarak Tesla envanterini kontrol eder
2. Her dakika düzenli kontrol yapar
3. Yeni araç geldiğinde bildirim gönderir
4. API hatalarında bildirim gönderir
5. 30 dakika sürekli hata durumunda tekrar bildirim gönderir

### Bildirim Türleri

#### 🎉 Yeni Araç Bildirimi

```
🎉 Tesla envanterinde 3 yeni araç bulundu! Toplam: 15 araç
```

#### ❌ İlk Hata Bildirimi

```
❌ Tesla API'sine erişim hatası: timeout
```

#### ⚠️ Sürekli Hata Bildirimi

```
⚠️ Tesla API hatası 30 dakikadır devam ediyor: timeout
```

### Loglar

Bot logları `logs/tesla-bot.log` dosyasında saklanır:

```bash
# Canlı log takibi
tail -f logs/tesla-bot.log

# Son 50 satır
tail -50 logs/tesla-bot.log
```

## 🐳 Docker ile Çalıştırma

### Docker Compose (Önerilen)

```bash
# Environment variables ayarla
export PUSHOVER_USER_KEY="your_user_key"
export PUSHOVER_APP_TOKEN="your_app_token"

# Botu başlat
docker-compose up -d

# Logları takip et
docker-compose logs -f tesla-bot
```

### Manuel Docker

```bash
# Image oluştur
docker build -t tesla-inventory-bot .

# Container çalıştır
docker run -d \
  --name tesla-bot \
  -e PUSHOVER_USER_KEY="your_user_key" \
  -e PUSHOVER_APP_TOKEN="your_app_token" \
  -v $(pwd)/logs:/app/logs \
  tesla-inventory-bot
```

## ⚙️ Konfigürasyon

### Environment Variables

| Değişken             | Açıklama           | Zorunlu |
| -------------------- | ------------------ | ------- |
| `PUSHOVER_USER_KEY`  | Pushover user key  | ✅      |
| `PUSHOVER_APP_TOKEN` | Pushover app token | ✅      |

### Teknik Detaylar

- **Kontrol Sıklığı**: Her dakika
- **Hata Bildirim Aralığı**: 30 dakika
- **HTTP Timeout**: 60 saniye
- **Log Seviyesi**: INFO
- **Log Rotasyonu**: Günlük
- **Log Saklama**: 30 gün

## 🔧 Sorun Giderme

### Yaygın Sorunlar

#### 1. Pushover Bildirimi Gelmiyor

```bash
# Environment variables kontrol et
echo $PUSHOVER_USER_KEY
echo $PUSHOVER_APP_TOKEN
```

#### 2. Tesla API Bağlantı Hatası

- Ağ bağlantınızı kontrol edin
- Firewall ayarlarını kontrol edin
- VPN kullanıyorsanız kapatıp deneyin

#### 3. Bot Başlamıyor

```bash
# Java sürümünü kontrol et
java -version

# Maven sürümünü kontrol et
mvn -version
```

### Debug Modu

Daha detaylı loglar için logback.xml dosyasını düzenleyin:

```xml
<root level="DEBUG">
    <appender-ref ref="CONSOLE" />
    <appender-ref ref="FILE" />
</root>
```

## 📁 Proje Yapısı

```
tesla-bot/
├── src/main/java/com/teslabot/
│   ├── TeslaInventoryBot.java    # Ana bot sınıfı
│   └── PushoverNotifier.java     # Bildirim servisi
├── src/main/resources/
│   └── logback.xml              # Log konfigürasyonu
├── logs/                        # Log dosyaları
├── pom.xml                      # Maven konfigürasyonu
├── start.sh                     # Başlatma scripti
├── Dockerfile                   # Docker image
├── docker-compose.yml           # Docker Compose
└── README.md                    # Bu dosya
```

## 🛡️ Güvenlik

- Pushover API anahtarları environment variable olarak saklanır
- HTTP istekleri için uygun User-Agent header'ı kullanılır
- Tüm hatalar loglanır ve bildirim gönderilir
- Bağlantı havuzu ile optimize edilmiş HTTP client

## 🤝 Katkıda Bulunma

1. Fork yapın
2. Feature branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Commit yapın (`git commit -m 'Add amazing feature'`)
4. Push yapın (`git push origin feature/amazing-feature`)
5. Pull Request oluşturun

## 📄 Lisans

Bu proje MIT lisansı altında lisanslanmıştır.

## 🙏 Teşekkürler

- [Tesla](https://www.tesla.com/) - Envanter API'si
- [Pushover](https://pushover.net/) - Bildirim servisi
- [OkHttp](https://square.github.io/okhttp/) - HTTP client
- [Jackson](https://github.com/FasterXML/jackson) - JSON işleme

---

**Not**: Bu bot sadece eğitim amaçlıdır. Tesla'nın kullanım şartlarına uygun kullanın.
