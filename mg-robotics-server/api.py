from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import Dict, Any, List
from world_model import WorldModel, Transform, WorldObject, RobotState

app = FastAPI(title='MG-AI Robot Perception & World Model API', version='0.1')
WORLD = WorldModel()

class TransformRequest(BaseModel):
    parent: str
    child: str
    translation_xyz: List[float]
    quaternion_xyzw: List[float]
    observed_at_ms: int
    valid_for_ms: int = 1000
    confidence: float = 1.0
    source: str = 'unknown'

class ObjectRequest(BaseModel):
    object_id: str
    label: str
    frame_id: str
    position_xyz: List[float]
    orientation_xyzw: List[float] = [0,0,0,1]
    dimensions_xyz: List[float] = [0,0,0]
    confidence: float = 0.5
    observed_at_ms: int
    valid_for_ms: int = 1000
    source: str = 'unknown'
    properties: Dict[str, Any] = {}

class RobotStateRequest(BaseModel):
    frame_id: str = 'base_link'
    observed_at_ms: int
    joint_positions: Dict[str, float] = {}
    joint_velocities: Dict[str, float] = {}
    localization_confidence: float = 0.0
    mode: str = 'perception_only'
    estop: str = 'unknown'
    protective_stop: bool = False
    source: str = 'unknown'

@app.get('/health')
def health():
    return {
        'ok': True,
        'mode': 'perception_only',
        'actuator_endpoints': 0,
        'required_frames': ['map','odom','base_link','sensor_frame'],
        'safety_boundary': 'No motor/servo/torque/velocity command API exists in this service.'
    }

@app.post('/v1/frames/transform')
def transform(req: TransformRequest):
    try: return WORLD.upsert_transform(Transform(**req.model_dump()))
    except Exception as e: raise HTTPException(400, str(e))

@app.post('/v1/world/object')
def world_object(req: ObjectRequest):
    try: return WORLD.upsert_object(WorldObject(**req.model_dump()))
    except Exception as e: raise HTTPException(400, str(e))

@app.post('/v1/robot/state')
def robot_state(req: RobotStateRequest):
    try: return WORLD.set_robot_state(RobotState(**req.model_dump()))
    except Exception as e: raise HTTPException(400, str(e))

@app.get('/v1/world/snapshot')
def snapshot(now_ms: int | None = None):
    return WORLD.snapshot(now_ms)

@app.get('/v1/events')
def events():
    return {'events': WORLD.events[-200:], 'count': len(WORLD.events)}
