from safety_supervisor import SafetySupervisor,SafetyConfig,JointLimit,SafetyState,MotionProposal

sup=SafetySupervisor()
p=MotionProposal('p0',{'j1':0},{'j1':0},[0,0,0],0.1,1)
s=SafetyState(False,False,1.0,10,1.0)
assert sup.evaluate(p,s)['decision']=='REJECT'

cfg=SafetyConfig('test-v1',{'j1':JointLimit(-1,1,0.5)},0.25,20,0.5,200,0.8,[-1,-1,0],[1,1,2],[{'min':[0.4,0.4,0.4],'max':[0.6,0.6,0.6]}])
sup.configure(cfg)
assert sup.evaluate(MotionProposal('ok',{'j1':0},{'j1':0.1},[0,0,1],0.1,5),SafetyState(False,False,1.0,50,0.95))['decision']=='ALLOW'
assert sup.evaluate(MotionProposal('estop',{'j1':0},{'j1':0},[0,0,1],0.1,5),SafetyState(True,False,1.0,50,0.95))['decision']=='REJECT'
assert 'human_too_close' in sup.evaluate(MotionProposal('human',{'j1':0},{'j1':0},[0,0,1],0.1,5),SafetyState(False,False,0.1,50,0.95))['reasons']
assert 'joint_position_limit:j1' in sup.evaluate(MotionProposal('joint',{'j1':2},{'j1':0},[0,0,1],0.1,5),SafetyState(False,False,1.0,50,0.95))['reasons']
assert 'forbidden_zone' in sup.evaluate(MotionProposal('zone',{'j1':0},{'j1':0},[0.5,0.5,0.5],0.1,5),SafetyState(False,False,1.0,50,0.95))['reasons']
assert sup.evaluate(MotionProposal('stale',{'j1':0},{'j1':0},[0,0,1],0.1,5),SafetyState(False,False,1.0,999,0.95))['decision']=='HOLD'
r=sup.evaluate(MotionProposal('speed',{'j1':0},{'j1':0},[0,0,1],1.0,5),SafetyState(False,False,1.0,50,0.95))
assert r['decision']=='REJECT' and r['actuator_command'] is None
print('MG robot safety contract OK')
