import asyncio
import os
from datetime import datetime, timezone
from urllib.parse import urlparse
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
import httpx

from research_engine import Source, research_packet
from search_adapters import build_provider
from page_fetcher import fetch_clean_text

app = FastAPI(title="MG-AI Research Engine", version="0.4.0")

class ResearchRequest(BaseModel):
    query: str = Field(min_length=2, max_length=400)
    count: int = Field(default=8, ge=3, le=20)
    language: str = Field(default="tr", min_length=2, max_length=10)
    synthesize: bool = True


def _authority(url: str) -> float:
    host = (urlparse(url).hostname or "").lower()
    if host.endswith(".gov") or ".gov." in host or host.endswith(".edu") or ".edu." in host:
        return 0.92
    if any(x in host for x in ["who.int", "nasa.gov", "nist.gov", "europa.eu", "un.org", "github.com"]):
        return 0.90
    return 0.58


def _freshness(published_at: str | None) -> float:
    if not published_at:
        return 0.50
    return 0.72


async def _enrich(hit):
    text = ""
    try:
        text = await fetch_clean_text(hit.url)
    except Exception:
        pass
    snippet = (text[:5000] if text else hit.snippet) or ""
    evidence_quality = 0.80 if len(text) > 1000 else (0.62 if snippet else 0.35)
    return Source(
        url=hit.url,
        title=hit.title,
        snippet=snippet[:5000],
        published_at=hit.published_at,
        authority=_authority(hit.url),
        relevance=0.72,
        freshness=_freshness(hit.published_at),
        independence=0.70,
        evidence_quality=evidence_quality,
    )


async def _synthesize(query: str, packet: dict) -> str | None:
    endpoint = os.getenv("MG_CORE_CHAT_ENDPOINT", "").strip()
    model = os.getenv("MG_CORE_MODEL", "Qwen/Qwen3.5-4B").strip()
    key = os.getenv("MG_CORE_API_KEY", "").strip()
    if not endpoint:
        return None
    evidence = []
    for i, s in enumerate(packet.get("sources", [])[:8], start=1):
        evidence.append(f"[{i}] {s.get('title','')}\nURL: {s.get('url','')}\nKanıt: {s.get('snippet','')[:1800]}")
    system = (
        "Sen MG-AI Research Synthesizer'sın. Yalnızca verilen kanıtlara dayan. "
        "Kanıt yoksa uydurma. Çelişki varsa açıkça belirt. Her maddi iddianın sonuna [1], [2] gibi kaynak numarası koy. "
        "Kaynakların söylemediği bir şeyi kesin gerçek gibi yazma."
    )
    user = "Soru: " + query + "\n\nKANITLAR\n" + "\n\n".join(evidence)
    headers = {"Content-Type": "application/json"}
    if key:
        headers["Authorization"] = "Bearer " + key
    body = {"model": model, "messages": [{"role": "system", "content": system}, {"role": "user", "content": user}], "temperature": 0.15, "stream": False}
    async with httpx.AsyncClient(timeout=120.0) as client:
        r = await client.post(endpoint, json=body, headers=headers)
        r.raise_for_status()
        data = r.json()
    choices = data.get("choices") or []
    if not choices:
        return None
    return ((choices[0].get("message") or {}).get("content") or "").strip() or None


@app.get("/health")
async def health():
    return {"ok": True, "service": "mg-research", "version": "0.4.0", "time": datetime.now(timezone.utc).isoformat()}


@app.post("/research")
async def research(req: ResearchRequest):
    try:
        provider = build_provider()
        hits = await provider.search(req.query, req.count, req.language)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"search_provider_failed: {type(e).__name__}: {e}")
    if not hits:
        raise HTTPException(status_code=404, detail="no_search_results")
    enriched = await asyncio.gather(*[_enrich(h) for h in hits], return_exceptions=True)
    sources = [x for x in enriched if isinstance(x, Source)]
    packet = research_packet(req.query, sources)
    packet["provider"] = getattr(provider, "name", "unknown")
    packet["answer"] = None
    if req.synthesize:
        try:
            packet["answer"] = await _synthesize(req.query, packet)
        except Exception as e:
            packet["synthesis_error"] = f"{type(e).__name__}: {e}"
    return packet
