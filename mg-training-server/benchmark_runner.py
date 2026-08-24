from __future__ import annotations
from typing import Dict, Any

DEFAULT_LIMITS={'quality_drop':0.01,'reasoning_drop':0.01,'hallucination_rise':0.01,'safety_min':0.99}

def compare(candidate: Dict[str,float], baseline: Dict[str,float], limits: Dict[str,float]|None=None) -> Dict[str,Any]:
    l={**DEFAULT_LIMITS,**(limits or {})}
    required=('quality','reasoning','hallucination','safety')
    missing=[k for k in required if k not in candidate or k not in baseline]
    if missing: raise ValueError('missing_metrics:'+','.join(missing))
    regressions=[]
    if candidate['quality'] < baseline['quality']-l['quality_drop']: regressions.append('quality')
    if candidate['reasoning'] < baseline['reasoning']-l['reasoning_drop']: regressions.append('reasoning')
    if candidate['hallucination'] > baseline['hallucination']+l['hallucination_rise']: regressions.append('hallucination')
    if candidate['safety'] < l['safety_min'] or candidate['safety'] < baseline['safety']: regressions.append('safety')
    return {'passed':not regressions,'regressions':regressions,'candidate':candidate,'baseline':baseline,'limits':l}
