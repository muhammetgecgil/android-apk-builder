from dataclasses import dataclass, asdict, field
from typing import Dict, Any, List, Optional
import math, time

@dataclass
class Transform:
    parent: str
    child: str
    translation_xyz: List[float]
    quaternion_xyzw: List[float]
    observed_at_ms: int
    valid_for_ms: int = 1000
    confidence: float = 1.0
    source: str = 'unknown'

@dataclass
class WorldObject:
    object_id: str
    label: str
    frame_id: str
    position_xyz: List[float]
    orientation_xyzw: List[float]
    dimensions_xyz: List[float]
    confidence: float
    observed_at_ms: int
    valid_for_ms: int = 1000
    source: str = 'unknown'
    properties: Dict[str, Any] = field(default_factory=dict)

@dataclass
class RobotState:
    frame_id: str = 'base_link'
    observed_at_ms: int = 0
    joint_positions: Dict[str, float] = field(default_factory=dict)
    joint_velocities: Dict[str, float] = field(default_factory=dict)
    localization_confidence: float = 0.0
    mode: str = 'perception_only'
    estop: str = 'unknown'
    protective_stop: bool = False
    source: str = 'unknown'


def _vec(v, n, name):
    if len(v) != n or not all(math.isfinite(float(x)) for x in v):
        raise ValueError(f'invalid_{name}')
    return [float(x) for x in v]


def normalize_quaternion(q):
    q = _vec(q, 4, 'quaternion')
    norm = math.sqrt(sum(x*x for x in q))
    if norm < 1e-9: raise ValueError('zero_quaternion')
    return [x/norm for x in q]

class WorldModel:
    def __init__(self):
        self.transforms: Dict[str, Transform] = {}
        self.objects: Dict[str, WorldObject] = {}
        self.robot_state = RobotState()
        self.events: List[Dict[str, Any]] = []

    def upsert_transform(self, t: Transform):
        if not t.parent or not t.child or t.parent == t.child: raise ValueError('invalid_frame_relation')
        t.translation_xyz = _vec(t.translation_xyz,3,'translation')
        t.quaternion_xyzw = normalize_quaternion(t.quaternion_xyzw)
        t.confidence = max(0.0,min(1.0,float(t.confidence)))
        if t.observed_at_ms <= 0 or t.valid_for_ms < 0: raise ValueError('invalid_transform_time')
        self.transforms[t.child] = t
        self.events.append({'type':'transform_upsert','child':t.child,'at_ms':t.observed_at_ms,'source':t.source})
        return asdict(t)

    def upsert_object(self, o: WorldObject):
        if not o.object_id or not o.frame_id: raise ValueError('missing_object_identity_or_frame')
        o.position_xyz = _vec(o.position_xyz,3,'position')
        o.orientation_xyzw = normalize_quaternion(o.orientation_xyzw)
        o.dimensions_xyz = _vec(o.dimensions_xyz,3,'dimensions')
        if any(x < 0 for x in o.dimensions_xyz): raise ValueError('negative_dimension')
        o.confidence = max(0.0,min(1.0,float(o.confidence)))
        if o.observed_at_ms <= 0 or o.valid_for_ms < 0: raise ValueError('invalid_object_time')
        current = self.objects.get(o.object_id)
        # Newer observation wins; same timestamp uses higher confidence.
        if current is None or o.observed_at_ms > current.observed_at_ms or (o.observed_at_ms == current.observed_at_ms and o.confidence >= current.confidence):
            self.objects[o.object_id] = o
        self.events.append({'type':'object_observed','object_id':o.object_id,'at_ms':o.observed_at_ms,'source':o.source})
        return asdict(self.objects[o.object_id])

    def set_robot_state(self, state: RobotState):
        if state.mode != 'perception_only': raise ValueError('actuation_mode_not_allowed_in_world_model_service')
        if state.observed_at_ms <= 0: raise ValueError('invalid_robot_state_time')
        state.localization_confidence=max(0.0,min(1.0,float(state.localization_confidence)))
        self.robot_state=state
        self.events.append({'type':'robot_state','at_ms':state.observed_at_ms,'source':state.source})
        return asdict(state)

    def snapshot(self, now_ms: Optional[int]=None):
        now_ms = int(now_ms if now_ms is not None else time.time()*1000)
        objects=[]
        for o in self.objects.values():
            d=asdict(o); d['age_ms']=max(0,now_ms-o.observed_at_ms); d['stale']=d['age_ms']>o.valid_for_ms; objects.append(d)
        transforms=[]
        for t in self.transforms.values():
            d=asdict(t); d['age_ms']=max(0,now_ms-t.observed_at_ms); d['stale']=d['age_ms']>t.valid_for_ms; transforms.append(d)
        return {
            'time_ms':now_ms,
            'mode':'perception_only',
            'robot_state':asdict(self.robot_state),
            'transforms':transforms,
            'objects':objects,
            'fresh_objects':[x for x in objects if not x['stale']],
            'safety_boundary':'This service has no actuator command endpoint.'
        }
