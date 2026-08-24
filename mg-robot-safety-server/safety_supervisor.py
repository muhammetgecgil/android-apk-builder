from dataclasses import dataclass, field, asdict
from typing import Dict, List, Any, Optional
import math

@dataclass
class JointLimit:
    minimum: float
    maximum: float
    max_velocity: float

@dataclass
class SafetyConfig:
    config_id: str
    joint_limits: Dict[str, JointLimit]
    max_tcp_speed_m_s: float
    max_force_n: float
    min_human_distance_m: float
    max_perception_age_ms: int
    min_localization_confidence: float
    workspace_min_xyz: List[float]
    workspace_max_xyz: List[float]
    forbidden_aabbs: List[Dict[str, List[float]]] = field(default_factory=list)

@dataclass
class SafetyState:
    estop_active: bool
    protective_stop_active: bool
    human_distance_m: Optional[float]
    perception_age_ms: int
    localization_confidence: float

@dataclass
class MotionProposal:
    proposal_id: str
    joint_positions: Dict[str, float]
    joint_velocities: Dict[str, float]
    tcp_position_xyz: List[float]
    requested_tcp_speed_m_s: float
    requested_force_n: float
    planned_contact: bool = False

class SafetySupervisor:
    def __init__(self):
        self.config: Optional[SafetyConfig] = None
        self.audit: List[Dict[str, Any]] = []

    def configure(self, c: SafetyConfig):
        if not c.config_id: raise ValueError('missing_config_id')
        if not c.joint_limits: raise ValueError('missing_joint_limits')
        if c.max_tcp_speed_m_s <= 0 or c.max_force_n <= 0: raise ValueError('invalid_positive_limit')
        if c.min_human_distance_m < 0 or c.max_perception_age_ms < 0: raise ValueError('invalid_safety_threshold')
        if not 0 <= c.min_localization_confidence <= 1: raise ValueError('invalid_localization_threshold')
        _box(c.workspace_min_xyz,c.workspace_max_xyz)
        for name,l in c.joint_limits.items():
            if l.minimum >= l.maximum or l.max_velocity <= 0: raise ValueError(f'invalid_joint_limit:{name}')
        for z in c.forbidden_aabbs: _box(z['min'],z['max'])
        self.config=c
        return {'configured':True,'config_id':c.config_id,'rule':'No AI may bypass this deterministic evaluation.'}

    def evaluate(self, p: MotionProposal, s: SafetyState):
        reasons=[]; c=self.config
        if c is None: reasons.append('safety_config_missing')
        if s.estop_active: reasons.append('estop_active')
        if s.protective_stop_active: reasons.append('protective_stop_active')
        if c is not None:
            if s.perception_age_ms > c.max_perception_age_ms: reasons.append('stale_perception')
            if s.localization_confidence < c.min_localization_confidence: reasons.append('localization_confidence_low')
            if s.human_distance_m is None: reasons.append('human_distance_unknown')
            elif s.human_distance_m < c.min_human_distance_m: reasons.append('human_too_close')
            if p.requested_tcp_speed_m_s < 0 or p.requested_tcp_speed_m_s > c.max_tcp_speed_m_s: reasons.append('tcp_speed_limit')
            if p.requested_force_n < 0 or p.requested_force_n > c.max_force_n: reasons.append('force_limit')
            if not _inside(p.tcp_position_xyz,c.workspace_min_xyz,c.workspace_max_xyz): reasons.append('outside_workspace')
            for z in c.forbidden_aabbs:
                if _inside(p.tcp_position_xyz,z['min'],z['max']): reasons.append('forbidden_zone'); break
            for name,limit in c.joint_limits.items():
                if name not in p.joint_positions: reasons.append(f'joint_position_missing:{name}'); continue
                q=float(p.joint_positions[name]); v=abs(float(p.joint_velocities.get(name,0.0)))
                if not math.isfinite(q) or q < limit.minimum or q > limit.maximum: reasons.append(f'joint_position_limit:{name}')
                if not math.isfinite(v) or v > limit.max_velocity: reasons.append(f'joint_velocity_limit:{name}')
        decision='ALLOW' if not reasons else ('HOLD' if all(x in {'stale_perception','localization_confidence_low','human_distance_unknown'} for x in reasons) else 'REJECT')
        result={'proposal_id':p.proposal_id,'decision':decision,'reasons':reasons,'config_id':c.config_id if c else None,'deterministic':True,'actuator_command':None}
        self.audit.append(result.copy())
        return result

def _vec(v):
    if len(v)!=3 or not all(math.isfinite(float(x)) for x in v): raise ValueError('invalid_xyz')
    return [float(x) for x in v]

def _box(mn,mx):
    mn=_vec(mn); mx=_vec(mx)
    if any(a>=b for a,b in zip(mn,mx)): raise ValueError('invalid_aabb')

def _inside(p,mn,mx):
    p=_vec(p); return all(float(a)<=float(x)<=float(b) for x,a,b in zip(p,mn,mx))
