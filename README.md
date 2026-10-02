# Chatyar — چتیار 

<p align="center">
  <strong>Privacy-first, BYOK Android client for multiple AI providers</strong>
  <br>
  <strong>کلاینت متن‌باز اندروید برای گفتگو با سرویس‌های مختلف هوش مصنوعی با API Key شخصی</strong>
</p>

---

## 🌐 Languages

- [🇮🇷 فارسی](#-فارسی)
- [🇬🇧 English](#-english)

---

# 🇮🇷 فارسی

## چتیار چیست؟

**چتیار** یک اپلیکیشن متن‌باز اندرویدی برای گفتگو با مدل‌ها و سرویس‌های مختلف هوش مصنوعی است.

چتیار بر اساس مدل **BYOK — Bring Your Own Key** ساخته شده است.

یعنی:

1. سرویس‌دهنده موردنظر خود را انتخاب می‌کنید.
2. API Key خودتان را وارد می‌کنید.
3. مدل موردنظر را انتخاب می‌کنید.
4. مستقیماً از گوشی خود با API سرویس‌دهنده ارتباط برقرار می‌کنید.

چتیار برای استفاده عادی از Providerهای ابری نیازی به Backend یا Proxy اختصاصی ندارد.

```text
Android Device
      │
      │ HTTPS
      ▼
AI Provider API
```

نمونه:

```text
Chatyar
   ├── OpenAI
   ├── Claude
   ├── Gemini
   ├── OpenRouter
   ├── Groq
   ├── Mistral
   ├── DeepSeek
   ├── xAI
   ├── Ollama
   └── Custom OpenAI-compatible API
```

---

## ✨ امکانات

### 🤖 پشتیبانی از چند Provider

چتیار از Providerهای مختلف هوش مصنوعی پشتیبانی می‌کند:

- OpenAI
- Anthropic / Claude
- Google Gemini
- OpenRouter
- Groq
- Mistral
- Together
- DeepSeek
- xAI
- Ollama
- LM Studio
- APIهای سفارشی سازگار با OpenAI

---

### OpenAI Responses API

برای OpenAI یک Gateway مستقل برای **Responses API** وجود دارد.

این یعنی OpenAI اصلی مجبور نیست از لایه Chat Completions سازگار با Providerهای دیگر استفاده کند.

Endpoint پیش‌فرض:

```text
https://api.openai.com/v1/responses
```

---

### OpenAI-compatible APIs

برای سرویس‌هایی که API مشابه OpenAI دارند می‌توانید از Protocol مربوط به OpenAI Compatible استفاده کنید.

نمونه Base URL:

```text
https://api.example.com/v1
```

Endpoint پیش‌فرض Chat:

```text
/chat/completions
```

Model discovery:

```text
GET /models
```

---

## ⚙️ تنظیم کامل Provider

هر Provider می‌تواند تنظیمات مستقل داشته باشد:

- Name
- Protocol
- Base URL
- API Key
- Model
- Endpoint Path
- Auth Header
- Auth Prefix
- Extra Headers
- Temperature
- Max Tokens
- Timeout
- Streaming

این قابلیت اجازه می‌دهد علاوه بر سرویس‌های از پیش تعریف‌شده، بسیاری از APIهای سفارشی نیز به چتیار متصل شوند.

---

## 🔐 امنیت API Key

API Keyها به‌صورت plaintext در Room Database ذخیره نمی‌شوند.

چتیار برای محافظت از کلیدها از:

```text
Android Keystore
       +
AES/GCM/NoPadding
```

استفاده می‌کند.

داده رمزنگاری‌شده در حافظه خصوصی برنامه نگهداری می‌شود و کلید اصلی رمزنگاری توسط Android Keystore مدیریت می‌شود.

### نکته امنیتی

هیچ برنامه Client-side نمی‌تواند روی دستگاه Root شده یا در برابر Runtime Instrumentation امنیت مطلق تضمین کند.

با این حال، استفاده از Android Keystore نسبت به ذخیره مستقیم API Key در SharedPreferences یا Database امنیت بسیار بهتری فراهم می‌کند.

---

## 💬 Chat

چتیار از گفتگوهای چندمرحله‌ای پشتیبانی می‌کند.

امکانات:

- Streaming response
- Stop generation
- Local history
- Multiple conversations
- System Prompt اختصاصی
- انتخاب Provider برای هر Chat
- عنوان‌گذاری خودکار گفتگو
- Copy پاسخ
- نمایش خطاهای API
- ذخیره پیام‌ها با Room

---

## 🧠 مدیریت Context

ارسال تمام تاریخچه یک گفتگوی بسیار طولانی در هر Request می‌تواند باعث:

- مصرف بالای Token
- افزایش هزینه
- افزایش Latency
- پر شدن Context Window
- رد شدن Request

شود.

چتیار برای کاهش این مشکل، بخش اخیر و مفید گفتگو را برای Provider ارسال می‌کند.

در نسخه‌های آینده امکان استفاده از Token Counting دقیق و Summarization نیز قابل اضافه شدن است.

---

## 🧪 Test Connection

قبل از ذخیره Provider می‌توانید اتصال آن را تست کنید.

چتیار ابتدا تلاش می‌کند Model Discovery انجام دهد.

برای APIهای OpenAI-compatible معمولاً:

```text
GET /models
```

استفاده می‌شود.

اگر Provider از Model Discovery پشتیبانی نکند ولی Chat API در دسترس باشد، چتیار می‌تواند با یک درخواست کوچک Chat اتصال را بررسی کند.

---

## ⚠️ مدیریت خطا

چتیار خطاهای رایج را به پیام‌های قابل فهم‌تر تبدیل می‌کند:

| HTTP | معنی |
|---|---|
| `400` | Request نامعتبر |
| `401` | API Key نامعتبر یا وجود ندارد |
| `403` | دسترسی مجاز نیست |
| `404` | Endpoint یا Model پیدا نشد |
| `408` | Timeout |
| `429` | Rate Limit |
| `5xx` | خطای Provider |
| Network | مشکل اتصال اینترنت |

---

## 🎨 رابط کاربری

چتیار با:

- Jetpack Compose
- Material 3

ساخته شده است.

امکانات رابط کاربری:

- Light Theme
- Dark Theme
- System Theme
- فارسی
- انگلیسی
- RTL
- LTR
- Onboarding
- Navigation مبتنی بر Compose

---

## 💾 ذخیره اطلاعات

### Room

برای نگهداری:

- Providers metadata
- Chats
- Messages

### DataStore

برای نگهداری تنظیمات برنامه مانند:

- زبان
- Theme
- وضعیت Onboarding

### Android Keystore

برای محافظت از:

- API Keys

---

## 🔌 معماری شبکه

ارتباط HTTP توسط:

```text
OkHttp
```

و پردازش JSON توسط:

```text
kotlinx.serialization
```

انجام می‌شود.

پاسخ‌های Streaming نیز به‌صورت SSE پردازش می‌شوند.

ساختار Gateway:

```text
AiGateway
│
├── OpenAiResponsesGateway
├── OpenAiCompatibleGateway
├── AnthropicGateway
└── GeminiGateway
```

این معماری باعث می‌شود اضافه کردن Protocol جدید نیازمند بازنویسی UI یا سیستم History نباشد.

---

## 🌍 تنظیم Providerها

### OpenAI

Preset:

```text
Protocol:
OpenAI Responses

Base URL:
https://api.openai.com/v1

Endpoint:
/responses
```

API Key باید از پلتفرم API خود OpenAI دریافت شود.

> داشتن اشتراک ChatGPT لزوماً به معنی داشتن اعتبار یا API Key برای OpenAI API نیست.

---

### Anthropic / Claude

```text
Protocol:
Anthropic

Base URL:
https://api.anthropic.com
```

چتیار از Messages API استفاده می‌کند.

---

### Google Gemini

```text
Protocol:
Gemini

Base URL:
https://generativelanguage.googleapis.com
```

چتیار از:

```text
generateContent
streamGenerateContent
```

استفاده می‌کند.

---

### OpenRouter

```text
Protocol:
OpenAI Compatible

Base URL:
https://openrouter.ai/api/v1
```

سپس Model ID موردنظر خود را وارد کنید.

---

### Groq

```text
Protocol:
OpenAI Compatible

Base URL:
https://api.groq.com/openai/v1
```

---

### Mistral

```text
Protocol:
OpenAI Compatible

Base URL:
https://api.mistral.ai/v1
```

---

### Together

```text
Protocol:
OpenAI Compatible

Base URL:
https://api.together.xyz/v1
```

---

### DeepSeek

```text
Protocol:
OpenAI Compatible

Base URL:
https://api.deepseek.com/v1
```

---

### xAI

```text
Protocol:
OpenAI Compatible

Base URL:
https://api.x.ai/v1
```

---

## 🏠 Ollama

### Android Emulator

Android Emulator برای دسترسی به `localhost` کامپیوتر میزبان از IP زیر استفاده می‌کند:

```text
10.0.2.2
```

بنابراین:

```text
http://10.0.2.2:11434/v1
```

### گوشی واقعی

اگر IP کامپیوتر در LAN مثلاً:

```text
192.168.1.10
```

باشد:

```text
http://192.168.1.10:11434/v1
```

استفاده کنید.

گوشی و کامپیوتر باید بتوانند در شبکه محلی یکدیگر را ببینند.

---

## 🖥️ LM Studio

در Emulator:

```text
http://10.0.2.2:1234/v1
```

در گوشی واقعی، `10.0.2.2` را با IP سیستم میزبان جایگزین کنید.

---

## ⚠️ HTTP Local Endpoints

برای پشتیبانی از Ollama و LM Studio، برنامه امکان اتصال به endpointهای HTTP محلی را دارد.

اما:

> API Key محرمانه را هرگز روی یک HTTP endpoint غیرقابل اعتماد ارسال نکنید.

برای Providerهای Cloud همیشه HTTPS توصیه می‌شود.

---

## 🚀 روش استفاده

### 1. نصب

آخرین APK را از بخش:

```text
Releases
```

دانلود کنید.

حداقل نسخه:

```text
Android 8.0
API 26+
```

---

### 2. اجرای اولیه

در اولین اجرا صفحه معرفی نمایش داده می‌شود.

پس از مطالعه توضیحات، وارد برنامه شوید.

---

### 3. اضافه کردن Provider

به:

```text
Providers
```

بروید.

روی:

```text
+
```

بزنید.

یکی از Presetها را انتخاب کنید یا تنظیمات را دستی وارد کنید.

---

### 4. وارد کردن API Key

برای Providerهای Cloud، API Key سرویس‌دهنده را وارد کنید.

مثلاً:

```text
OpenAI
Anthropic
Gemini
OpenRouter
```

برای Ollama و برخی Providerهای Local ممکن است API Key لازم نباشد.

---

### 5. انتخاب Model

Model ID را وارد کنید یا در صورت پشتیبانی Provider، از Model Discovery استفاده کنید.

---

### 6. Test Connection

روی:

```text
Test Connection
```

بزنید.

اگر اتصال موفق باشد Provider را Save کنید.

---

### 7. ساخت Chat

به بخش:

```text
Chats
```

بروید و Chat جدید ایجاد کنید.

Provider موردنظر را انتخاب کنید.

---

### 8. System Prompt

در صورت نیاز می‌توانید برای هر Chat یک System Prompt تعریف کنید.

مثال:

```text
You are a concise programming assistant.
```

---

### 9. گفتگو

پیام را ارسال کنید.

در صورت فعال بودن Streaming، پاسخ به‌تدریج نمایش داده خواهد شد.

در حین تولید پاسخ می‌توانید Generation را متوقف کنید.

---

# 🛠️ توسعه پروژه

## نیازمندی‌ها

- Android Studio
- JDK 17
- Android SDK 35
- Android 8.0+ target device/emulator

---

## دریافت سورس

```bash
git clone https://github.com/SeyedTahaKhademi/Chatyar.git
cd Chatyar
```

پروژه را با Android Studio باز کنید.

---

## Gradle

پروژه از Gradle Wrapper استفاده می‌کند.

روی Linux/macOS:

```bash
chmod +x gradlew
./gradlew assembleDebug
```

روی Windows:

```text
gradlew.bat assembleDebug
```

APK خروجی:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🪟 Build روی Windows

فایل:

```text
BUILD_WINDOWS.bat
```

برای Build ساده روی Windows در پروژه قرار دارد.

خروجی مورد انتظار:

```text
app\build\outputs\apk\debug\app-debug.apk
```

---

## 🤖 GitHub Actions

پروژه دارای GitHub Actions برای Build اندروید و انتشار APK است.

Workflow:

```text
.github/workflows/android-release.yml
```

Release می‌تواند با Tagهایی مانند:

```text
v0.1.3
```

ساخته شود.

APK تولیدشده در قسمت GitHub Releases قرار می‌گیرد.

---

## 🏗️ Architecture

```text
app
│
├── data
│   ├── Room Database
│   ├── DAOs
│   ├── Repositories
│   ├── SettingsRepository
│   └── SecretStore
│
├── network
│   ├── AiGateway
│   ├── OpenAI Responses
│   ├── OpenAI Compatible
│   ├── Anthropic
│   └── Gemini
│
└── ui
    ├── Onboarding
    ├── Providers
    ├── Provider Editor
    ├── Chats
    ├── Chat
    ├── Settings
    ├── MainViewModel
    └── Theme
```

---

## 📦 Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- DataStore
- Android Keystore
- OkHttp
- SSE Streaming
- kotlinx.serialization
- Kotlin Coroutines
- Navigation Compose

---

## 📱 Package name

```text
ir.hooshamoozan.chatyar
```

---

## 🗺️ قابلیت‌های آینده

معماری پروژه امکان توسعه قابلیت‌هایی مانند موارد زیر را فراهم می‌کند:

- Image input
- File attachments
- Multimodal conversations
- Tool calling
- Web search
- Better token counting
- Conversation summarization
- Export / Import chats
- Backup
- More AI providers
- Improved Markdown rendering
- Code highlighting
- Usage statistics

این موارد Roadmap هستند و وجودشان در نسخه فعلی تضمین نشده است.

---

## 🔒 نکات امنیتی

- API Key خود را در GitHub Issue ارسال نکنید.
- API Key را در Screenshot عمومی قرار ندهید.
- Log حاوی Credential را منتشر نکنید.
- برای APIهای Cloud از HTTPS استفاده کنید.
- دستگاه Root شده می‌تواند سطح امنیت Client-side را کاهش دهد.

---

## 🤝 مشارکت

Contribution، Pull Request، گزارش Bug و پیشنهاد قابلیت جدید استقبال می‌شود.

هنگام گزارش مشکل Provider لطفاً این اطلاعات را ارائه کنید:

```text
Provider:
Protocol:
Base URL:
Model:
HTTP Status:
Error message:
Android version:
```

**API Key را ارسال نکنید.**

---

# 🇬🇧 English

## What is Chatyar?

**Chatyar** is an open-source, privacy-first Android client for communicating with multiple AI providers using your own API credentials.

It follows the **BYOK — Bring Your Own Key** model:

1. Choose an AI provider.
2. Enter your own API key.
3. Select a model.
4. Communicate directly with the provider from your Android device.

Chatyar does not require its own proxy/backend for normal cloud-provider communication.

```text
Android Device
      │
      │ HTTPS
      ▼
AI Provider API
```

---

## ✨ Features

- Multiple AI providers
- OpenAI Responses API
- OpenAI-compatible Chat Completions
- Anthropic / Claude support
- Google Gemini support
- OpenRouter
- Groq
- Mistral
- Together
- DeepSeek
- xAI
- Ollama
- LM Studio
- Custom OpenAI-compatible endpoints
- Streaming responses
- Stop generation
- Local chat history
- Per-chat system prompts
- Provider-specific settings
- Secure API key storage
- Dark / Light / System themes
- Persian / English UI
- RTL / LTR support
- Friendly API error handling
- Provider connection testing
- Model discovery

---

## 🔐 API Key Security

API keys are not stored as plaintext inside the Room database.

Chatyar uses:

```text
Android Keystore
+
AES/GCM/NoPadding
```

for local key protection.

Encrypted data is stored in the app's private storage while the encryption key is managed through Android Keystore.

No client-side application can guarantee absolute protection on rooted or instrumented devices, but this approach provides substantially better protection than storing credentials directly in a database or plain preferences.

---

## 🤖 Supported protocols

```text
AiGateway
│
├── OpenAiResponsesGateway
├── OpenAiCompatibleGateway
├── AnthropicGateway
└── GeminiGateway
```

The gateway-based architecture keeps provider-specific behavior separate from the rest of the application.

---

## ⚙️ Provider configuration

Providers can define:

- Name
- Protocol
- Base URL
- API Key
- Model
- Endpoint Path
- Authorization Header
- Authorization Prefix
- Extra Headers
- Temperature
- Max Tokens
- Timeout
- Streaming

---

## 💬 Conversations

Chatyar supports persistent multi-turn conversations.

Features include:

- Local Room history
- Streaming output
- Stop generation
- Partial-response preservation
- Automatic conversation titles
- Per-chat system prompts
- Provider selection per conversation

---

## 🧠 Context management

Unlimited conversation history is not always practical.

Sending an entire long conversation on every request may increase:

- token usage
- API cost
- latency

and may eventually exceed a model's context window.

Chatyar limits the amount of recent conversation context included in a request.

More advanced token counting and summarization may be added later.

---

## 🧪 Provider testing

Chatyar can test provider connectivity before saving the configuration.

It first attempts model discovery.

For many OpenAI-compatible APIs this means:

```text
GET /models
```

If model discovery is unavailable but a model has already been configured, Chatyar may perform a small real chat request as a fallback connectivity test.

---

## ⚠️ Error handling

Common errors are mapped to clearer user-facing messages:

| Code | Meaning |
|---|---|
| `400` | Invalid request |
| `401` | Missing or invalid API key |
| `403` | Forbidden |
| `404` | Model or endpoint not found |
| `408` | Timeout |
| `429` | Rate limited |
| `5xx` | Provider/server failure |
| Network | Connectivity problem |

---

## 🚀 Getting started

### 1. Install

Download the latest APK from:

```text
GitHub → Releases
```

Minimum Android version:

```text
Android 8.0 / API 26+
```

### 2. Open Chatyar

Complete the initial onboarding screen.

### 3. Add a provider

Open:

```text
Providers
```

and tap the add button.

### 4. Select a preset

Choose OpenAI, Claude, Gemini, OpenRouter or another supported provider.

You may also create a custom OpenAI-compatible configuration.

### 5. Enter credentials

Enter your API key when required.

Local providers such as Ollama may not require authentication.

### 6. Choose a model

Enter a model ID or use model discovery when supported.

### 7. Test

Run:

```text
Test Connection
```

### 8. Save

Save the provider configuration.

### 9. Create a conversation

Open:

```text
Chats
```

and create a new chat.

Select a provider and optionally enter a system prompt.

### 10. Chat

Send a message.

If streaming is enabled, output appears incrementally.

Generation can be stopped at any time.

---

## OpenAI

Default Base URL:

```text
https://api.openai.com/v1
```

Chatyar's OpenAI preset uses the Responses API.

Default endpoint:

```text
/responses
```

---

## Anthropic / Claude

```text
https://api.anthropic.com
```

Chatyar uses Anthropic's Messages-style API integration.

---

## Google Gemini

```text
https://generativelanguage.googleapis.com
```

Chatyar supports Gemini content generation and streaming.

---

## OpenRouter

```text
https://openrouter.ai/api/v1
```

Use:

```text
OpenAI Compatible
```

as the protocol.

---

## Ollama

Android Emulator:

```text
http://10.0.2.2:11434/v1
```

Physical devices must use the LAN IP of the machine running Ollama.

Example:

```text
http://192.168.1.10:11434/v1
```

---

## LM Studio

Android Emulator:

```text
http://10.0.2.2:1234/v1
```

Use your host machine's LAN IP on a physical Android device.

---

## ⚠️ Cleartext HTTP

Local/self-hosted providers may use HTTP.

Never send a secret cloud API key over an untrusted cleartext connection.

Prefer HTTPS for remote services.

---

## 🛠️ Development

Requirements:

```text
Android Studio
JDK 17
Android SDK 35
```

Clone:

```bash
git clone https://github.com/SeyedTahaKhademi/Chatyar.git
cd Chatyar
```

Open the project in Android Studio and sync Gradle.

---

## Build

Linux/macOS:

```bash
chmod +x gradlew
./gradlew assembleDebug
```

Windows:

```text
gradlew.bat assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## GitHub Actions

The repository contains an Android build/release workflow:

```text
.github/workflows/android-release.yml
```

Version tags such as:

```text
v0.1.3
```

can trigger APK builds and GitHub Releases.

---

## 🏗️ Project architecture

```text
app
│
├── data
│   ├── Room
│   ├── DAOs
│   ├── Repositories
│   ├── Settings
│   └── SecretStore
│
├── network
│   ├── AiGateway
│   ├── OpenAI Responses
│   ├── OpenAI Compatible
│   ├── Anthropic
│   └── Gemini
│
└── ui
    ├── Onboarding
    ├── Providers
    ├── Provider Editor
    ├── Chats
    ├── Chat
    ├── Settings
    ├── MainViewModel
    └── Theme
```

---

## 📦 Technology

- Kotlin
- Jetpack Compose
- Material 3
- Room
- DataStore
- Android Keystore
- OkHttp
- SSE
- kotlinx.serialization
- Kotlin Coroutines
- Navigation Compose

---

## Package

```text
ir.hooshamoozan.chatyar
```

---

## 🗺️ Possible future work

The current architecture can be extended with features such as:

- image input
- file attachments
- multimodal conversations
- tool calling
- web search
- accurate token accounting
- conversation summarization
- import/export
- backups
- more providers
- improved Markdown rendering
- syntax highlighting
- usage statistics

These items are possible roadmap directions and are not guaranteed to be included in the current release.

---

## 🔒 Security notes

Never include API keys in:

- GitHub Issues
- screenshots
- public logs
- commits
- pull requests

Use HTTPS for cloud APIs whenever possible.

---

## 🤝 Contributing

Bug reports, pull requests and feature suggestions are welcome.

When reporting provider compatibility problems, include:

```text
Provider:
Protocol:
Base URL:
Model:
HTTP status:
Error message:
Android version:
```

Do **not** include your API key.

---

## 📄 License

Check the repository's license file for the terms that apply to this project.
