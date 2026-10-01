# Chatyar — چتیار

A privacy-first Android BYOK (Bring Your Own Key) chat client written in Kotlin + Jetpack Compose.

## What is included

- First-run Persian/English onboarding.
- Provider management with presets for OpenAI, Anthropic/Claude, Gemini, OpenRouter, Groq, Mistral, Together, DeepSeek, xAI, Ollama and LM Studio.
- Native OpenAI Responses API support for the OpenAI preset.
- Custom OpenAI-compatible Chat Completions endpoints.
- Configurable Base URL, API key, model, endpoint path, auth header/prefix, custom headers, temperature, max output tokens, timeout and streaming.
- Provider connection test + model discovery.
- Streaming chat responses.
- Local chat history using Room.
- API keys encrypted with Android Keystore (AES-GCM); the plaintext key is not stored in Room.
- Dark / light / system theme.
- Persian / English UI and RTL support.
- Vazirmatn (Vazir family successor) loaded through Android downloadable Google Fonts. No font binary is bundled in this project.
- Telegram button for `t.me/hooshamoozan`.
- Minimal Chatyar vector logo using the blue palette sampled from the supplied Hooshamoozan logo (primary: `#2574FC`).
- Friendly mappings for 401 / 403 / 404 / 429 / 5xx / network errors.
- Local HTTP support for Ollama / LM Studio. A visible warning is shown because cleartext HTTP must never be used with a secret API key on an untrusted network.

## Architecture

- Kotlin
- Jetpack Compose + Material 3
- Room for local chat/provider metadata
- DataStore for settings
- Android Keystore for provider API keys
- OkHttp for HTTP + SSE streaming
- kotlinx.serialization for JSON

The cloud providers are called directly from the Android device. Chatyar itself does not require a proxy/backend.

## Open in Android Studio

1. Open this folder in Android Studio.
2. Use JDK 17 or Android Studio's bundled JBR.
3. Install Android SDK 35 if Android Studio asks for it.
4. Sync Gradle.
5. Run the `app` configuration on Android 8.0+ (API 26+).

If `local.properties` is missing, Android Studio creates it automatically. `local.properties.example` is included for manual setup.

## Windows one-click build

Run:

`BUILD_WINDOWS.bat`

The script tries to find Android Studio's bundled Java, detects the default Android SDK path, downloads Gradle 8.9 if needed, creates the Gradle wrapper, then builds a debug APK.

Expected output:

`app\build\outputs\apk\debug\app-debug.apk`

## Important provider notes

### OpenAI
The built-in OpenAI preset uses the native Responses API (`/v1/responses`) so current OpenAI models can be used without forcing them through a third-party compatibility layer. Model discovery still uses `/v1/models`.

### OpenAI-compatible
Base URL should normally end at the API version root, for example:

`https://api.example.com/v1`

The default chat path is:

`/chat/completions`

Model discovery uses:

`GET /models`

### Anthropic
Use protocol `Anthropic / Claude`, Base URL:

`https://api.anthropic.com`

Chatyar uses the Messages API and the required Anthropic headers.

### Gemini
Use protocol `Google Gemini`, Base URL:

`https://generativelanguage.googleapis.com`

Chatyar uses `generateContent` / `streamGenerateContent` and the `x-goog-api-key` header.

### Ollama on Android emulator
The emulator reaches your PC through `10.0.2.2`, so the preset is:

`http://10.0.2.2:11434/v1`

On a physical phone, replace this with the LAN IP of the computer running Ollama.

## Scope of v0.1

This build focuses on the requested provider setup, secure key storage, text chat, streaming, history, errors, theme and localization. The OpenAI Responses adapter intentionally avoids forcing `temperature` into the request so newer reasoning-model parameter differences do not break basic chat. The architecture intentionally separates protocol gateways so image input, file attachments, tool calling and web-search tools can be added without rewriting the provider/history UI.

## Package

`ir.hooshamoozan.chatyar`

## Brand files

- `brand/chatyar_mark.svg` — editable vector mark
- `brand/chatyar_mark.png` — rendered preview
- Android vector icon: `app/src/main/res/drawable/ic_chatyar_logo.xml`
