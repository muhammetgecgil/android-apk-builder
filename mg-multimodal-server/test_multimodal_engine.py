import os
from multimodal_engine import make_event, fuse
from model_adapters import status, analyze_image, AdapterUnavailable

a = make_event('image','android-camera',{'width':100,'height':80},0.9,100,'calibrated',{'source':'camera'})
b = make_event('sensor','imu',{'ax':0.1},0.8,50,'calibrated',{'source':'imu'})
r = fuse([a,b])
assert r['modalities'] == ['image','sensor']
assert r['fusion_confidence'] > 0.8
assert r['requires_verification'] is False
try:
    make_event('unknown','x',{},0.5,0,'unknown',{'source':'x'})
    raise AssertionError('unsupported modality accepted')
except ValueError:
    pass

s = status()
assert 'vision' in s and 'audio' in s and 'ocr' in s
assert s['vision']['model']
# No endpoint in CI: adapter must refuse rather than fabricate an analysis.
if not s['vision']['configured']:
    try:
        analyze_image('AA==','image/jpeg','test')
        raise AssertionError('unconfigured vision adapter fabricated a result')
    except AdapterUnavailable:
        pass
print('MG multimodal contract OK')
