from dataclasses import dataclass, asdict
from typing import Dict, Any, List
from datetime import datetime, timezone

ALLOWED_MODALITIES = {'image','audio','sensor','video','document'}

@dataclass
class PerceptionEvent:
    modality: str
    source: str
    observed_at: str
    confidence: float
    freshness_ms: int
    calibration_state: str
    provenance: Dict[str, Any]
    payload: Dict[str, Any]

    def validate(self) -> Dict[str, Any]:
        if self.modality not in ALLOWED_MODALITIES:
            raise ValueError('unsupported_modality')
        if not self.source:
            raise ValueError('missing_source')
        if not (0.0 <= self.confidence <= 1.0):
            raise ValueError('invalid_confidence')
        if self.freshness_ms < 0:
            raise ValueError('invalid_freshness')
        if not self.calibration_state:
            raise ValueError('missing_calibration_state')
        if not self.provenance:
            raise ValueError('missing_provenance')
        return asdict(self)

def make_event(modality: str, source: str, payload: Dict[str, Any], confidence: float=0.5,
               freshness_ms: int=0, calibration_state: str='unknown', provenance: Dict[str, Any]|None=None):
    ev = PerceptionEvent(modality, source, datetime.now(timezone.utc).isoformat(), confidence,
                         freshness_ms, calibration_state, provenance or {'source': source}, payload)
    return ev.validate()

def fuse(events: List[Dict[str, Any]]) -> Dict[str, Any]:
    if not events:
        return {'events': [], 'modalities': [], 'fusion_confidence': 0.0, 'requires_verification': True}
    modalities = sorted(set(e['modality'] for e in events))
    conf = sum(float(e['confidence']) for e in events) / len(events)
    stale = any(int(e.get('freshness_ms',0)) > 5000 for e in events)
    return {
        'events': events,
        'modalities': modalities,
        'fusion_confidence': round(conf, 4),
        'requires_verification': stale or conf < 0.7,
        'robot_rule': 'Safety-relevant perception must be revalidated by deterministic robot safety/world-state logic.'
    }
