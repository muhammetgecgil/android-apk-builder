from fastapi import FastAPI
from orchestrator import snapshot, SERVICES

app=FastAPI(title='MG-AI Unified Orchestrator',version='0.16.0')

@app.get('/health')
def health():
    return {'ok':True,'service_count':len(SERVICES),'automatic_model_activation':False,'robot_safety_bypass':False}

@app.get('/v1/system/status')
def system_status():
    return snapshot()
