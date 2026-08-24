from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Dict, Any
from model_registry_control import snapshot, stage, activate, rollback

app=FastAPI(title='MG-Core Model Registry API',version='0.15.0')

class StageRequest(BaseModel):
    model: Dict[str, Any]

class ApprovalRequest(BaseModel):
    explicit_approval: bool = False

@app.get('/health')
def health():
    s=snapshot()
    return {
        'ok': True,
        'automatic_activation': s['automatic_activation'],
        'rollback_supported': s['rollback_supported']
    }

@app.get('/v1/model-registry/state')
def state():
    return snapshot()

@app.post('/v1/model-registry/stage')
def stage_model(req: StageRequest):
    try:
        return stage(req.model)
    except Exception as e:
        raise HTTPException(400,str(e))

@app.post('/v1/model-registry/activate')
def activate_model(req: ApprovalRequest):
    try:
        return activate(req.explicit_approval)
    except Exception as e:
        raise HTTPException(400,str(e))

@app.post('/v1/model-registry/rollback')
def rollback_model(req: ApprovalRequest):
    try:
        return rollback(req.explicit_approval)
    except Exception as e:
        raise HTTPException(400,str(e))
