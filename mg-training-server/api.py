from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any
from training_engine import manifest, validate_dataset, create_job, record_evaluation, JOBS, CHECKPOINTS

app=FastAPI(title='MG-AI Training API',version='0.1')

class DatasetRequest(BaseModel):
    examples: List[Dict[str,Any]]
class JobRequest(BaseModel):
    base_model: str
    dataset_id: str
    method: str='lora_sft'
class EvalRequest(BaseModel):
    candidate: Dict[str,float]
    baseline: Dict[str,float]

@app.get('/health')
def health(): return {'ok':True,**manifest()}
@app.post('/v1/training/dataset/validate')
def dataset_validate(req:DatasetRequest): return validate_dataset(req.examples)
@app.post('/v1/training/jobs')
def jobs(req:JobRequest):
    try: return create_job(req.base_model,req.dataset_id,req.method)
    except Exception as e: raise HTTPException(400,str(e))
@app.get('/v1/training/jobs')
def list_jobs(): return {'jobs':[vars(x) for x in JOBS.values()]}
@app.get('/v1/training/jobs/{job_id}')
def get_job(job_id:str):
    if job_id not in JOBS: raise HTTPException(404,'job_not_found')
    return vars(JOBS[job_id])
@app.post('/v1/training/jobs/{job_id}/evaluate')
def evaluate(job_id:str,req:EvalRequest):
    if job_id not in JOBS: raise HTTPException(404,'job_not_found')
    try: return record_evaluation(job_id,req.candidate,req.baseline)
    except Exception as e: raise HTTPException(400,str(e))
@app.get('/v1/training/checkpoints')
def checkpoints(): return {'checkpoints':CHECKPOINTS}
