# Chatyar v0.2 — چتیار

نسخه بازطراحی‌شده چتیار؛ کلاینت اندرویدی BYOK برای اتصال مستقیم به سرویس‌های هوش مصنوعی.

## مهم‌ترین تغییرات این بسته

- بازطراحی کامل UI با ظاهر تیره، مینیمال و کارت‌های گرد، با حفظ رنگ آبی برند Chatyar.
- حذف Bottom Navigation قدیمی و ساده‌تر شدن مسیر Chats / Providers / Settings.
- طراحی جدید صفحه Chat با model/provider pill، empty state، suggestion chips و composer جدید.
- طراحی جدید Providers و فرم Add/Edit Provider.
- طراحی جدید Settings با section cardها و امکان واقعی Clear all chats.
- حفظ Vazirmatn برای تایپوگرافی فارسی.
- اصلاح Test Connection برای جلوگیری از انتظار چنددقیقه‌ای/طولانی:
  - timeout مستقل 15 ثانیه‌ای برای تست.
  - تلاش برای Model Discovery.
  - fallback به یک درخواست کوچک واقعی در صورت پشتیبانی نکردن `/models`.
  - parser منعطف‌تر برای پاسخ فهرست مدل‌ها.
  - User-Agent و Accept header استانداردتر.
- Timeout چت واقعی همچنان قابل تنظیم است، ولی در محدوده امن‌تری clamp می‌شود.
- API Key با Android Keystore / AES-GCM نگهداری می‌شود.
- تاریخچه با Room روی دستگاه ذخیره می‌شود.

## Build

نیازمندی‌ها:

- JDK 17
- Android SDK 35
- Android 8.0+ (API 26+)

Linux/macOS:

```bash
chmod +x gradlew
./gradlew clean assembleDebug
```

Windows:

```bat
gradlew.bat clean assembleDebug
```

خروجی:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Provider test behavior

Test Connection دیگر از timeout طولانی تنظیمات چت استفاده نمی‌کند. هر مرحله تست در حدود 15 ثانیه قطع می‌شود تا UI ساعت‌ها در حالت Loading نماند.

برای Providerهای OpenAI-compatible:

1. ابتدا `GET /models` امتحان می‌شود.
2. اگر endpoint مدل‌ها در دسترس نبود و Model وارد شده باشد، یک درخواست Chat کوچک non-streaming ارسال می‌شود.
3. اگر همان درخواست موفق شود، Provider معتبر تلقی می‌شود حتی اگر لیست مدل‌ها برنگردد.

## Supported protocols

- OpenAI Responses API
- OpenAI-compatible Chat Completions
- Anthropic / Claude
- Google Gemini
- OpenRouter / Groq / Mistral / Together / DeepSeek / xAI
- Ollama / LM Studio
- Custom OpenAI-compatible providers

## Package

`ir.hooshamoozan.chatyar`
