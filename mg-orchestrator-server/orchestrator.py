from __future__ import annotations
from dataclasses import dataclass, asdict
from typing import Dict, Any
import os, time, urllib.request, json

SERVICES={
 'mg_core':os.getenv('MG_CORE_HEALTH','http://mg-core:8000/v1/models'),
 'research':os.getenv('MG_RESEARCH_HEALTH','http://research:8010/health'),
 'memory':os.getenv('MG_MEMORY_HEALTH','http://memory:8020/health'),
 'reasoning':os.getenv('MG_REASONING_HEALTH','http://reasoning:8030/health'),
 'tools':os.getenv('MG_TOOLS_HEALTH','http://tools:8040/health'),
 'learning':os.getenv('MG_LEARNING_HEALTH','http://learning:8050/health'),
 'training':os.getenv('MG_TRAINING_HEALTH','http://training:8060/health'),
 'multimodal':os.getenv('MG_MULTIMODAL_HEALTH','http://multimodal:8070/health'),
 'robot_world':os.getenv('MG_ROBOT_WORLD_HEALTH','http://robot-world:8080/health'),
 'robot_safety':os.getenv('MG_ROBOT_SAFETY_HEALTH','http://robot-safety:8081/health'),
 'model_registry':os.getenv('MG_MODEL_REGISTRY_HEALTH','http://model-registry:8090/health'),
}

@dataclass
class ServiceStatus:
    name:str; url:str; ok:bool; latency_ms:int; detail:str; checked_at:float

def check(name:str,url:str,timeout:float=2.5)->ServiceStatus:
    start=time.time()
    try:
        with urllib.request.urlopen(url,timeout=timeout) as r:
            body=r.read(2048).decode('utf-8','replace')
            ok=200 <= r.status < 300
            detail=body[:500]
    except Exception as e:
        ok=False; detail=str(e)
    return ServiceStatus(name,url,ok,int((time.time()-start)*1000),detail,time.time())

def snapshot()->Dict[str,Any]:
    items=[check(n,u) for n,u in SERVICES.items()]
    healthy=sum(1 for x in items if x.ok)
    return {
        'services':[asdict(x) for x in items],
        'healthy':healthy,
        'total':len(items),
        'all_healthy':healthy==len(items),
        'degraded':0 < healthy < len(items),
        'checked_at':time.time(),
        'automatic_model_activation':False,
        'robot_safety_bypass':False,
    }
