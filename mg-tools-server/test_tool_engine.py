from tool_engine import safe_calc, authorize, manifest, PermissionErrorDenied

assert safe_calc('2+3*4') == 14.0
assert safe_calc('(10-2)/4') == 2.0
assert any(x['name']=='calculator' for x in manifest()['tools'])
assert authorize('calculator', {'execute'}).name == 'calculator'
try:
    authorize('robot_task', {'execute'})
    raise AssertionError('robot permission bypass')
except PermissionErrorDenied:
    pass
print('MG tools contract OK')
