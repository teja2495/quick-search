# Add a built-in AI provider

A compatible provider takes one definition in [AiSearchLlmProviderRegistry.kt](AiSearchLlmProviderRegistry.kt), a translated name, and an icon. User-configured endpoints already work through the custom provider settings.

## Register the definition

Use an immutable lowercase ID. It identifies the provider in saved selections, model caches, backups, and preference keys. Pick a default priority for automatic provider selection. The order of the definitions controls the Settings screens and model picker.

When the API supports OpenAI-compatible `GET /models` and `POST /chat/completions`, reuse `OpenAiCompatibleLlmProvider` with the provider's HTTPS base URL and a valid fallback chat model. For example, a definition for a hypothetical provider looks like this:

```kotlin
AiSearchLlmProviderDefinition(
    id = AiSearchLlmProviderId("example"),
    searchName = "Example",
    nameRes = R.string.settings_ai_provider_example,
    priority = 7,
    branding = AiProviderBranding(icon = R.drawable.example_logo),
    create = { id ->
        OpenAiCompatibleLlmProvider(
            id = id,
            baseUrl = "https://api.example.com/v1",
            defaultModelId = "example-chat",
        )
    },
)
```

Settings badges, model search, attribution, encrypted credentials, model preferences, and backup read the definition automatically. Add the name in all 17 resource files with `scripts/add_strings.py`. Add an API key signature only for a reliably distinguishable key. The Settings tile always saves a key to the selected provider.

## Handle a provider-specific API

Implement `AiSearchLlmProvider` when the shared adapter cannot handle the protocol. Use the factory's ID as the adapter's ID. Provide a chat-capable model catalog and a fallback, and preserve model IDs and conversation history when constructing requests. Declare native web search and thinking in the definition only when the adapter implements them. `featuredModels` and `preferredModel` are optional hooks for catalog filtering and automatic selection.

## Verify

Run `scripts/verify.sh`. The registry contract test resolves each definition to its own adapter and checks that existing provider IDs and key names stay stable. On a device, validate saving and deleting a key, selecting a model, and completing a real request with that provider.
