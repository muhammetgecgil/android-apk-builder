from dataclasses import dataclass, asdict
from typing import List, Dict, Any, Callable
import hashlib
import math
import re

@dataclass
class Chunk:
    document_id: str
    index: int
    content: str
    memory_type: str = 'semantic'
    importance: float = 0.5
    confidence: float = 0.5
    provenance: Dict[str, Any] | None = None

    def to_dict(self):
        d = asdict(self)
        d['content_hash'] = hashlib.sha256(self.content.encode('utf-8')).hexdigest()
        return d


def normalize_text(text: str) -> str:
    return re.sub(r'\s+', ' ', text or '').strip()


def chunk_text(document_id: str, text: str, max_chars: int = 1200, overlap: int = 180) -> List[Chunk]:
    text = normalize_text(text)
    if not text:
        return []
    out: List[Chunk] = []
    start = 0
    i = 0
    while start < len(text):
        end = min(len(text), start + max_chars)
        if end < len(text):
            split = text.rfind('. ', start, end)
            if split > start + max_chars // 2:
                end = split + 1
        content = text[start:end].strip()
        if content:
            out.append(Chunk(document_id=document_id, index=i, content=content))
            i += 1
        if end >= len(text):
            break
        start = max(start + 1, end - overlap)
    return out


def lexical_embedding(text: str, dims: int = 1024) -> List[float]:
    # Deterministic contract embedding used for tests/fallback only.
    vec = [0.0] * dims
    for tok in re.findall(r"[\wçğıöşüÇĞİÖŞÜ]+", text.lower()):
        h = int(hashlib.sha256(tok.encode('utf-8')).hexdigest()[:16], 16)
        vec[h % dims] += 1.0
    norm = math.sqrt(sum(x*x for x in vec)) or 1.0
    return [x / norm for x in vec]


def cosine(a: List[float], b: List[float]) -> float:
    return sum(x*y for x, y in zip(a, b))


def retrieve(query: str, chunks: List[Chunk], top_k: int = 5, embed_fn: Callable[[str], List[float]] | None = None) -> List[Dict[str, Any]]:
    embed_fn = embed_fn or lexical_embedding
    q = embed_fn(query)
    scored = []
    for c in chunks:
        sim = cosine(q, embed_fn(c.content))
        score = sim * 0.75 + c.importance * 0.15 + c.confidence * 0.10
        item = c.to_dict()
        item['retrieval_score'] = round(score, 6)
        scored.append(item)
    return sorted(scored, key=lambda x: x['retrieval_score'], reverse=True)[:top_k]


def make_edge(src: str, predicate: str, dst: str, confidence: float = 0.5, provenance: Dict[str, Any] | None = None) -> Dict[str, Any]:
    return {
        'src': src,
        'predicate': predicate,
        'dst': dst,
        'confidence': confidence,
        'provenance': provenance or {},
    }
