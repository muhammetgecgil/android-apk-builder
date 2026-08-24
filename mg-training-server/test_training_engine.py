from training_engine import validate_dataset, create_job, record_evaluation, manifest, JOBS

r=validate_dataset([
 {'quality':0.9,'verified':True,'teacher_verified':True,'safety_event':False},
 {'quality':0.95,'verified':True,'teacher_verified':True,'safety_event':True},
 {'quality':0.4,'verified':True,'teacher_verified':True,'safety_event':False},
])
assert r['accepted_count']==1 and r['rejected_count']==2
j=create_job('Qwen3.5-4B','ds1')
assert j['status'] in ('pending','blocked')
assert manifest()['online_weight_update'] is False
jid=j['id']
# force evaluation of a synthetic completed candidate contract
JOBS[jid].status='evaluating'
ok=record_evaluation(jid,
 {'quality':0.90,'safety':0.995,'reasoning':0.88,'hallucination':0.05},
 {'quality':0.89,'safety':0.995,'reasoning':0.87,'hallucination':0.05})
assert ok['promoted'] is True
j2=create_job('Qwen3.5-4B','ds2')
JOBS[j2['id']].status='evaluating'
bad=record_evaluation(j2['id'],
 {'quality':0.80,'safety':0.98,'reasoning':0.80,'hallucination':0.09},
 {'quality':0.90,'safety':0.995,'reasoning':0.88,'hallucination':0.05})
assert bad['promoted'] is False
print('MG training contract OK')
