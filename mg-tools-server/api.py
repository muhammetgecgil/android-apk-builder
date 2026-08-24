from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import List
from tool_engine import TOOLS, authorize, safe_calc, manifest, PermissionErrorDenied
from sandbox_worker import run_python, SandboxRejected
from agent_runtime import create_agent, transition, get_agent

app = FastAPI(title='MG-AI Tools & Agents API', version='0.2')

class CalcRequest(BaseModel):
    expression: str
    permissions: List[str] = ['execute']

class PythonRequest(BaseModel):
    code: str
    permissions: List[str] = ['execute']

class AgentRequest(BaseModel):
    goal: str
    available_tools: List[str] = []
    permissions: List[str] = []

class AgentCreateRequest(BaseModel):
    role: str = 'general'
    goal: str
    permissions: List[str] = []

class AgentTransitionRequest(BaseModel):
    state: str
    error: str | None = None

@app.get('/health')
def health(): return {'ok': True, 'tool_count': len(TOOLS)}

@app.get('/v1/tools')
def tools(): return manifest()

@app.post('/v1/tools/calculator')
def calculator(req: CalcRequest):
    try:
        authorize('calculator', set(req.permissions))
        return {'tool':'calculator','result':safe_calc(req.expression),'verified':'deterministic_ast'}
    except PermissionErrorDenied as e:
        raise HTTPException(403, str(e))
    except Exception as e:
        raise HTTPException(400, str(e))

@app.post('/v1/tools/python')
def python_tool(req: PythonRequest):
    try:
        authorize('python_sandbox', set(req.permissions))
        result = run_python(req.code)
        result['tool'] = 'python_sandbox'
        result['requires_verifier'] = True
        return result
    except PermissionErrorDenied as e:
        raise HTTPException(403, str(e))
    except SandboxRejected as e:
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
            {'step':2,'action':'retrieve_memory'},
            {'step':3,'action':'research_if_needed'},
            {'step':4,'action':'select_authorized_tools','tools':selected},
            {'step':5,'action':'execute'},
            {'step':6,'action':'verify_tool_results'},
            {'step':7,'action':'synthesize_and_record_outcome'}
        ],
        'robot_safety_boundary':'Any robot_task is a high-level mission only; deterministic safety supervisor remains mandatory.'
    }

@app.post('/v1/agents')
def create_agent_api(req: AgentCreateRequest):
    return create_agent(req.role, req.goal, req.permissions).to_dict()

@app.get('/v1/agents/{agent_id}')
def get_agent_api(agent_id: str):
    try:
        return get_agent(agent_id).to_dict()
    except KeyError as e:
        raise HTTPException(404, str(e))

@app.post('/v1/agents/{agent_id}/transition')
def transition_agent_api(agent_id: str, req: AgentTransitionRequest):
    try:
        return transition(agent_id, req.state, req.error).to_dict()
    except KeyError as e:
        raise HTTPException(404, str(e))
    except ValueError as e:
        raise HTTPException(400, str(e))
