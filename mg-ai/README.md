# MG-AI v0.2

Second executable Android milestone for the MG-AI project.

## Step 2 scope

- Native Android chat screen
- Persistent local conversation history
- Real OpenAI-compatible MG-Core model adapter
- Configurable endpoint and model name
- Optional session-only API key
- Recent conversation context sent to the model
- Explicit connection/error states
- HTTP local-network development endpoints plus HTTPS production endpoints
- Isolated `mg-ai/` workspace
- GitHub Actions APK build

The adapter is provider-neutral: our future self-hosted MG-Core can expose the same `/v1/chat/completions` contract through vLLM/Ollama-compatible gateways. A third-party compatible endpoint can also be used temporarily for testing or teacher-model evaluation.

## Not implemented yet

- Dedicated hosted MG-Core server/checkpoint
- Web research engine
- RAG / vector memory
- Critic / verifier
- Vision / audio
- Robotics perception and control

## Build

```bash
gradle -p mg-ai assembleDebug
```

Expected APK:

`mg-ai/app/build/outputs/apk/debug/app-debug.apk`
