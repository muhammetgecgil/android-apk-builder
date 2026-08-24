from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any
from training_engine import manifest, validate_dataset, create_job, record_evaluation, JOBS, CHECKPOINTS
from training_pipeline import TrainingRegistry

app=FastAPI(title='MG-AI Training API',version='0.13.0')
reg=TrainingRegistry()

class DatasetRequest(BaseModel): examples: List[Dict[str,Any]]
class JobRequest(BaseModel): base_model:str; dataset_id:str; method:str='lora_sft'
class EvalRequest(BaseModel): candidate:Dict[str,float]; baseline:Dict[str,float]
class CandidateRequest(BaseModel): base_model:str='Qwen/Qwen3.5-4B'; dataset_id:str; method:str='lora_sft'
class GateRequest(BaseModel): verified_samples:int; safety_events:int=0; quality_score:float
class CheckpointRequest(BaseModel): checkpoint:str
class PromotionEvalRequest(BaseModel): quality_score:float; regression_score:float; safety_pass:bool
class PromoteRequest(BaseModel): explicit_approval:bool=False

@app.get('/health')
def health(): return {'ok':True,**manifest(),'online_weight_updates':False,'robot_runtime_training':False,'automatic_promotion':False}
@app.post('/v1/training/dataset/validate')
def dataset_validate(req:DatasetRequest): return validate_dataset(req.examples)
@app.post('/v1/training/jobs')
def jobs(req:JobRequest):
    try:return create_job(req.base_model,req.dataset_id,req.method)
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
    try:return record_evaluation(job_id,req.candidate,req.baseline)
    except Exception as e: raise HTTPException(400,str(e))
@app.get('/v1/training/checkpoints')
def checkpoints(): return {'checkpoints':CHECKPOINTS}

@app.post('/v1/training/candidates')
def candidate(req:CandidateRequest):
    c=reg.create(req.base_model,req.dataset_id,req.method); return reg.snapshot(c.id)
@app.post('/v1/training/candidates/{cid}/dataset-gate')
def candidate_gate(cid:str,req:GateRequest):
    try:return reg.snapshot(reg.dataset_gate(cid,req.verified_samples,req.safety_events,req.quality_score).id)
    except Exception as e: raise HTTPException(400,str(e))
@app.post('/v1/training/candidates/{cid}/start')
def candidate_start(cid:str):
    try:return reg.snapshot(reg.mark_training(cid).id)
    except Exception as e: raise HTTPException(400,str(e))
@app.post('/v1/training/candidates/{cid}/checkpoint')
def candidate_checkpoint(cid:str,req:CheckpointRequest):
    try:return reg.snapshot(reg.register_checkpoint(cid,req.checkpoint).id)
    except Exception as e: raise HTTPException(400,str(e))
@app.post('/v1/training/candidates/{cid}/promotion-eval')
def promotion_eval(cid:str,req:PromotionEvalRequest):
    try:return reg.snapshot(reg.evaluation_gate(cid,req.quality_score,req.regression_score,req.safety_pass).id)
    except Exception as e: raise HTTPException(400,str(e))
@app.post('/v1/training/candidates/{cid}/promote')
def promote(cid:str,req:PromoteRequest):
    try:return reg.snapshot(reg.promote(cid,req.explicit_approval).id)
    except Exception as e: raise HTTPException(400,str(e))
