from learning_engine import Experience, score_experience, compare_teachers, promotion_gate, RULES

exp=Experience('e1','task','answer',0.9,0.95,0.9)
c=score_experience(exp)
assert c.status=='eligible'
q=Experience('e2','task','answer',1,1,1,safety_event=True)
assert score_experience(q).status=='quarantine'
assert compare_teachers([{'teacher_id':'a','text':'x','verifier_score':0.5}])['status']=='reject'
assert compare_teachers([{'teacher_id':'b','text':'y','verifier_score':0.9}])['status']=='candidate'
assert promotion_gate({'quality':.91,'safety':.995,'calibration':.9},{'quality':.9,'safety':.99,'calibration':.89})['decision']=='promote_candidate'
assert promotion_gate({'quality':.89,'safety':1,'calibration':.9},{'quality':.9,'safety':.99,'calibration':.89})['decision']=='reject'
assert RULES['online_weight_updates'] is False
assert RULES['robot_runtime_weight_updates'] is False
print('MG learning contract OK')
