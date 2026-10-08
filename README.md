# Chatyar v0.3.0-beta — چتیار

نسخه آزمایشی چتیار با اصلاحات کیبورد چت، Image Studio واقعی برای APIهای OpenAI-compatible و اتصال به VPN فعال اندروید از طریق کلاینت مستقل.

> **وضعیت:** Beta / source package. APK کامپایل و روی دستگاه واقعی تأیید نشده است. جزئیات و محدودیت‌ها: [UPDATE_NOTES_FA.md](UPDATE_NOTES_FA.md).

## قابلیت‌های افزوده در 0.3.0-beta

- رفع مشکل پنهان شدن Composer پشت کیبورد با `adjustResize`، IME insets و bring-into-view.
- Image Studio: اتصال مستقیم به `/images/generations`، دریافت Base64 یا URL، تصاویر قبلی و خروجی PNG با file picker.
- Preset «OpenAI Images (gpt-image-1)» برای Provider تصویری مستقل.
- Settings → VPN & Network: ذخیره امن `vless://` و `ss://`، انتقال به کلاینت VPN جداگانه، تشخیص VPN فعال اندروید و تست عملی API.
- **VPN داخلی و اتصال خودکار VLESS/Shadowsocks وجود ندارد.** این مورد نیازمند هسته تونل و Android VpnService است.



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

---

## Experimental internal VPN — v0.3.1-beta

Source includes a `VpnService` + embedded sing-box/libbox integration (the native AAR is downloaded with SHA-256 verification at build time). It requests Android VPN consent and can route **Chatyar app traffic only** through imported endpoints such as VLESS, VMess, Trojan, Shadowsocks, Hysteria2, TUIC, AnyTLS, SOCKS/HTTP, and selected sing-box JSON/Clash YAML.

This is an **experimental source build, not a tested Android release**. See [`INTERNAL_VPN_README_FA.md`](INTERNAL_VPN_README_FA.md), [`VPN_QA_CHECKLIST.md`](VPN_QA_CHECKLIST.md) and GPLv3 notices under `third_party/`. The GPLv3 requirements of libbox must be resolved before redistributing an APK containing it.
