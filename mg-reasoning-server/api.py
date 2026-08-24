from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Dict, Any
import os, json, urllib.request
from reasoning_engine import contract_reason, decompose_task

app = FastAPI(title='MG-AI Reasoner Critic Verifier API', version='0.1')

MG_CORE_URL = os.getenv('MG_CORE_URL', 'http://mg-core:8000/v1/chat/completions')
MG_CORE_MODEL = os.getenv('MG_CORE_MODEL', 'Qwen/Qwen3.5-4B')
MG_CORE_API_KEY = os.getenv('MG_CORE_API_KEY', '')

class ReasonRequest(BaseModel):
    task: str
    evidence: List[Dict[str, Any]] = []
    deep: bool = True


def call_core(system: str, user: str) -> str:
    body = {
        'model': MG_CORE_MODEL,
        'messages': [
            {'role':'system','content':system},
            {'role':'user','content':user}
        ],
        'temperature': 0.2,
        'stream': False
    }
    req = urllib.request.Request(MG_CORE_URL, data=json.dumps(body).encode('utf-8'), headers={'Content-Type':'application/json'})
    if MG_CORE_API_KEY:
        req.add_header('Authorization', 'Bearer ' + MG_CORE_API_KEY)
    with urllib.request.urlopen(req, timeout=120) as r:
        data = json.loads(r.read().decode('utf-8'))
    return data['choices'][0]['message']['content'].strip()

@app.get('/health')
def health():
    return {'ok': True, 'engine':'mg-reasoner-critic-verifier-v0.1'}

@app.post('/v1/reason')
def reason(req: ReasonRequest):
    plan = decompose_task(req.task)
    evidence_text = '\n'.join(f"[{i+1}] {e}" for i,e in enumerate(req.evidence)) or 'No external evidence supplied.'
    try:
        candidate = call_core(
            'You are MG-AI Reasoner. Solve the task carefully. Distinguish facts, assumptions, calculations and uncertainty.',
            f"Task: {req.task}\nPlan: {plan}\nEvidence:\n{evidence_text}"
        )
        critique = call_core(
            'You are MG-AI Critic. Find material errors, unsupported claims, missing assumptions, contradictions and verification gaps. Do not merely disagree.',
            f"Task: {req.task}\nCandidate:\n{candidate}\nEvidence:\n{evidence_text}"
        )
        revision = call_core(
            'You are MG-AI Revising Reasoner. Produce a corrected answer using the candidate, critique and evidence. Do not invent missing evidence.',
            f"Task: {req.task}\nCandidate:\n{candidate}\nCritique:\n{critique}\nEvidence:\n{evidence_text}"
        )
        verification_text = call_core(
            'You are MG-AI Verifier. Return a concise assessment with VERIFIED, PARTIAL, or NEEDS_EVIDENCE and list the concrete checks that support the verdict.',
            f"Task: {req.task}\nRevised answer:\n{revision}\nEvidence:\n{evidence_text}"
        )
        base = contract_reason(req.task, candidate, critique, revision, len(req.evidence))
        base['verifier_report'] = verification_text
        base['mode'] = 'live-mg-core'
        return base
    except Exception as exc:
        base = contract_reason(req.task, 'MG-Core unavailable', str(exc), 'MG-Core unavailable', len(req.evidence))
        base['mode'] = 'contract-fallback'
        base['error'] = f'{type(exc).__name__}: {exc}'
        return base
