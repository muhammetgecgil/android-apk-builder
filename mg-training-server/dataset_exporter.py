from __future__ import annotations
import json
from pathlib import Path
from typing import Iterable, Dict, Any, List

REQUIRED = ('prompt','response','verified','teacher_verified','quality','safety_event')

def clean_examples(examples: Iterable[Dict[str,Any]], min_quality: float = 0.80) -> Dict[str,Any]:
    accepted: List[Dict[str,Any]]=[]
    rejected: List[Dict[str,Any]]=[]
    seen=set()
    for i,e in enumerate(examples):
        missing=[k for k in REQUIRED if k not in e]
        if missing:
            rejected.append({'index':i,'reason':'missing_fields','fields':missing}); continue
        if bool(e.get('safety_event')):
            rejected.append({'index':i,'reason':'safety_quarantine'}); continue
        if not bool(e.get('verified')) or not bool(e.get('teacher_verified')):
            rejected.append({'index':i,'reason':'verification_required'}); continue
        if float(e.get('quality',0.0)) < min_quality:
            rejected.append({'index':i,'reason':'low_quality'}); continue
        prompt=str(e.get('prompt','')).strip(); response=str(e.get('response','')).strip()
        if not prompt or not response:
            rejected.append({'index':i,'reason':'empty_text'}); continue
        key=(prompt,response)
        if key in seen:
            rejected.append({'index':i,'reason':'duplicate'}); continue
        seen.add(key)
        accepted.append({'messages':[{'role':'user','content':prompt},{'role':'assistant','content':response}],
                         'quality':float(e['quality']),'provenance':e.get('provenance',{}),'source_id':e.get('id',f'exp-{i}')})
    return {'accepted':accepted,'rejected':rejected,'accepted_count':len(accepted),'rejected_count':len(rejected)}

def export_jsonl(examples: Iterable[Dict[str,Any]], output_path: str, min_quality: float = 0.80) -> Dict[str,Any]:
    result=clean_examples(examples,min_quality)
    p=Path(output_path); p.parent.mkdir(parents=True,exist_ok=True)
    with p.open('w',encoding='utf-8') as f:
        for row in result['accepted']:
            f.write(json.dumps(row,ensure_ascii=False)+'\n')
    result['output_path']=str(p)
    return result
