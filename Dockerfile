# Build stage
FROM --platform=linux/amd64 maven:3.9.6-eclipse-temurin-11 AS builder

WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM --platform=linux/amd64 eclipse-temurin:11-jre

LABEL maintainer="aydinozturk"
LABEL description="Tesla Inventory Monitoring Bot"
LABEL version="1.0.0"
LABEL org.opencontainers.image.source="https://github.com/aydinozturk/tesla-bot"

WORKDIR /app

# Timezone ayarla
RUN ln -sf /usr/share/zoneinfo/Europe/Istanbul /etc/localtime && \
    echo "Europe/Istanbul" > /etc/timezone

RUN mkdir -p logs && chmod 755 logs

COPY --from=builder /build/target/tesla-inventory-bot-1.0.0.jar ./tesla-bot.jar
COPY proxy-list.txt proxy-list.txt

ENV TELEGRAM_BOT_TOKEN=""
ENV TELEGRAM_CHAT_ID=""
ENV TELEGRAM_NEW_CARS_CHAT_ID=""
ENV TESLA_MARKET="DE"
ENV TESLA_LANGUAGE="de"
ENV JAVA_OPTS="-Xmx512m -Xms256m"

USER root

CMD ["java", "-jar", "tesla-bot.jar"] 