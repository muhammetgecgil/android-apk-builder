from world_model import WorldModel, Transform, WorldObject, RobotState

w=WorldModel()
w.upsert_transform(Transform('map','base_link',[1,2,0],[0,0,0,1],1000,500,0.9,'test'))
w.upsert_object(WorldObject('obj-1','box','map',[2,0,0],[0,0,0,1],[0.2,0.3,0.4],0.8,1000,500,'camera'))
w.set_robot_state(RobotState(frame_id='base_link',observed_at_ms=1000,localization_confidence=0.95,mode='perception_only',source='localizer'))
s=w.snapshot(1200)
assert s['mode']=='perception_only'
assert len(s['fresh_objects'])==1
assert s['objects'][0]['stale'] is False
assert s['transforms'][0]['stale'] is False
s2=w.snapshot(1700)
assert len(s2['fresh_objects'])==0
assert s2['objects'][0]['stale'] is True
try:
    w.set_robot_state(RobotState(observed_at_ms=2000,mode='velocity_control'))
    raise AssertionError('actuation mode accepted')
except ValueError:
    pass
try:
    w.upsert_transform(Transform('map','map',[0,0,0],[0,0,0,1],2000))
    raise AssertionError('self frame transform accepted')
except ValueError:
    pass
print('MG robot world model contract OK')
