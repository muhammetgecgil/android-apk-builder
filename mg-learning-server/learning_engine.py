from dataclasses import dataclass, asdict
from typing import Dict, Any, List
import hashlib, time

@dataclass
class Experience:
    experience_id: str
    task: str
    answer: str
    outcome_score: float
    verifier_score: float
    user_feedback: float
    source: str = 'runtime'
    safety_event: bool = False
    metadata: Dict[str, Any] | None = None

@dataclass
class TrainingCandidate:
    candidate_id: str
    experience_id: str
    quality_score: float
    status: str
    reasons: List[str]


def make_id(prefix: str, text: str) -> str:
    return prefix + '-' + hashlib.sha256(text.encode('utf-8')).hexdigest()[:16]


def score_experience(exp: Experience) -> TrainingCandidate:
    reasons=[]
    if exp.safety_event:
        return TrainingCandidate(make_id('cand', exp.experience_id), exp.experience_id, 0.0, 'quarantine', ['safety_event'])
    q = 0.45*max(0,min(1,exp.verifier_score)) + 0.35*max(0,min(1,exp.outcome_score)) + 0.20*max(0,min(1,exp.user_feedback))
    if q >= 0.85:
        status='eligible'
        reasons.append('high_quality')
    elif q >= 0.65:
        status='review'
        reasons.append('needs_additional_review')
    else:
        status='reject'
        reasons.append('low_quality')
    return TrainingCandidate(make_id('cand', exp.experience_id), exp.experience_id, round(q,4), status, reasons)


def compare_teachers(outputs: List[Dict[str,Any]]) -> Dict[str,Any]:
    valid=[x for x in outputs if x.get('verifier_score',0) >= 0.8 and x.get('text')]
    if not valid:
        return {'status':'reject','reason':'no_verified_teacher'}
    best=max(valid,key=lambda x:x.get('verifier_score',0))
    return {'status':'candidate','teacher_id':best.get('teacher_id'),'text':best['text'],'verifier_score':best['verifier_score']}


def promotion_gate(metrics: Dict[str,float], baseline: Dict[str,float]) -> Dict[str,Any]:
    required=['quality','safety','calibration']
    for k in required:
        if metrics.get(k,0) < baseline.get(k,0):
            return {'decision':'reject','reason':f'regression:{k}'}
    if metrics.get('safety',0) < 0.99:
        return {'decision':'reject','reason':'safety_floor'}
    return {'decision':'promote_candidate','reason':'no_required_regression'}

RULES={
 'online_weight_updates':False,
 'robot_runtime_weight_updates':False,
 'internet_direct_to_weights':False,
 'teacher_output_requires_verification':True,
 'promotion_requires_offline_evaluation':True,
 'rollback_required':True,
}
