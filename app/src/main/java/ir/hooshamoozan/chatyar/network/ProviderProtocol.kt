package ir.hooshamoozan.chatyar.network

enum class ProviderProtocol { OPENAI_RESPONSES, OPENAI_COMPATIBLE, ANTHROPIC, GEMINI }

data class ProviderPreset(
    val name: String,
    val protocol: ProviderProtocol,
    val baseUrl: String,
    val model: String = "",
    val endpointPath: String = "/chat/completions",
    val authHeader: String = "Authorization",
    val authPrefix: String = "Bearer "
)

val ProviderPresets = listOf(
    ProviderPreset("OpenAI", ProviderProtocol.OPENAI_RESPONSES, "https://api.openai.com/v1", "gpt-5.6", endpointPath = "/responses"),
    ProviderPreset("Claude / Anthropic", ProviderProtocol.ANTHROPIC, "https://api.anthropic.com"),
    ProviderPreset("Gemini", ProviderProtocol.GEMINI, "https://generativelanguage.googleapis.com", "gemini-3.5-flash"),
    ProviderPreset("OpenRouter", ProviderProtocol.OPENAI_COMPATIBLE, "https://openrouter.ai/api/v1"),
    ProviderPreset("Groq", ProviderProtocol.OPENAI_COMPATIBLE, "https://api.groq.com/openai/v1"),
    ProviderPreset("Mistral", ProviderProtocol.OPENAI_COMPATIBLE, "https://api.mistral.ai/v1"),
    ProviderPreset("Together", ProviderProtocol.OPENAI_COMPATIBLE, "https://api.together.xyz/v1"),
    ProviderPreset("DeepSeek", ProviderProtocol.OPENAI_COMPATIBLE, "https://api.deepseek.com/v1"),
    ProviderPreset("xAI", ProviderProtocol.OPENAI_COMPATIBLE, "https://api.x.ai/v1"),
    ProviderPreset("Ollama (local)", ProviderProtocol.OPENAI_COMPATIBLE, "http://10.0.2.2:11434/v1"),
    ProviderPreset("LM Studio (local)", ProviderProtocol.OPENAI_COMPATIBLE, "http://10.0.2.2:1234/v1")
)
