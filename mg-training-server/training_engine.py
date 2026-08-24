from __future__ import annotations
from dataclasses import dataclass, asdict
from typing import Dict, Any, List
import os, time, uuid

ALLOWED_METHODS = {'lora_sft'}

@dataclass
class TrainingJob:
    id: str
    base_model: str
    dataset_id: str
    method: str
    status: str
    reason: str
    created_at: float
    metrics: Dict[str, float]
    checkpoint: str | None = None

JOBS: Dict[str, TrainingJob] = {}
CHECKPOINTS: Dict[str, Dict[str, Any]] = {}


def gpu_available() -> bool:
    return os.getenv('MG_TRAIN_GPU_AVAILABLE','0') == '1'


def validate_dataset(examples: List[Dict[str, Any]]) -> Dict[str, Any]:
    accepted=[]; rejected=[]
    for i,e in enumerate(examples):
        quality=float(e.get('quality',0.0))
        safety=bool(e.get('safety_event',False))
        verified=bool(e.get('verified',False))
        teacher_ok=bool(e.get('teacher_verified',False))
        if safety:
            rejected.append({'index':i,'reason':'safety_quarantine'})
        elif quality < 0.75:
            rejected.append({'index':i,'reason':'low_quality'})
        elif not verified or not teacher_ok:
            rejected.append({'index':i,'reason':'verification_required'})
        else:
            accepted.append(e)
    return {'accepted':accepted,'rejected':rejected,'accepted_count':len(accepted),'rejected_count':len(rejected)}


def create_job(base_model: str, dataset_id: str, method: str='lora_sft') -> Dict[str, Any]:
    if method not in ALLOWED_METHODS:
        raise ValueError('unsupported_training_method')
    jid='train_'+uuid.uuid4().hex[:12]
    ready=gpu_available()
    j=TrainingJob(jid,base_model,dataset_id,method,'pending' if ready else 'blocked',
                  '' if ready else 'gpu_unavailable',time.time(),{})
    JOBS[jid]=j
    return asdict(j)


def record_evaluation(job_id: str, candidate: Dict[str,float], baseline: Dict[str,float]) -> Dict[str, Any]:
    j=JOBS[job_id]
    required=['quality','safety','reasoning','hallucination']
    if any(k not in candidate or k not in baseline for k in required):
        raise ValueError('missing_evaluation_metric')
    regressions=[]
    if candidate['quality'] < baseline['quality'] - 0.01: regressions.append('quality')
    if candidate['safety'] < baseline['safety']: regressions.append('safety')
    if candidate['reasoning'] < baseline['reasoning'] - 0.01: regressions.append('reasoning')
    if candidate['hallucination'] > baseline['hallucination'] + 0.01: regressions.append('hallucination')
    promoted = not regressions and candidate['safety'] >= 0.99
    j.metrics={f'candidate_{k}':v for k,v in candidate.items()}
    j.metrics.update({f'baseline_{k}':v for k,v in baseline.items()})
    if promoted:
        ck='ckpt_'+uuid.uuid4().hex[:10]
        j.status='promoted'; j.checkpoint=ck; j.reason=''
        CHECKPOINTS[ck]={'job_id':job_id,'base_model':j.base_model,'method':j.method,'created_at':time.time(),'status':'candidate_promoted'}
    else:
        j.status='rejected'; j.reason='regression:'+','.join(regressions) if regressions else 'safety_gate_failed'
    return {'promoted':promoted,'regressions':regressions,'job':asdict(j)}


def manifest() -> Dict[str, Any]:
    return {
        'methods':sorted(ALLOWED_METHODS),
        'online_weight_update':False,
        'robot_runtime_weight_update':False,
        'internet_direct_to_weights':False,
        'promotion_requires_offline_eval':True,
        'gpu_available':gpu_available(),
    }
