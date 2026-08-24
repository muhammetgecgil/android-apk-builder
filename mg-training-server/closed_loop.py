from __future__ import annotations
from typing import List, Dict, Any
from dataset_exporter import export_training_jsonl
from benchmark_runner import compare_metrics
import hashlib, json, time

MODEL_REGISTRY: Dict[str, Dict[str, Any]] = {}

def eligible_experiences(experiences: List[Dict[str,Any]], candidates: List[Dict[str,Any]]) -> List[Dict[str,Any]]:
    by_exp={c.get('experience_id'):c for c in candidates}
    out=[]
    for e in experiences:
        c=by_exp.get(e.get('experience_id'))
        if not c or c.get('status')!='eligible':
            continue
        if e.get('safety_event'):
            continue
        out.append({
            'instruction':e.get('task',''),
            'response':e.get('answer',''),
            'quality':float(c.get('quality_score',0)),
            'verified':float(e.get('verifier_score',0))>=0.8,
            'teacher_verified':bool((e.get('metadata') or {}).get('teacher_verified',False)),
            'safety_event':False,
            'source':e.get('source','runtime'),
            'experience_id':e.get('experience_id')
        })
    return out

def build_dataset(experiences: List[Dict[str,Any]], candidates: List[Dict[str,Any]], output_path: str) -> Dict[str,Any]:
    rows=eligible_experiences(experiences,candidates)
    result=export_training_jsonl(rows,output_path)
    result['eligible_before_export']=len(rows)
    return result

def register_model(checkpoint: str, base_model: str, benchmark: Dict[str,Any], explicit_approval: bool) -> Dict[str,Any]:
    if not benchmark.get('pass'):
        raise ValueError('benchmark_gate_failed')
    if not explicit_approval:
        raise ValueError('explicit_approval_required')
    mid='mg-'+hashlib.sha256((checkpoint+'|'+base_model).encode()).hexdigest()[:12]
    item={'model_id':mid,'checkpoint':checkpoint,'base_model':base_model,'status':'promoted','created_at':time.time(),'benchmark':benchmark}
    MODEL_REGISTRY[mid]=item
    return item

def registry_snapshot() -> Dict[str,Any]:
    return {'models':list(MODEL_REGISTRY.values()),'count':len(MODEL_REGISTRY)}
