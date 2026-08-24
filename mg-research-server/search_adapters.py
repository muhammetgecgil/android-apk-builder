import os
from dataclasses import dataclass
from typing import List, Dict, Any
from urllib.parse import urljoin
import httpx

@dataclass
class SearchHit:
    url: str
    title: str
    snippet: str
    published_at: str | None = None
    provider: str = "unknown"

class SearchProvider:
    name = "base"
    async def search(self, query: str, count: int = 8, language: str = "tr") -> List[SearchHit]:
        raise NotImplementedError

class SearxNGProvider(SearchProvider):
    name = "searxng"
    def __init__(self, base_url: str):
        self.base_url = base_url.rstrip("/")

    async def search(self, query: str, count: int = 8, language: str = "tr") -> List[SearchHit]:
        params = {"q": query, "format": "json", "language": language, "safesearch": 1}
        async with httpx.AsyncClient(timeout=20.0, follow_redirects=True) as client:
            r = await client.get(self.base_url + "/search", params=params)
            r.raise_for_status()
            data = r.json()
        out: List[SearchHit] = []
        for item in data.get("results", [])[:count]:
            url = str(item.get("url") or "").strip()
            if not url:
                continue
            out.append(SearchHit(
                url=url,
                title=str(item.get("title") or url).strip(),
                snippet=str(item.get("content") or "").strip(),
                published_at=item.get("publishedDate") or item.get("published_at"),
                provider=self.name,
            ))
        return out

class BraveProvider(SearchProvider):
    name = "brave"
    def __init__(self, api_key: str):
        self.api_key = api_key

    async def search(self, query: str, count: int = 8, language: str = "tr") -> List[SearchHit]:
        headers = {"X-Subscription-Token": self.api_key, "Accept": "application/json"}
        params = {"q": query, "count": max(1, min(count, 20)), "search_lang": language, "safesearch": "moderate"}
        async with httpx.AsyncClient(timeout=20.0, follow_redirects=True) as client:
            r = await client.get("https://api.search.brave.com/res/v1/web/search", headers=headers, params=params)
            r.raise_for_status()
            data = r.json()
        out: List[SearchHit] = []
        for item in ((data.get("web") or {}).get("results") or [])[:count]:
            url = str(item.get("url") or "").strip()
            if not url:
                continue
            out.append(SearchHit(
                url=url,
                title=str(item.get("title") or url).strip(),
                snippet=str(item.get("description") or "").strip(),
                published_at=item.get("age") or item.get("page_age"),
                provider=self.name,
            ))
        return out

def build_provider() -> SearchProvider:
    provider = os.getenv("MG_RESEARCH_PROVIDER", "searxng").lower().strip()
    if provider == "brave":
        key = os.getenv("BRAVE_SEARCH_API_KEY", "").strip()
        if not key:
            raise RuntimeError("BRAVE_SEARCH_API_KEY is required for Brave provider")
        return BraveProvider(key)
    base = os.getenv("SEARXNG_URL", "http://searxng:8080").strip()
    return SearxNGProvider(base)
