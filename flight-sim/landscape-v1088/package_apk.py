"""Package the verified scenery DEX files into a copy of the v108.7 APK.
The original APK is never modified. Native libraries remain stored and 16 KiB aligned.
"""
from pathlib import Path
import struct, zipfile, hashlib, json, sys

ROOT = Path(__file__).resolve().parent
WORK = ROOT.parent / 'work'
EXPECTED = '16e678c3bea41b359e99ea471cb97a84a72d110464793c6d7b5b2994d04e600f'

def rewrite_manifest(data):
    data = bytearray(data); pos = 8; strings = None
    while pos < len(data):
        typ, header, size = struct.unpack_from('<HHI', data, pos)
        if typ == 1:
            count, styles, flags, string_start, style_start = struct.unpack_from('<IIIII',data,pos+8)
            assert not styles and not style_start, 'Unexpected manifest styles'
            strings = []
            for i in range(count):
                q = pos + string_start + struct.unpack_from('<I',data,pos+header+i*4)[0]
                if flags & 256:
                    n=data[q];q+=1
                    if n&128:q+=1
                    n=data[q];q+=1
                    if n&128:n=((n&127)<<8)|data[q];q+=1
                    value=bytes(data[q:q+n]).decode('utf-8')
                else:
                    n=struct.unpack_from('<H',data,q)[0];q+=2
                    assert n<32768
                    value=bytes(data[q:q+n*2]).decode('utf-16-le')
                strings.append(value)
            changes = {
                'com.mg.fixturecockpitsim.v17':'com.mg.fixturecockpitsim.scenery1088',
                '26.26-avm35.0-cinematic-turkiye-3d-world-debug':'108.8-realistic-landscape-test',
                'Aircraft Simulator 3D':'Aircraft 3D Manzara',
            }
            for old,new in changes.items():
                assert strings.count(old)==1, f'Manifest anchor: {old}'
                strings[strings.index(old)]=new
            def len8(n):return bytes([n]) if n<128 else bytes([(n>>8)|128,n&255])
            offsets=[];body=bytearray()
            for s in strings:
                offsets.append(len(body));raw=s.encode('utf-8' if flags&256 else 'utf-16-le')
                if flags&256:body+=len8(len(s.encode('utf-16-le'))//2)+len8(len(raw))+raw+b'\0'
                else:body+=struct.pack('<H',len(raw)//2)+raw+b'\0\0'
            while len(body)%4:body+=b'\0'
            start=28+4*count
            chunk=struct.pack('<HHIIIIII',1,28,start+len(body),count,0,flags&~1,start,0)
            chunk+=struct.pack('<'+'I'*count,*offsets)+body
            data[pos:pos+size]=chunk;size=len(chunk)
        elif typ == 0x102:
            # XML start-element extension begins after the 16-byte node header.
            name=struct.unpack_from('<I',data,pos+20)[0]
            attr_start,attr_size,attr_count=struct.unpack_from('<HHH',data,pos+24)
            if strings[name]=='manifest':
                for i in range(attr_count):
                    a=pos+16+attr_start+i*attr_size
                    attr_name=struct.unpack_from('<I',data,a+4)[0]
                    if strings[attr_name]=='versionCode':
                        assert data[a+15]==0x10
                        struct.pack_into('<I',data,a+16,10808)
        pos+=size
    struct.pack_into('<I',data,4,len(data));return bytes(data)

def build(original,destination):
    assert hashlib.sha256(original.read_bytes()).hexdigest()==EXPECTED, 'APK baseline mismatch'
    with zipfile.ZipFile(original) as src, zipfile.ZipFile(destination,'w') as dst:
        for entry in src.infolist():
            if entry.filename.startswith('META-INF/') and entry.filename.upper().endswith(('.RSA','.SF','.DSA','.EC','MANIFEST.MF')):continue
            data=src.read(entry.filename)
            if entry.filename=='classes3.dex':data=(WORK/'patched-classes3.dex').read_bytes()
            elif entry.filename=='classes4.dex':data=(WORK/'patched-classes4.dex').read_bytes()
            elif entry.filename=='AndroidManifest.xml':data=rewrite_manifest(data)
            out=zipfile.ZipInfo(entry.filename,entry.date_time);out.compress_type=entry.compress_type
            out.external_attr=entry.external_attr;out.create_system=entry.create_system
            if out.compress_type==zipfile.ZIP_STORED:
                alignment=16384 if entry.filename.endswith('.so') else 4
                padding=(-(dst.fp.tell()+30+len(out.filename.encode('utf-8'))+6))%alignment
                out.extra=struct.pack('<HHH',0xd935,2+padding,alignment)+b'\0'*padding
            dst.writestr(out,data)
    with zipfile.ZipFile(destination) as z:assert z.testzip() is None
    print(json.dumps({'unsigned_apk':str(destination),'bytes':destination.stat().st_size}))

if __name__=='__main__':build(Path(sys.argv[1]),Path(sys.argv[2]))
