from dataclasses import dataclass, asdict
from typing import List, Dict, Any
import json

@dataclass
class ReasoningTrace:
    task: str
    plan: List[str]
    candidate: str
    critique: str
    revision: str
    verification: Dict[str, Any]

    def to_dict(self):
        return asdict(self)


def decompose_task(task: str) -> List[str]:
    task = (task or '').strip()
    if not task:
        return []
    # Contract-level deterministic planner. Production planner will use MG-Core.
    return [
        'Problemi ve hedefi açıkça tanımla',
        'Gerekli bilgi, hafıza ve araçları belirle',
        'Aday çözüm üret',
        'Varsayım ve hata noktalarını eleştir',
        'Gerekirse çözümü revize et',
        'Sonucu doğrulama kriterlerine göre değerlendir'
    ]


def score_verification(candidate: str, critique: str, evidence_count: int = 0) -> Dict[str, Any]:
    checks = {
        'non_empty': bool(candidate.strip()),
        'has_critique': bool(critique.strip()),
        'has_evidence': evidence_count > 0,
    }
    passed = sum(1 for v in checks.values() if v)
    confidence = round(passed / len(checks), 3)
    return {
        'checks': checks,
        'confidence': confidence,
        'status': 'verified' if confidence >= 0.67 else 'needs_more_evidence'
    }


def contract_reason(task: str, candidate: str, critique: str = '', revision: str | None = None, evidence_count: int = 0) -> Dict[str, Any]:
    plan = decompose_task(task)
    revised = revision if revision is not None else candidate
    verification = score_verification(revised, critique, evidence_count)
    trace = ReasoningTrace(task=task, plan=plan, candidate=candidate, critique=critique, revision=revised, verification=verification)
    return trace.to_dict()
