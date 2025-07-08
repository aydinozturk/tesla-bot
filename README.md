# 🚗 Tesla Envanter Kontrol Botu

Bu bot, Tesla'nın resmi envanter API'sini düzenli olarak kontrol eder ve yeni araç geldiğinde veya hata durumunda Pushover üzerinden bildirim gönderir.

## ✨ Özellikler

- 🕐 **Her dakika otomatik kontrol** - Tesla envanterini sürekli izler
- 📱 **Pushover bildirimleri** - Yeni araç ve hata durumlarında anında bildirim
- 🔗 **VIN Linkleri** - Her araç için Tesla sipariş sayfası linki
- 🌍 **Çoklu Market Desteği** - Farklı ülkeler için kolay konfigürasyon
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
# Zorunlu - Pushover bildirimleri için
export PUSHOVER_USER_KEY="your_user_key_here"
export PUSHOVER_APP_TOKEN="your_app_token_here"

# Opsiyonel - Tesla market ayarları (varsayılan: DE/de)
export TESLA_MARKET="DE"      # Ülke kodu (DE, TR, US, CA, vb.)
export TESLA_LANGUAGE="de"    # Dil kodu (de, tr, en, vb.)
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
3. Yeni araç geldiğinde bildirim gönderir (VIN linkleri ile)
4. API hatalarında bildirim gönderir
5. 30 dakika sürekli hata durumunda tekrar bildirim gönderir

### Bildirim Türleri

#### 🎉 Yeni Araç Bildirimi (VIN Linkleri ile)

```
🎉 Tesla envanterinde 3 yeni araç bulundu!
Toplam: 15 araç

🔗 Yeni Araç Linkleri:
1. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123456?titleStatus=new&redirect=no#overview
2. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123457?titleStatus=new&redirect=no#overview
3. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123458?titleStatus=new&redirect=no#overview
... ve 2 araç daha
```

#### 📊 İlk Envanter Durumu

```
📊 Tesla envanterinde 272 araç bulundu

🔗 Araç Linkleri:
1. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123456?titleStatus=new&redirect=no#overview
2. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123457?titleStatus=new&redirect=no#overview
3. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123458?titleStatus=new&redirect=no#overview
4. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123459?titleStatus=new&redirect=no#overview
5. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123460?titleStatus=new&redirect=no#overview
... ve 19 araç daha
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

## 🌍 Market Konfigürasyonu

### Desteklenen Marketler

| Ülke      | Market | Language | Super Region  |
| --------- | ------ | -------- | ------------- |
| Almanya   | `DE`   | `de`     | europe        |
| Türkiye   | `TR`   | `tr`     | europe        |
| ABD       | `US`   | `en`     | north america |
| Kanada    | `CA`   | `en`     | north america |
| İngiltere | `GB`   | `en`     | europe        |
| Fransa    | `FR`   | `fr`     | europe        |
| İtalya    | `IT`   | `it`     | europe        |
| İspanya   | `ES`   | `es`     | europe        |

### Market Değiştirme Örnekleri

```bash
# Türkiye için
export TESLA_MARKET="TR" && export TESLA_LANGUAGE="tr"

# ABD için
export TESLA_MARKET="US" && export TESLA_LANGUAGE="en"

# İngiltere için
export TESLA_MARKET="GB" && export TESLA_LANGUAGE="en"
```

## 🐳 Docker ile Çalıştırma

### Docker Compose (Önerilen)

```bash
# Environment variables ayarla
export PUSHOVER_USER_KEY="your_user_key"
export PUSHOVER_APP_TOKEN="your_app_token"
export TESLA_MARKET="DE"
export TESLA_LANGUAGE="de"

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
  -e TESLA_MARKET="DE" \
  -e TESLA_LANGUAGE="de" \
  -v $(pwd)/logs:/app/logs \
  tesla-inventory-bot
```

## ⚙️ Konfigürasyon

### Environment Variables

| Değişken             | Açıklama           | Zorunlu | Varsayılan |
| -------------------- | ------------------ | ------- | ---------- |
| `PUSHOVER_USER_KEY`  | Pushover user key  | ✅      | -          |
| `PUSHOVER_APP_TOKEN` | Pushover app token | ✅      | -          |
| `TESLA_MARKET`       | Tesla market kodu  | ❌      | `DE`       |
| `TESLA_LANGUAGE`     | Tesla dil kodu     | ❌      | `de`       |

### Teknik Detaylar

- **Kontrol Sıklığı**: Her dakika
- **Hata Bildirim Aralığı**: 30 dakika
- **HTTP Timeout**: 60 saniye
- **Log Seviyesi**: INFO
- **Log Rotasyonu**: Günlük
- **Log Saklama**: 30 gün
- **Maksimum Araç Gösterimi**: 5 araç (VIN linkleri ile)
- **URL Encoding**: Otomatik

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
- Market ve language ayarlarını kontrol edin

#### 3. Bot Başlamıyor

```bash
# Java sürümünü kontrol et
java -version

# Maven sürümünü kontrol et
mvn -version

# Environment variables kontrol et
env | grep -E "(PUSHOVER|TESLA)"
```

#### 4. VIN Linkleri Çalışmıyor

- Market ayarının doğru olduğundan emin olun
- Tesla'nın o market için envanter sunduğunu kontrol edin
- API yanıtında VIN alanının bulunduğunu kontrol edin

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

## 🔄 Güncellemeler

### v1.1.0

- ✅ VIN linkleri eklendi
- ✅ Çoklu market desteği (TESLA_MARKET, TESLA_LANGUAGE)
- ✅ Otomatik super region belirleme
- ✅ URL encoding iyileştirmesi
- ✅ Daha detaylı bildirimler

### v1.0.0

- ✅ Temel Tesla envanter kontrolü
- ✅ Pushover bildirimleri
- ✅ Hata yönetimi
- ✅ Docker desteği
