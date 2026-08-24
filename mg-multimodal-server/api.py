from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Dict, Any, List
from multimodal_engine import make_event, fuse

app = FastAPI(title='MG-AI Multimodal Perception API', version='0.1')

class EventRequest(BaseModel):
    modality: str
    source: str
    payload: Dict[str, Any]
    confidence: float = 0.5
    freshness_ms: int = 0
    calibration_state: str = 'unknown'
    provenance: Dict[str, Any] = {}

class FuseRequest(BaseModel):
    events: List[Dict[str, Any]]

@app.get('/health')
def health():
    return {'ok': True, 'modalities': ['image','audio','sensor','video','document']}

@app.post('/v1/perception/event')
def event(req: EventRequest):
    try:
        return make_event(req.modality, req.source, req.payload, req.confidence, req.freshness_ms,
                          req.calibration_state, req.provenance or {'source': req.source})
    except Exception as e:
        raise HTTPException(400, str(e))

@app.post('/v1/perception/fuse')
def fusion(req: FuseRequest):
    return fuse(req.events)
