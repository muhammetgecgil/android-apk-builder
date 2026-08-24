# MG-Core v0.1

MG-Core is the self-hosted foundation-model serving layer for MG-AI.

## Baseline model

Initial reference model: `Qwen/Qwen3.5-4B`.

Why this baseline:
- open-weight Apache-2.0 family
- supports modern reasoning/multimodal roadmap
- compatible with vLLM/OpenAI-compatible serving
- small enough to be the development baseline while larger MG-Core variants are evaluated later

The model is not considered "our trained MG-Core" yet. It is the bootstrap foundation model. Later stages add domain continued-pretraining, instruction tuning, tool/reasoning training, evaluation gates, and versioned MG-Core checkpoints.

## Serving contract

The Android app uses an OpenAI-compatible endpoint:

`POST /v1/chat/completions`

vLLM exposes that API directly. The APK therefore does not need to change when the underlying model is upgraded.

## Start on a GPU host

1. Copy `.env.example` to `.env` and change `MG_CORE_API_KEY`.
2. Run:

```bash
docker compose up
```

3. Test:

```bash
python smoke_test.py --url http://localhost:8000/v1 --api-key YOUR_KEY --model Qwen/Qwen3.5-4B
```

## Android settings

Endpoint: `http://YOUR_GPU_HOST:8000/v1/chat/completions`
Model: `Qwen/Qwen3.5-4B`
API key: the value configured on the server.

Use HTTPS/reverse proxy before exposing this outside a trusted development network.

## Safety boundary

MG-Core produces high-level text/tool/planning output only. Future robot actuation must pass through the deterministic Robotics Safety Supervisor and real-time controller. MG-Core will never directly drive PWM, current, torque, servo timing, safety PLC, or E-stop circuitry.
