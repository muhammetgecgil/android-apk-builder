from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from urllib.parse import urlparse
from typing import List, Dict, Any
import hashlib

@dataclass
class Source:
    url: str
    title: str
    snippet: str
    published_at: str | None = None
    authority: float = 0.5
    relevance: float = 0.5
    freshness: float = 0.5
    independence: float = 0.5
    evidence_quality: float = 0.5

    @property
    def score(self) -> float:
        return round((
            self.authority * 0.25 +
            self.relevance * 0.30 +
            self.freshness * 0.15 +
            self.independence * 0.15 +
            self.evidence_quality * 0.15
        ), 4)

    @property
    def domain(self) -> str:
        return urlparse(self.url).netloc.lower()

    def to_dict(self) -> Dict[str, Any]:
        d = asdict(self)
        d.update({"score": self.score, "domain": self.domain})
        return d


def source_fingerprint(source: Source) -> str:
    normalized = (source.domain + "|" + source.title.strip().lower() + "|" + source.snippet.strip().lower())
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()[:16]


def deduplicate(sources: List[Source]) -> List[Source]:
    seen = set()
    out = []
    for s in sources:
        fp = source_fingerprint(s)
        if fp not in seen:
            seen.add(fp)
            out.append(s)
    return out


def rank_sources(sources: List[Source]) -> List[Source]:
    return sorted(deduplicate(sources), key=lambda s: s.score, reverse=True)


def contradiction_signals(sources: List[Source]) -> List[Dict[str, Any]]:
    # Contract-level detector: flags materially different claims supplied by adapters.
    # Future semantic NLI verifier will replace this simple lexical heuristic.
    signals = []
    positives = [s for s in sources if any(x in s.snippet.lower() for x in ["yes", "increased", "supports", "is "])]
    negatives = [s for s in sources if any(x in s.snippet.lower() for x in ["no", "decreased", "does not", "is not"])]
    if positives and negatives:
        signals.append({
            "type": "possible_conflict",
            "positive_sources": [s.url for s in positives[:3]],
            "negative_sources": [s.url for s in negatives[:3]],
            "requires_verifier": True,
        })
    return signals


def research_packet(query: str, sources: List[Source]) -> Dict[str, Any]:
    ranked = rank_sources(sources)
    return {
        "query": query,
        "created_at": datetime.now(timezone.utc).isoformat(),
        "sources": [s.to_dict() for s in ranked],
        "contradictions": contradiction_signals(ranked),
        "provenance": {
            "source_count": len(ranked),
            "independent_domains": len(set(s.domain for s in ranked)),
            "engine": "mg-research-contract-v0.1"
        }
    }
