import json, os, urllib.request, urllib.error
from typing import Dict, Any

class AdapterUnavailable(RuntimeError): pass

VISION_MODEL = os.getenv('MG_VISION_MODEL', 'Qwen/Qwen3-VL-4B-Instruct')
AUDIO_MODEL = os.getenv('MG_AUDIO_MODEL', 'Qwen/Qwen2-Audio-7B-Instruct')
OCR_MODEL = os.getenv('MG_OCR_MODEL', VISION_MODEL)
VISION_ENDPOINT = os.getenv('MG_VISION_ENDPOINT', '').rstrip('/')
AUDIO_ENDPOINT = os.getenv('MG_AUDIO_ENDPOINT', '').rstrip('/')
OCR_ENDPOINT = os.getenv('MG_OCR_ENDPOINT', VISION_ENDPOINT).rstrip('/')


def _post_json(endpoint: str, body: Dict[str, Any], timeout: int = 120) -> Dict[str, Any]:
    if not endpoint:
        raise AdapterUnavailable('adapter_unconfigured')
    req = urllib.request.Request(endpoint, data=json.dumps(body).encode('utf-8'),
                                 headers={'Content-Type':'application/json'}, method='POST')
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        detail = e.read().decode('utf-8', errors='replace')[:2000]
        raise RuntimeError(f'adapter_http_{e.code}:{detail}')


def analyze_image(base64_data: str, mime_type: str, prompt: str) -> Dict[str, Any]:
    # Contract: external/self-hosted adapter accepts model + data URI and returns JSON analysis.
    body = {
        'model': VISION_MODEL,
        'input': {'mime_type': mime_type, 'data_base64': base64_data},
        'instruction': prompt or 'Describe, identify text/objects, spatial relations, uncertainty and safety-relevant observations.',
        'output_contract': {'text':'string','confidence':'0..1','objects':'array','ocr':'array'}
    }
    out = _post_json(VISION_ENDPOINT, body)
    return {'adapter':'vision','model':VISION_MODEL,'result':out}


def analyze_audio(base64_data: str, mime_type: str, prompt: str) -> Dict[str, Any]:
    body = {
        'model': AUDIO_MODEL,
        'input': {'mime_type': mime_type, 'data_base64': base64_data},
        'instruction': prompt or 'Transcribe speech when present and analyze salient acoustic events with uncertainty.',
        'output_contract': {'transcript':'string','analysis':'string','confidence':'0..1','events':'array'}
    }
    out = _post_json(AUDIO_ENDPOINT, body)
    return {'adapter':'audio','model':AUDIO_MODEL,'result':out}


def analyze_ocr(base64_data: str, mime_type: str) -> Dict[str, Any]:
    body = {
        'model': OCR_MODEL,
        'input': {'mime_type': mime_type, 'data_base64': base64_data},
        'instruction': 'Extract visible text while preserving reading order. Do not invent unreadable text.',
        'output_contract': {'text':'string','blocks':'array','confidence':'0..1'}
    }
    out = _post_json(OCR_ENDPOINT, body)
    return {'adapter':'ocr','model':OCR_MODEL,'result':out}


def status() -> Dict[str, Any]:
    return {
        'vision': {'configured': bool(VISION_ENDPOINT), 'model': VISION_MODEL},
        'audio': {'configured': bool(AUDIO_ENDPOINT), 'model': AUDIO_MODEL},
        'ocr': {'configured': bool(OCR_ENDPOINT), 'model': OCR_MODEL},
        'rule': 'No configured adapter means no fabricated model analysis.'
    }
