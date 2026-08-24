from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from typing import Dict, Any, List
import secrets

ALLOWED_STATES = {'created','planning','running','verifying','completed','failed','cancelled'}

@dataclass
class AgentSession:
    agent_id: str
    role: str
    goal: str
    permissions: List[str]
    state: str = 'created'
    created_at: str = ''
    updated_at: str = ''
    last_error: str | None = None

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)

SESSIONS: Dict[str, AgentSession] = {}

def _now() -> str:
    return datetime.now(timezone.utc).isoformat()

def create_agent(role: str, goal: str, permissions: List[str]) -> AgentSession:
    aid = 'agt_' + secrets.token_hex(6)
    t = _now()
    s = AgentSession(aid, role.strip()[:80] or 'general', goal.strip()[:2000], sorted(set(permissions)), created_at=t, updated_at=t)
    SESSIONS[aid] = s
    return s

def transition(agent_id: str, target: str, error: str | None = None) -> AgentSession:
    if target not in ALLOWED_STATES:
        raise ValueError('invalid_state')
    s = SESSIONS.get(agent_id)
    if not s:
        raise KeyError('agent_not_found')
    if s.state in {'completed','failed','cancelled'}:
        raise ValueError('agent_already_terminal')
    allowed = {
        'created': {'planning','cancelled'},
        'planning': {'running','failed','cancelled'},
        'running': {'verifying','failed','cancelled'},
        'verifying': {'completed','failed','cancelled'},
    }
    if target not in allowed.get(s.state, set()):
        raise ValueError(f'invalid_transition:{s.state}->{target}')
    s.state = target
    s.updated_at = _now()
    s.last_error = error
    return s

def get_agent(agent_id: str) -> AgentSession:
    s = SESSIONS.get(agent_id)
    if not s:
        raise KeyError('agent_not_found')
    return s
