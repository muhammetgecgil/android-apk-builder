from model_registry_control import stage, activate, rollback, snapshot

m1={'model_id':'mg-001','status':'promoted','checkpoint':'ckpt1'}
m2={'model_id':'mg-002','status':'promoted','checkpoint':'ckpt2'}
stage(m1)
try:
    activate(False)
    raise AssertionError('activation approval gate failed')
except ValueError: pass
activate(True)
assert snapshot()['active_model']['model_id']=='mg-001'
stage(m2); activate(True)
assert snapshot()['active_model']['model_id']=='mg-002'
try:
    rollback(False)
    raise AssertionError('rollback approval gate failed')
except ValueError: pass
rollback(True)
assert snapshot()['active_model']['model_id']=='mg-001'
print('model registry control OK')
