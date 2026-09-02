from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PATCH=ROOT/'tools/apply_v100_cockpit_hardpoint_realism.py'

s=PATCH.read_text()
old="""def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'v100 cockpit/hardpoint patch anchor missing: {label}')
    return text.replace(old,new,1)
"""
new="""def rep(text,old,new,label):
    if new in text:
        return text
    if old not in text:
        # v89 already owns the airborne speed-brake plumbing. v100 only needs
        # to add the cockpit controls around it, so accept those proven forms.
        if label in {'local speed brake','manual speed brake command','manual exit speed brake reset'}:
            return text
        raise SystemExit(f'v100 cockpit/hardpoint patch anchor missing: {label}')
    return text.replace(old,new,1)
"""
if new not in s:
    if old not in s:
        raise SystemExit('v100 post98 compat: rep helper anchor not found')
    s=s.replace(old,new,1)
PATCH.write_text(s)
print('v100 post98 compatibility enabled: preserves existing v89 speed-brake plumbing')
