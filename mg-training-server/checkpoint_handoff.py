from __future__ import annotations
import argparse, hashlib, json, os, time
from pathlib import Path

def dir_digest(path: str) -> str:
    root=Path(path)
    h=hashlib.sha256()
    files=sorted([p for p in root.rglob('*') if p.is_file()])
    for p in files:
        h.update(str(p.relative_to(root)).encode())
        h.update(str(p.stat().st_size).encode())
    return h.hexdigest()

def build(checkpoint: str, base_model: str) -> dict:
    p=Path(checkpoint)
    if not p.exists(): raise FileNotFoundError(checkpoint)
    metrics_path=p/'training_metrics.json'
    metrics={}
    if metrics_path.exists():
        metrics=json.loads(metrics_path.read_text(encoding='utf-8'))
    return {
        'schema':'mg-ai-checkpoint-handoff/v1',
        'checkpoint':str(p),
        'base_model':base_model,
        'artifact_digest':dir_digest(str(p)),
        'training_metrics':metrics,
        'created_at':time.time(),
        'status':'awaiting_benchmark',
        'automatic_activation':False,
        'requires_benchmark':True,
        'requires_explicit_activation':True,
        'rollback_required':True,
    }

def main():
    ap=argparse.ArgumentParser(); ap.add_argument('--checkpoint',required=True); ap.add_argument('--base-model',required=True); ap.add_argument('--output',required=True)
    a=ap.parse_args(); out=build(a.checkpoint,a.base_model); Path(a.output).write_text(json.dumps(out,indent=2),encoding='utf-8'); print(json.dumps(out))
if __name__=='__main__': main()
