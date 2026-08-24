from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Dict, List, Optional
from safety_supervisor import SafetySupervisor, SafetyConfig, JointLimit, SafetyState, MotionProposal

app = FastAPI(title='MG-AI Deterministic Robotics Safety Supervisor', version='0.1')
SUPERVISOR = SafetySupervisor()

class JointLimitRequest(BaseModel):
    minimum: float
    maximum: float
    max_velocity: float

class ConfigRequest(BaseModel):
    config_id: str
    joint_limits: Dict[str, JointLimitRequest]
    max_tcp_speed_m_s: float
    max_force_n: float
    min_human_distance_m: float
    max_perception_age_ms: int
    min_localization_confidence: float
    workspace_min_xyz: List[float]
    workspace_max_xyz: List[float]
    forbidden_aabbs: List[Dict[str,List[float]]] = []

class StateRequest(BaseModel):
    estop_active: bool
    protective_stop_active: bool
    human_distance_m: Optional[float] = None
    perception_age_ms: int
    localization_confidence: float

class ProposalRequest(BaseModel):
    proposal_id: str
    joint_positions: Dict[str,float]
    joint_velocities: Dict[str,float]
    tcp_position_xyz: List[float]
    requested_tcp_speed_m_s: float
    requested_force_n: float
    planned_contact: bool = False

class EvaluateRequest(BaseModel):
    proposal: ProposalRequest
    state: StateRequest

@app.get('/health')
def health():
    return {
        'ok': True,
        'deterministic': True,
        'configured': SUPERVISOR.config is not None,
        'default_when_unconfigured': 'REJECT',
        'actuator_endpoints': 0,
        'llm_dependency': False
    }

@app.post('/v1/safety/configure')
def configure(req: ConfigRequest):
    try:
        c=SafetyConfig(
            config_id=req.config_id,
            joint_limits={k:JointLimit(**v.model_dump()) for k,v in req.joint_limits.items()},
            max_tcp_speed_m_s=req.max_tcp_speed_m_s,
            max_force_n=req.max_force_n,
            min_human_distance_m=req.min_human_distance_m,
            max_perception_age_ms=req.max_perception_age_ms,
            min_localization_confidence=req.min_localization_confidence,
            workspace_min_xyz=req.workspace_min_xyz,
            workspace_max_xyz=req.workspace_max_xyz,
            forbidden_aabbs=req.forbidden_aabbs)
        return SUPERVISOR.configure(c)
    except Exception as e: raise HTTPException(400,str(e))

@app.post('/v1/safety/evaluate')
def evaluate(req: EvaluateRequest):
    try:
        return SUPERVISOR.evaluate(MotionProposal(**req.proposal.model_dump()),SafetyState(**req.state.model_dump()))
    except Exception as e: raise HTTPException(400,str(e))

@app.get('/v1/safety/audit')
def audit():
    return {'events':SUPERVISOR.audit[-500:],'count':len(SUPERVISOR.audit)}
