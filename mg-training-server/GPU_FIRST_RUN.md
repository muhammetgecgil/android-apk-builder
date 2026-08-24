# MG-AI first real GPU fine-tuning run

## Goal
Run the first controlled QLoRA fine-tune, produce a checkpoint handoff manifest, then benchmark before staging in MG-Core.

## Preconditions
- NVIDIA GPU host with Docker + NVIDIA Container Toolkit
- verified training JSONL exported by the Learning/Training closed loop
- sufficient local disk for model cache + checkpoints
- no robot runtime training; no online weight updates

## Build
```bash
docker build -f Dockerfile.gpu -t mg-ai-trainer:0.15 .
```

## Run
```bash
docker run --rm --gpus all \
  -v /srv/mg-ai/data:/data \
  -v /srv/mg-ai/checkpoints:/checkpoints \
  -v /srv/mg-ai/hf-cache:/root/.cache/huggingface \
  -e BASE_MODEL=Qwen/Qwen3.5-4B \
  -e DATASET=/data/train.jsonl \
  -e OUTPUT_DIR=/checkpoints/mg-ai-first-lora \
  -e METHOD=qlora_sft \
  mg-ai-trainer:0.15
```

## Required post-training gates
1. Confirm `training_metrics.json` exists.
2. Confirm `handoff.json` exists and says `automatic_activation=false`.
3. Run benchmark against the currently active baseline.
4. Reject on safety regression, hallucination regression, or unacceptable quality/reasoning regression.
5. Only after benchmark PASS, register the checkpoint as promoted/staged.
6. Activation is a separate explicit operation.
7. Keep the previous active model as rollback target.

## Production note
The first run should intentionally use a small, high-quality dataset and conservative steps. It is a pipeline validation run, not a claim that the resulting adapter is globally superior to the base model.
