#!/bin/bash

# Tesla Envanter Bot Başlatma Scripti

echo "🚗 Tesla Envanter Bot başlatılıyor..."

# Environment variables kontrolü
if [ -z "$PUSHOVER_USER_KEY" ]; then
    echo "❌ HATA: PUSHOVER_USER_KEY environment variable'ı ayarlanmamış!"
    echo "Lütfen export PUSHOVER_USER_KEY='your_user_key' komutunu çalıştırın"
    exit 1
fi

if [ -z "$PUSHOVER_APP_TOKEN" ]; then
    echo "❌ HATA: PUSHOVER_APP_TOKEN environment variable'ı ayarlanmamış!"
    echo "Lütfen export PUSHOVER_APP_TOKEN='your_app_token' komutunu çalıştırın"
    exit 1
fi

echo "✅ Pushover bilgileri doğrulandı"

# Java kontrolü
if ! command -v java &> /dev/null; then
    echo "❌ HATA: Java bulunamadı! Lütfen Java 11 veya üzerini yükleyin"
    exit 1
fi

# Maven kontrolü
if ! command -v mvn &> /dev/null; then
    echo "❌ HATA: Maven bulunamadı! Lütfen Maven'ı yükleyin"
    exit 1
fi

echo "✅ Java ve Maven doğrulandı"

# Projeyi derle
echo "🔨 Proje derleniyor..."
mvn clean package

if [ $? -ne 0 ]; then
    echo "❌ Derleme hatası! Lütfen hataları kontrol edin"
    exit 1
fi

echo "✅ Proje başarıyla derlendi"

# Logs dizinini oluştur
mkdir -p logs

# Botu çalıştır
echo "🚀 Bot başlatılıyor..."
echo "Botu durdurmak için Ctrl+C kullanın"
echo "Loglar logs/tesla-bot.log dosyasında saklanacak"
echo ""

java -jar target/tesla-inventory-bot-1.0.0.jar 

#API KEY aiaohjuhnk7x594wzi23xtebi3i2qo
#API user umzgdu98a27peb2tpjcvnp6h6znfhm