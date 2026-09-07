#!/usr/bin/env python3
from pathlib import Path
from PIL import Image
import sys

TEXTURES = [
    'Map__2_Mix.png','Map__4_Mix.png','Map__4_Noise.png','Map__6_Noise.png',
    'Map__8_Mix.png','Map__8_Noise.png','Map__9_Mix.png','Map__10_Mix.png',
    'Map__15_Noise.png','Map__18_Mix.png','Map__20_Mix.png','Map__24_Mix.png',
    'Map__26_Mix.png','Map__90_Mix.png','Map__96_Mix.png'
]

def main():
    if len(sys.argv) != 3:
        raise SystemExit('usage: build_f22_atlas.py models_dir output.png')
    root = Path(sys.argv[1]); out = Path(sys.argv[2])
    tile = 512
    atlas = Image.new('RGBA', (tile*4, tile*4), (128,128,128,255))
    for i,name in enumerate(TEXTURES):
        p = root / name
        if not p.exists():
            print('missing texture', name)
            continue
        im = Image.open(p).convert('RGBA')
        im.thumbnail((tile,tile), Image.Resampling.LANCZOS)
        cell = Image.new('RGBA',(tile,tile),(128,128,128,255))
        x=(tile-im.width)//2; y=(tile-im.height)//2
        cell.paste(im,(x,y))
        atlas.paste(cell,((i%4)*tile,(i//4)*tile))
    out.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(out,optimize=True)
    print('atlas',out,atlas.size,out.stat().st_size)

if __name__=='__main__': main()
