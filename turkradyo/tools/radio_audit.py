#!/usr/bin/env python3
import csv, json, re, socket, ssl, sys, time
from concurrent.futures import ThreadPoolExecutor, as_completed
from urllib.parse import urlparse
from urllib.request import Request, urlopen

API='https://de1.api.radio-browser.info/json/stations/bycountrycodeexact/TR?hidebroken=true&order=clickcount&reverse=true&limit=3000'
BAD=re.compile(r'kurd|kürt|kurdish|kurdi|zaza|zazaki|sorani|kurman',re.I)
DINAMO=re.compile(r'\bdinamo(?:\.fm)?\b',re.I)
UA='Mozilla/5.0 TürkRadyoAudit/1.0'
TARGET=715
ROUNDS=3
READ_SECS=6.0
TIMEOUT=8.0
WORKERS=28


def fetch_catalog():
    req=Request(API,headers={'User-Agent':UA})
    with urlopen(req,timeout=20) as r:
        data=json.load(r)
    seen=set(); out=[]
    for s in data:
        txt=' '.join(str(s.get(k,'') or '') for k in ('name','language','languagecodes','tags','url','url_resolved'))
        if BAD.search(txt) or DINAMO.search(txt):
            continue
        url=s.get('url_resolved') or s.get('url')
        key=s.get('stationuuid') or url
        if not url or key in seen:
            continue
        seen.add(key)
        s['_url']=url
        out.append(s)
        if len(out)>=TARGET:
            break
    return out


def dns_tls(url):
    p=urlparse(url)
    host=p.hostname
    if not host: return False,False,0.0,'no_host'
    t=time.monotonic()
    try:
        socket.getaddrinfo(host,p.port or (443 if p.scheme=='https' else 80),type=socket.SOCK_STREAM)
        dns_ms=(time.monotonic()-t)*1000
    except Exception as e:
        return False,False,0.0,'dns:'+type(e).__name__
    if p.scheme!='https':
        return True,True,dns_ms,''
    try:
        ctx=ssl.create_default_context()
        with socket.create_connection((host,p.port or 443),timeout=TIMEOUT) as s:
            with ctx.wrap_socket(s,server_hostname=host):
                pass
        return True,True,dns_ms,''
    except Exception as e:
        return True,False,dns_ms,'tls:'+type(e).__name__


def probe_once(st):
    url=st['_url']
    dns_ok,tls_ok,dns_ms,err=dns_tls(url)
    if not dns_ok:
        return {'ok':False,'dns_ok':False,'tls_ok':False,'dns_ms':round(dns_ms,1),'error':err}
    t0=time.monotonic(); bytes_read=0; first_ms=None; status=None; ctype=''; icy=''; final_url='';
    try:
        req=Request(url,headers={'User-Agent':UA,'Icy-MetaData':'1','Accept':'*/*','Connection':'close'})
        with urlopen(req,timeout=TIMEOUT) as r:
            status=getattr(r,'status',200)
            final_url=r.geturl()
            ctype=(r.headers.get('Content-Type') or '').lower()
            icy=r.headers.get('icy-br') or r.headers.get('Ice-Audio-Info') or ''
            deadline=time.monotonic()+READ_SECS
            while time.monotonic()<deadline:
                chunk=r.read(16384)
                if not chunk: break
                if first_ms is None: first_ms=(time.monotonic()-t0)*1000
                bytes_read+=len(chunk)
                if bytes_read>=512000: break
        elapsed=max(.001,time.monotonic()-t0)
        bps=(bytes_read*8/elapsed)/1000
        audioish=('audio/' in ctype or 'mpeg' in ctype or 'aac' in ctype or 'ogg' in ctype or 'octet-stream' in ctype or bytes_read>65536)
        ok=(status and 200<=status<400 and bytes_read>=32768 and audioish)
        return {'ok':ok,'dns_ok':dns_ok,'tls_ok':tls_ok,'dns_ms':round(dns_ms,1),'status':status,'ctype':ctype[:80],
                'first_ms':round(first_ms or 0,1),'bytes':bytes_read,'observed_kbps':round(bps,1),'icy':str(icy)[:80],
                'redirected': bool(final_url and final_url!=url),'final_url':final_url[:300],'error':'' if ok else 'no_audio_or_short'}
    except Exception as e:
        return {'ok':False,'dns_ok':dns_ok,'tls_ok':tls_ok,'dns_ms':round(dns_ms,1),'status':status,'ctype':ctype,
                'first_ms':round(first_ms or 0,1),'bytes':bytes_read,'observed_kbps':0,'icy':str(icy)[:80],
                'redirected':False,'final_url':final_url[:300],'error':type(e).__name__+':'+str(e)[:120]}


def audit_one(st):
    results=[]
    for i in range(ROUNDS):
        results.append(probe_once(st))
        if i<ROUNDS-1: time.sleep(.25)
    oks=sum(1 for r in results if r['ok'])
    first=[r.get('first_ms',0) for r in results if r.get('first_ms',0)>0]
    kb=[r.get('observed_kbps',0) for r in results if r.get('observed_kbps',0)>0]
    dns=[r.get('dns_ms',0) for r in results if r.get('dns_ms',0)>0]
    if oks==3:
        verdict='KAL'
    elif oks==2:
        verdict='KAL'
    elif oks==1:
        verdict='INCELE'
    else:
        # 3/3 fail only; still distinguish hard failures from transient timeouts
        hard=all((not r.get('dns_ok',True)) or ('HTTP Error 404' in r.get('error','')) or ('HTTP Error 410' in r.get('error','')) or ('Name or service not known' in r.get('error','')) for r in results)
        verdict='CIKAR_ADAYI' if hard else 'INCELE'
    score=round((oks/3)*70 + max(0,20-(sum(first)/len(first)/500 if first else 20)) + (10 if kb and sum(kb)/len(kb)>=32 else 0))
    return {
        'name':st.get('name',''),'uuid':st.get('stationuuid',''),'url':st['_url'],'declared_bitrate':st.get('bitrate',0),
        'tags':st.get('tags',''),'rounds_ok':oks,'verdict':verdict,'score':max(0,min(100,score)),
        'avg_start_ms':round(sum(first)/len(first),1) if first else 0,
        'avg_kbps':round(sum(kb)/len(kb),1) if kb else 0,
        'avg_dns_ms':round(sum(dns)/len(dns),1) if dns else 0,
        'r1':results[0],'r2':results[1],'r3':results[2]
    }


def main():
    stations=fetch_catalog()
    print(f'catalog={len(stations)}',flush=True)
    rows=[]
    with ThreadPoolExecutor(max_workers=WORKERS) as ex:
        fut={ex.submit(audit_one,s):s for s in stations}
        for n,f in enumerate(as_completed(fut),1):
            try: rows.append(f.result())
            except Exception as e:
                s=fut[f]; rows.append({'name':s.get('name',''),'uuid':s.get('stationuuid',''),'url':s.get('_url',''),'verdict':'INCELE','score':0,'rounds_ok':0,'error':repr(e)})
            if n%25==0 or n==len(stations): print(f'{n}/{len(stations)}',flush=True)
    rows.sort(key=lambda x:( {'CIKAR_ADAYI':0,'INCELE':1,'KAL':2}.get(x.get('verdict'),9), x.get('score',0), x.get('name','').lower()))
    with open('radio-audit.json','w',encoding='utf-8') as f: json.dump(rows,f,ensure_ascii=False,indent=2)
    fields=['name','uuid','url','declared_bitrate','tags','rounds_ok','verdict','score','avg_start_ms','avg_kbps','avg_dns_ms']
    with open('radio-audit.csv','w',newline='',encoding='utf-8-sig') as f:
        w=csv.DictWriter(f,fieldnames=fields);w.writeheader();
        for r in rows:w.writerow({k:r.get(k,'') for k in fields})
    summary={k:sum(1 for r in rows if r.get('verdict')==k) for k in ('KAL','INCELE','CIKAR_ADAYI')}
    summary['total']=len(rows)
    with open('radio-audit-summary.json','w',encoding='utf-8') as f: json.dump(summary,f,ensure_ascii=False,indent=2)
    print(summary)

if __name__=='__main__': main()
