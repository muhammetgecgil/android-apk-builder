#!/usr/bin/env bash
set -euo pipefail
BASE_MODEL="${BASE_MODEL:-Qwen/Qwen3.5-4B}"
DATASET="${DATASET:-/data/train.jsonl}"
OUTPUT_DIR="${OUTPUT_DIR:-/checkpoints/mg-ai-first-lora}"
METHOD="${METHOD:-qlora_sft}"
RESUME_FROM="${RESUME_FROM:-}"
ARGS=(--base-model "$BASE_MODEL" --dataset "$DATASET" --output-dir "$OUTPUT_DIR" --method "$METHOD")
if [[ -n "$RESUME_FROM" ]]; then ARGS+=(--resume-from "$RESUME_FROM"); fi
python3 gpu_trainer.py "${ARGS[@]}"
python3 checkpoint_handoff.py --checkpoint "$OUTPUT_DIR" --base-model "$BASE_MODEL" --output "$OUTPUT_DIR/handoff.json"
echo "Training complete. Handoff manifest: $OUTPUT_DIR/handoff.json"
