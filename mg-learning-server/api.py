from fastapi import FastAPI
from pydantic import BaseModel
from typing import Dict, Any, List
from learning_engine import Experience, score_experience, compare_teachers, promotion_gate, RULES, make_id

app=FastAPI(title='MG-AI Controlled Learning API',version='0.2')
EXPERIENCES: Dict[str,Experience]={}
CANDIDATES: Dict[str,Dict[str,Any]]={}

class ExperienceRequest(BaseModel):
    task:str
    answer:str
    outcome_score:float
    verifier_score:float
    user_feedback:float
    safety_event:bool=False
    source:str='runtime'
    metadata:Dict[str,Any]={}

class TeacherRequest(BaseModel): outputs:List[Dict[str,Any]]
class PromoteRequest(BaseModel): metrics:Dict[str,float]; baseline:Dict[str,float]

@app.get('/health')
def health(): return {'ok':True,'experiences':len(EXPERIENCES),'rules':RULES}
@app.get('/v1/learning/rules')
def rules(): return RULES
@app.post('/v1/experience')
def experience(req:ExperienceRequest):
    eid=make_id('exp',req.task+'|'+req.answer)
    exp=Experience(eid,req.task,req.answer,req.outcome_score,req.verifier_score,req.user_feedback,req.source,req.safety_event,req.metadata)
    EXPERIENCES[eid]=exp
    cand=score_experience(exp)
    CANDIDATES[cand.candidate_id]=cand.__dict__
    return {'experience':exp.__dict__,'candidate':cand.__dict__}
@app.post('/v1/teacher/compare')
def teacher(req:TeacherRequest): return compare_teachers(req.outputs)
@app.post('/v1/promotion/evaluate')
def promote(req:PromoteRequest): return promotion_gate(req.metrics,req.baseline)
@app.get('/v1/candidates')
def candidates(): return {'items':list(CANDIDATES.values()),'count':len(CANDIDATES)}
@app.get('/v1/training/export')
def training_export():
    eligible=[]
    by_exp={c['experience_id']:c for c in CANDIDATES.values() if c.get('status')=='eligible'}
    for eid,exp in EXPERIENCES.items():
        c=by_exp.get(eid)
        if not c or exp.safety_event: continue
        item=exp.__dict__.copy()
        item['candidate']=c
        eligible.append(item)
    return {'experiences':eligible,'candidates':[x['candidate'] for x in eligible],'count':len(eligible),'rules':RULES}
