package ir.hooshamoozan.chatyar.ui

import androidx.compose.runtime.staticCompositionLocalOf
import ir.hooshamoozan.chatyar.data.AppLanguage

data class AppText(
    val appName: String, val tagline: String, val onboardingTitle: String, val onboardingBody: String,
    val onboardingPrivacyTitle: String, val onboardingPrivacyBody: String, val onboardingProviderTitle: String,
    val onboardingProviderBody: String, val onboardingLocalTitle: String, val onboardingLocalBody: String,
    val understood: String, val chats: String, val providers: String, val settings: String,
    val noProvidersTitle: String, val noProvidersBody: String, val addProvider: String, val editProvider: String,
    val providerName: String, val providerType: String, val baseUrl: String, val apiKey: String, val model: String,
    val endpointPath: String, val authHeader: String, val authPrefix: String, val extraHeaders: String, val advanced: String,
    val temperature: String, val maxTokens: String, val timeout: String, val streaming: String, val save: String,
    val cancel: String, val delete: String, val testConnection: String, val testing: String, val connectionOk: String,
    val modelsFound: String, val fillRequired: String, val invalidJson: String, val providerSaved: String,
    val newChat: String, val noChatsTitle: String, val noChatsBody: String, val chooseProvider: String,
    val systemPrompt: String, val optional: String, val create: String, val messageHint: String, val send: String,
    val stop: String, val emptyChat: String, val theme: String, val themeSystem: String, val themeLight: String,
    val themeDark: String, val language: String, val persian: String, val english: String, val telegramChannel: String,
    val telegramSubtitle: String, val privacyTitle: String, val privacyBody: String, val apiKeySecure: String,
    val cleartextWarning: String, val errorUnauthorized: String, val errorForbidden: String, val errorNotFound: String,
    val errorRateLimit: String, val errorServer: String, val errorNetwork: String, val errorGeneric: String,
    val protocolOpenAiResponses: String, val protocolOpenAi: String, val protocolAnthropic: String, val protocolGemini: String,
    val presetProviders: String, val customProvider: String, val edit: String, val modelRequired: String,
    val keyMayBeEmpty: String, val localEndpointNote: String, val noModelList: String,
    val providerInUseDeleteWarning: String, val dismiss: String
)

private val Fa = AppText(
    "چتیار","گفتگوی خصوصی با API خودت","چتیار چطور کار می‌کند؟",
    "کلید API را خودت وارد می‌کنی و درخواست‌ها مستقیم از گوشی به ارائه‌دهنده ارسال می‌شوند.",
    "کلیدها امن می‌مانند","API Key با Android Keystore رمزگذاری می‌شود و تاریخچه گفتگو روی دستگاه می‌ماند.",
    "هر ارائه‌دهنده‌ای که خواستی","OpenAI، Claude، Gemini، OpenRouter، Groq، Mistral، Ollama، LM Studio و APIهای سازگار با OpenAI.",
    "کنترل دست خودت است","مدل، Base URL، هدرها، دما، حداکثر توکن، تایم‌اوت و Streaming قابل تنظیم هستند.",
    "شروع کنیم","گفتگوها","پرووایدرها","تنظیمات","هنوز پرووایدری اضافه نکردی",
    "برای شروع یک سرویس آماده انتخاب کن یا Base URL و API Key دلخواهت را وارد کن.","اضافه کردن پرووایدر","ویرایش پرووایدر",
    "نام","نوع API","Base URL","API Key","مدل","مسیر API","هدر احراز هویت","پیشوند کلید","هدرهای اضافه (JSON)",
    "تنظیمات پیشرفته","Temperature","حداکثر توکن خروجی","Timeout (ثانیه)","Streaming","ذخیره","لغو","حذف",
    "تست اتصال و دریافت مدل‌ها","در حال بررسی…","اتصال موفق بود","مدل دریافت شد","نام، Base URL و مدل را کامل کن.",
    "JSON هدرهای اضافه معتبر نیست.","پرووایدر ذخیره شد.","گفتگوی جدید","هنوز گفتگویی نداری",
    "یک گفتگوی جدید بساز و پرووایدر موردنظرت را انتخاب کن.","انتخاب پرووایدر","System Prompt","اختیاری","ساخت گفتگو",
    "پیامت را بنویس…","ارسال","توقف","یک پیام بفرست تا گفتگو شروع شود.","ظاهر","مطابق سیستم","روشن","تاریک","زبان","فارسی","English",
    "کانال تلگرام","t.me/hooshamoozan","حریم خصوصی",
    "چت‌ها داخل دیتابیس محلی برنامه هستند. چتیار سرور واسط ندارد و محتوای گفتگو را برای خودش ارسال نمی‌کند.",
    "API Key در Android Keystore رمزگذاری می‌شود.",
    "برای Ollama و LM Studio روی شبکه محلی، HTTP فعال است. کلید واقعی را به آدرس HTTP ناشناس نفرست.",
    "API Key رد شد (401). کلید و هدر احراز هویت را بررسی کن.","دسترسی مجاز نیست (403). سطح دسترسی کلید یا حساب را بررسی کن.",
    "آدرس، مسیر API یا نام مدل پیدا نشد (404).","محدودیت درخواست یا اعتبار حساب فعال شده (429). کمی بعد دوباره امتحان کن.",
    "سرور ارائه‌دهنده خطا داد. چند لحظه بعد دوباره امتحان کن.","اتصال شبکه برقرار نشد. اینترنت، Base URL و Timeout را بررسی کن.",
    "درخواست ناموفق بود.","OpenAI Responses","OpenAI-compatible","Anthropic / Claude","Google Gemini","ارائه‌دهندگان آماده","پرووایدر سفارشی",
    "ویرایش","نام مدل لازم است. می‌توانی با دکمه تست، لیست مدل‌ها را بگیری.","برای سرویس‌های لوکال بدون کلید می‌توانی این فیلد را خالی بگذاری.",
    "روی شبیه‌ساز Android، 10.0.2.2 به کامپیوتر میزبان اشاره می‌کند. روی گوشی واقعی IP شبکه کامپیوتر را وارد کن.",
    "اتصال برقرار شد اما لیست مدلی برنگشت.","با حذف پرووایدر، گفتگوهای وابسته به آن هم حذف می‌شوند.","بستن"
)

private val En = AppText(
    "Chatyar","Private chat with your own API","How Chatyar works",
    "Bring your own API key and requests go directly from your phone to the selected provider.",
    "Your keys stay protected","API keys are encrypted with Android Keystore and chat history stays on your device.",
    "Use the provider you want","OpenAI, Claude, Gemini, OpenRouter, Groq, Mistral, Ollama, LM Studio and custom OpenAI-compatible APIs.",
    "You control the stack","Model, base URL, headers, temperature, max tokens, timeout and streaming are configurable.",
    "Get started","Chats","Providers","Settings","No provider yet","Choose a preset or add your own base URL and API key.",
    "Add provider","Edit provider","Name","API protocol","Base URL","API Key","Model","API path","Auth header","Key prefix","Extra headers (JSON)",
    "Advanced settings","Temperature","Max output tokens","Timeout (seconds)","Streaming","Save","Cancel","Delete","Test connection & fetch models",
    "Testing…","Connection successful","models found","Complete name, Base URL and model.","Extra headers must be valid JSON.","Provider saved.",
    "New chat","No chats yet","Create a new chat and select the provider you want to use.","Choose provider","System prompt","Optional","Create chat",
    "Message…","Send","Stop","Send a message to start the conversation.","Appearance","System","Light","Dark","Language","فارسی","English",
    "Telegram channel","t.me/hooshamoozan","Privacy","Chats are stored in the local database. Chatyar has no middleman server.",
    "API keys are encrypted with Android Keystore.","HTTP is enabled for local Ollama and LM Studio endpoints. Never send a real key to an untrusted HTTP endpoint.",
    "The API key was rejected (401).","Access denied (403).","Base URL, API path or model was not found (404).","Rate limit or quota reached (429).",
    "The provider returned a server error.","Network connection failed. Check internet, Base URL and timeout.","The request failed.",
    "OpenAI Responses","OpenAI-compatible","Anthropic / Claude","Google Gemini","Provider presets","Custom provider","Edit",
    "A model name is required. Use the test button to fetch models.","Local services can leave this field empty.",
    "On the Android emulator, 10.0.2.2 points to the host computer. On a real phone, use the host LAN IP.",
    "Connection worked, but no model list was returned.","Deleting this provider also deletes chats that use it.","Dismiss"
)

fun textsFor(language: AppLanguage): AppText = if (language == AppLanguage.FA) Fa else En
val LocalAppText = staticCompositionLocalOf { En }
