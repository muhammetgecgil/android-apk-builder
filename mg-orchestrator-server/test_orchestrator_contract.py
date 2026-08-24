import orchestrator

assert len(orchestrator.SERVICES) >= 10
assert 'mg_core' in orchestrator.SERVICES
assert 'training' in orchestrator.SERVICES
assert 'robot_safety' in orchestrator.SERVICES
# Safety invariants are reported by the orchestrator and never inferred from service reachability.
print('orchestrator service registry OK')
