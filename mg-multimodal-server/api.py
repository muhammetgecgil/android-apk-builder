from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Dict, Any, List
from multimodal_engine import make_event, fuse
from model_adapters import analyze_image, analyze_audio, analyze_ocr, status as adapter_status, AdapterUnavailable

app = FastAPI(title='MG-AI Multimodal Perception API', version='0.2')

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

class AnalyzeRequest(BaseModel):
    modality: str
    mime_type: str
    data_base64: str
    instruction: str = ''
    ocr: bool = False
    provenance: Dict[str, Any] = {}

@app.get('/health')
def health():
    return {'ok': True, 'modalities': ['image','audio','sensor','video','document'], 'adapters': adapter_status()}

@app.get('/v1/perception/adapters')
def adapters():
    return adapter_status()

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

@app.post('/v1/perception/analyze')
def analyze(req: AnalyzeRequest):
    if not req.data_base64:
        raise HTTPException(400, 'empty_media')
    try:
        if req.modality == 'image':
            result = analyze_image(req.data_base64, req.mime_type, req.instruction)
            if req.ocr:
                result['ocr_result'] = analyze_ocr(req.data_base64, req.mime_type)
        elif req.modality == 'audio':
            result = analyze_audio(req.data_base64, req.mime_type, req.instruction)
        else:
            raise HTTPException(400, 'unsupported_analysis_modality')
        return {
            'modality': req.modality,
            'analysis': result,
            'provenance': req.provenance,
            'verified': False,
            'verification_required': True
        }
    except AdapterUnavailable as e:
        raise HTTPException(503, str(e))
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(502, str(e))
