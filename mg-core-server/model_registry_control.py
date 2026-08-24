from __future__ import annotations
from typing import Dict, Any
import time

STATE: Dict[str, Any] = {
    'active_model': None,
    'previous_model': None,
    'staged_model': None,
    'history': []
}

def stage(model: Dict[str,Any]) -> Dict[str,Any]:
    if model.get('status') != 'promoted':
        raise ValueError('only_promoted_models_may_be_staged')
    STATE['staged_model'] = model.copy()
    STATE['history'].append({'event':'stage','model_id':model.get('model_id'),'ts':time.time()})
    return snapshot()

def activate(explicit_approval: bool=False) -> Dict[str,Any]:
    if not explicit_approval:
        raise ValueError('explicit_activation_approval_required')
    staged=STATE.get('staged_model')
    if not staged:
        raise ValueError('no_staged_model')
    STATE['previous_model']=STATE.get('active_model')
    STATE['active_model']=staged
    STATE['staged_model']=None
    STATE['history'].append({'event':'activate','model_id':staged.get('model_id'),'ts':time.time()})
    return snapshot()

def rollback(explicit_approval: bool=False) -> Dict[str,Any]:
    if not explicit_approval:
        raise ValueError('explicit_rollback_approval_required')
    previous=STATE.get('previous_model')
    if not previous:
        raise ValueError('no_previous_model')
    current=STATE.get('active_model')
    STATE['active_model']=previous
    STATE['previous_model']=current
    STATE['history'].append({'event':'rollback','model_id':previous.get('model_id'),'ts':time.time()})
    return snapshot()

def snapshot() -> Dict[str,Any]:
    return {
        'active_model':STATE.get('active_model'),
        'previous_model':STATE.get('previous_model'),
        'staged_model':STATE.get('staged_model'),
        'automatic_activation':False,
        'rollback_supported':True,
        'history':list(STATE.get('history',[]))
    }
