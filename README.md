# 🚗 Tesla Inventory Monitoring Bot

A real-time Tesla inventory monitoring bot that checks Tesla's official inventory API at regular intervals and sends notifications via Pushover when new vehicles are available or when errors occur.

## ✨ Features

- 🕐 **Real-time monitoring** - Checks Tesla inventory every minute
- 📱 **Pushover notifications** - Instant notifications for new vehicles and errors
- 🔗 **VIN Links** - Direct links to Tesla order pages for each vehicle
- 🌍 **Multi-market support** - Easy configuration for different countries
- 🔄 **Proxy rotation** - Random proxy selection for each request
- 📦 **Grouped notifications** - Sends all inventory items in groups of 5
- ⚠️ **Smart error handling** - First error and 30-minute continuous error notifications
- 🔄 **Automatic retry** - Automatic retry on connection issues
- 📊 **Detailed logging** - All operations are logged and stored
- 🛡️ **Graceful shutdown** - Safe shutdown support
- 🐳 **Docker support** - Run in containerized environment
- 🔧 **Easy setup** - Start with single command

## 🚀 Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6+
- Pushover account
- `proxy-list.txt` file with proxy servers

### 1. Pushover Setup

1. Go to [Pushover](https://pushover.net/) and create an account
2. Create an application and get API token
3. Note your user key

### 2. Proxy List Setup

Create a `proxy-list.txt` file in the project root with proxy servers (one per line):

```
154.213.198.89:3129
154.213.203.129:3129
156.233.85.174:3129
...
```

### 3. Environment Variables

```bash
# Required - Pushover notifications
export PUSHOVER_USER_KEY="your_user_key_here"
export PUSHOVER_APP_TOKEN="your_app_token_here"

# Optional - Tesla market settings (default: DE/de)
export TESLA_MARKET="DE"      # Country code (DE, TR, US, CA, etc.)
export TESLA_LANGUAGE="de"    # Language code (de, tr, en, etc.)
```

### 4. Start the Bot

```bash
# Build the project
mvn clean package

# Run the bot
java -jar target/tesla-inventory-bot-1.0.0.jar
```

**Or use the automatic startup script:**

```bash
chmod +x start.sh
./start.sh
```

## 📋 Usage

### Normal Operation

When the bot starts:

1. First checks Tesla inventory
2. Performs regular checks every minute
3. Sends notifications when new vehicles arrive (with VIN links)
4. Sends notifications on API errors
5. Sends repeat notifications after 30 minutes of continuous errors
6. Uses random proxy for each request
7. Groups all inventory items in batches of 5 for notifications

### Notification Types

#### 🎉 New Vehicle Notification (with VIN Links)

```
🎉 3 new vehicles found in Tesla inventory!
Total: 15 vehicles

🔗 New Vehicle Links (1/3):
1. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123456?titleStatus=new&redirect=no#overview
2. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123457?titleStatus=new&redirect=no#overview
3. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123458?titleStatus=new&redirect=no#overview
4. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123459?titleStatus=new&redirect=no#overview
5. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123460?titleStatus=new&redirect=no#overview

🔗 New Vehicle Links (2/3):
6. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123461?titleStatus=new&redirect=no#overview
7. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123462?titleStatus=new&redirect=no#overview
8. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123463?titleStatus=new&redirect=no#overview
9. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123464?titleStatus=new&redirect=no#overview
10. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123465?titleStatus=new&redirect=no#overview

🔗 New Vehicle Links (3/3):
11. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123466?titleStatus=new&redirect=no#overview
12. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123467?titleStatus=new&redirect=no#overview
13. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123468?titleStatus=new&redirect=no#overview
```

#### 📊 Initial Inventory Status

```
📊 272 vehicles found in Tesla inventory

🔗 Vehicle Links (1/55):
1. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123456?titleStatus=new&redirect=no#overview
2. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123457?titleStatus=new&redirect=no#overview
3. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123458?titleStatus=new&redirect=no#overview
4. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123459?titleStatus=new&redirect=no#overview
5. https://www.tesla.com/de_DE/my/order/5YJSA1E47JF123460?titleStatus=new&redirect=no#overview
```

#### ❌ First Error Notification

```
❌ Tesla API access error: timeout
```

#### ⚠️ Continuous Error Notification

```
⚠️ Tesla API error continues for 30 minutes: timeout
```

### Logs

Bot logs are stored in `logs/tesla-bot.log`:

```bash
# Live log tracking
tail -f logs/tesla-bot.log

# Last 50 lines
tail -50 logs/tesla-bot.log
```

## 🌍 Market Configuration

### Supported Markets

| Country | Market | Language | Super Region  |
| ------- | ------ | -------- | ------------- |
| Germany | `DE`   | `de`     | europe        |
| Turkey  | `TR`   | `tr`     | europe        |
| USA     | `US`   | `en`     | north america |
| Canada  | `CA`   | `en`     | north america |
| UK      | `GB`   | `en`     | europe        |
| France  | `FR`   | `fr`     | europe        |
| Italy   | `IT`   | `it`     | europe        |
| Spain   | `ES`   | `es`     | europe        |

### Market Change Examples

```bash
# For Turkey
export TESLA_MARKET="TR" && export TESLA_LANGUAGE="tr"

# For USA
export TESLA_MARKET="US" && export TESLA_LANGUAGE="en"

# For UK
export TESLA_MARKET="GB" && export TESLA_LANGUAGE="en"
```

## 🐳 Docker Usage

### Docker Hub Image

```bash
# Pull and run from Docker Hub
docker run -d \
  --name tesla-bot \
  -e PUSHOVER_USER_KEY="your_user_key" \
  -e PUSHOVER_APP_TOKEN="your_app_token" \
  -e TESLA_MARKET="TR" \
  -e TESLA_LANGUAGE="tr" \
  aydinozturk/tesla-inventory-bot:latest
```

### Docker Compose (Recommended)

```yaml
version: "3.8"
services:
  tesla-bot:
    image: aydinozturk/tesla-inventory-bot:latest
    container_name: tesla-inventory-bot
    environment:
      - PUSHOVER_USER_KEY=your_user_key
      - PUSHOVER_APP_TOKEN=your_app_token
      - TESLA_MARKET=TR
      - TESLA_LANGUAGE=tr
    volumes:
      - ./logs:/app/logs
    restart: unless-stopped
```

```bash
# Start with docker-compose
docker-compose up -d

# Follow logs
docker-compose logs -f tesla-bot
```

### Manual Docker Build

```bash
# Build image
docker build -t tesla-inventory-bot .

# Run container
docker run -d \
  --name tesla-bot \
  -e PUSHOVER_USER_KEY="your_user_key" \
  -e PUSHOVER_APP_TOKEN="your_app_token" \
  -e TESLA_MARKET="DE" \
  -e TESLA_LANGUAGE="de" \
  -v $(pwd)/logs:/app/logs \
  tesla-inventory-bot
```

## ⚙️ Configuration

### Environment Variables

| Variable             | Description         | Required | Default |
| -------------------- | ------------------- | -------- | ------- |
| `PUSHOVER_USER_KEY`  | Pushover user key   | ✅       | -       |
| `PUSHOVER_APP_TOKEN` | Pushover app token  | ✅       | -       |
| `TESLA_MARKET`       | Tesla market code   | ❌       | `DE`    |
| `TESLA_LANGUAGE`     | Tesla language code | ❌       | `de`    |

### Proxy Configuration

The bot automatically uses proxies from `proxy-list.txt` file:

- Each request uses a random proxy from the list
- If no proxies are available, requests are made directly
- Proxy format: `IP:PORT` (one per line)

Example `proxy-list.txt`:

```
154.213.198.89:3129
154.213.203.129:3129
156.233.85.174:3129
```

## 🔧 Technical Details

### API Endpoints

- **Tesla Inventory API**: `https://www.tesla.com/coinorder/api/v4/inventory-results`
- **Pushover API**: `https://api.pushover.net/1/messages.json`

### Request Headers

```http
User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36
Accept: application/json
```

### Response Format

Tesla API returns JSON with vehicle inventory data including VIN numbers, which are used to generate direct order links.

## 🚀 Development

### Building from Source

```bash
# Clone repository
git clone https://github.com/aydinozturk/tesla-bot.git
cd tesla-bot

# Build with Maven
mvn clean package

# Run tests
mvn test
```

### Project Structure

```
tesla-bot/
├── src/main/java/com/teslabot/
│   ├── TeslaInventoryBot.java    # Main bot logic
│   └── PushoverNotifier.java     # Pushover notification service
├── src/main/resources/
│   └── logback.xml               # Logging configuration
├── proxy-list.txt                # Proxy server list
├── docker-compose.yml            # Docker Compose configuration
├── Dockerfile                    # Docker image definition
├── pom.xml                       # Maven dependencies
└── start.sh                      # Startup script
```

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📞 Support

If you encounter any issues or have questions:

1. Check the logs in `logs/tesla-bot.log`
2. Verify your environment variables are set correctly
3. Ensure your `proxy-list.txt` file is properly formatted
4. Open an issue on GitHub with detailed error information

## 🔄 Changelog

### v1.0.0

- Initial release with basic inventory monitoring
- Pushover notifications
- Multi-market support
- Docker support

### v1.1.0

- Added proxy rotation support
- Dynamic URL generation for car links
- Grouped notifications (5 items per message)
- Notifications for both inventory increases and decreases
- Improved error handling and logging
