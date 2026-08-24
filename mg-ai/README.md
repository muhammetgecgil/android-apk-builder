# MG-AI v0.1

First executable Android bootstrap for the MG-AI project.

## Step 1 scope

- Native Android chat screen
- Local persistent conversation history
- Deterministic bootstrap response engine
- Internet permission reserved for the next model/backend step
- Isolated `mg-ai/` workspace so existing apps are not modified
- GitHub Actions APK build

## Deliberately not implemented yet

- Real LLM / MG-Core adapter
- Web research engine
- RAG / vector memory
- Critic / verifier
- Vision / audio
- Robotics perception and control

Those are added incrementally after the bootstrap APK is verified on-device.

## Build

```bash
gradle -p mg-ai assembleDebug
```

Expected APK:

`mg-ai/app/build/outputs/apk/debug/app-debug.apk`
