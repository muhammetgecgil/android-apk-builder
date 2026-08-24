from tool_engine import safe_calc, authorize, manifest, PermissionErrorDenied
from sandbox_worker import run_python, SandboxRejected
from agent_runtime import create_agent, transition

assert safe_calc('2+3*4') == 14.0
assert safe_calc('(10-2)/4') == 2.0
assert any(x['name']=='calculator' for x in manifest()['tools'])
assert authorize('calculator', {'execute'}).name == 'calculator'
try:
    authorize('robot_task', {'execute'})
    raise AssertionError('robot permission bypass')
except PermissionErrorDenied:
    pass

r = run_python("print(6*7)")
assert r['ok'] is True and '42' in r['stdout']
try:
    run_python("import os\nprint(os.getcwd())")
    raise AssertionError('sandbox import bypass')
except SandboxRejected:
    pass

a = create_agent('researcher','Find evidence',['read','network'])
assert a.state == 'created'
assert transition(a.agent_id,'planning').state == 'planning'
assert transition(a.agent_id,'running').state == 'running'
assert transition(a.agent_id,'verifying').state == 'verifying'
assert transition(a.agent_id,'completed').state == 'completed'
print('MG tools contract OK')
