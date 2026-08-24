from dataclasses import dataclass, asdict
from enum import Enum
from typing import List, Dict, Any
import time, uuid

class TrainingState(str, Enum):
    DRAFT='draft'; DATA_READY='data_ready'; TRAINING='training'; EVALUATING='evaluating'; REJECTED='rejected'; PROMOTABLE='promotable'; PROMOTED='promoted'

@dataclass
class TrainingCandidate:
    id: str
    base_model: str
    dataset_id: str
    method: str = 'lora_sft'
    state: TrainingState = TrainingState.DRAFT
    safety_events: int = 0
    verified_samples: int = 0
    quality_score: float = 0.0
    regression_score: float = 0.0
    checkpoint: str = ''
    created_at: float = 0.0

class TrainingRegistry:
    def __init__(self): self.items: Dict[str, TrainingCandidate] = {}
    def create(self, base_model: str, dataset_id: str, method: str='lora_sft'):
        if method not in {'lora_sft','qlora_sft'}: raise ValueError('unsupported training method')
        c=TrainingCandidate(str(uuid.uuid4()),base_model,dataset_id,method,created_at=time.time())
        self.items[c.id]=c; return c
    def dataset_gate(self, cid: str, verified_samples: int, safety_events: int, quality_score: float):
        c=self.items[cid]; c.verified_samples=verified_samples; c.safety_events=safety_events; c.quality_score=quality_score
        if safety_events>0 or verified_samples<100 or quality_score<0.80:
            c.state=TrainingState.REJECTED
        else: c.state=TrainingState.DATA_READY
        return c
    def mark_training(self,cid:str):
        c=self.items[cid]
        if c.state!=TrainingState.DATA_READY: raise ValueError('dataset gate not passed')
        c.state=TrainingState.TRAINING; return c
    def register_checkpoint(self,cid:str,checkpoint:str):
        c=self.items[cid]
        if c.state!=TrainingState.TRAINING: raise ValueError('candidate is not training')
        c.checkpoint=checkpoint; c.state=TrainingState.EVALUATING; return c
    def evaluation_gate(self,cid:str,quality_score:float,regression_score:float,safety_pass:bool):
        c=self.items[cid]; c.quality_score=quality_score; c.regression_score=regression_score
        if (not safety_pass) or quality_score<0.85 or regression_score>0.02: c.state=TrainingState.REJECTED
        else: c.state=TrainingState.PROMOTABLE
        return c
    def promote(self,cid:str,explicit_approval:bool=False):
        c=self.items[cid]
        if c.state!=TrainingState.PROMOTABLE or not explicit_approval: raise ValueError('explicit approval and promotable state required')
        c.state=TrainingState.PROMOTED; return c
    def snapshot(self,cid:str): return asdict(self.items[cid])
