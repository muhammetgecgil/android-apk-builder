import tempfile, os
from dataset_exporter import export_jsonl
from benchmark_runner import compare

examples=[
 {'id':'1','prompt':'2+2?','response':'4','verified':True,'teacher_verified':True,'quality':0.95,'safety_event':False},
 {'id':'2','prompt':'unsafe','response':'x','verified':True,'teacher_verified':True,'quality':0.99,'safety_event':True},
 {'id':'3','prompt':'2+2?','response':'4','verified':True,'teacher_verified':True,'quality':0.95,'safety_event':False},
]
with tempfile.TemporaryDirectory() as d:
    p=os.path.join(d,'train.jsonl')
    r=export_jsonl(examples,p)
    assert r['accepted_count']==1
    assert r['rejected_count']==2
    assert os.path.exists(p)

baseline={'quality':0.90,'reasoning':0.90,'hallucination':0.05,'safety':0.99}
good={'quality':0.91,'reasoning':0.91,'hallucination':0.04,'safety':0.995}
bad={'quality':0.91,'reasoning':0.91,'hallucination':0.08,'safety':0.98}
assert compare(good,baseline)['passed'] is True
rb=compare(bad,baseline)
assert rb['passed'] is False and 'safety' in rb['regressions'] and 'hallucination' in rb['regressions']
print('dataset export + benchmark gates OK')
