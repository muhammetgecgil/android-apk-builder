from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List, Dict, Any
from tool_engine import TOOLS, authorize, safe_calc, manifest, PermissionErrorDenied

app = FastAPI(title='MG-AI Tools & Agents API', version='0.1')

class CalcRequest(BaseModel):
    expression: str
    permissions: List[str] = ['execute']

class AgentRequest(BaseModel):
    goal: str
    available_tools: List[str] = []
    permissions: List[str] = []

@app.get('/health')
def health(): return {'ok': True, 'tool_count': len(TOOLS)}

@app.get('/v1/tools')
def tools(): return manifest()

@app.post('/v1/tools/calculator')
def calculator(req: CalcRequest):
    try:
        authorize('calculator', set(req.permissions))
        return {'tool':'calculator','result':safe_calc(req.expression)}
    except PermissionErrorDenied as e:
        raise HTTPException(403, str(e))
    except Exception as e:
        raise HTTPException(400, str(e))

@app.post('/v1/agent/plan')
def agent_plan(req: AgentRequest):
    selected = []
    for name in req.available_tools:
        try:
            authorize(name, set(req.permissions))
            selected.append(name)
        except Exception:
            pass
    return {
        'goal': req.goal,
        'selected_tools': selected,
        'plan': [
            {'step':1,'action':'understand_goal'},
            {'step':2,'action':'retrieve_context'},
            {'step':3,'action':'select_authorized_tools','tools':selected},
            {'step':4,'action':'execute_with_verification'},
            {'step':5,'action':'synthesize_and_record_outcome'}
        ],
        'robot_safety_boundary':'Any robot_task is a high-level mission only; deterministic safety supervisor remains mandatory.'
    }
