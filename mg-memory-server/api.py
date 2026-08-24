from fastapi import FastAPI
from pydantic import BaseModel, Field
from typing import List, Dict, Any
from memory_engine import Chunk, chunk_text, retrieve, make_edge
from embedding_provider import embed

app = FastAPI(title='MG-AI Memory & Knowledge API', version='0.2')

# Contract-level store. Production persistence is PostgreSQL + pgvector using schema.sql.
CHUNKS: List[Chunk] = []
EDGES: List[Dict[str, Any]] = []

class IngestRequest(BaseModel):
    document_id: str
    text: str
    memory_type: str = 'semantic'
    importance: float = Field(default=0.5, ge=0.0, le=1.0)
    confidence: float = Field(default=0.5, ge=0.0, le=1.0)
    provenance: Dict[str, Any] = Field(default_factory=dict)

class QueryRequest(BaseModel):
    query: str
    top_k: int = Field(default=5, ge=1, le=20)

class EdgeRequest(BaseModel):
    src: str
    predicate: str
    dst: str
    confidence: float = Field(default=0.5, ge=0.0, le=1.0)
    provenance: Dict[str, Any] = Field(default_factory=dict)

@app.get('/health')
def health():
    return {
        'ok': True,
        'chunks': len(CHUNKS),
        'edges': len(EDGES),
        'embedding_dimensions': 1024,
        'embedding_provider': 'openai-compatible-or-contract-fallback'
    }

@app.post('/v1/memory/ingest')
def ingest(req: IngestRequest):
    new_chunks = chunk_text(req.document_id, req.text)
    for c in new_chunks:
        c.memory_type = req.memory_type
        c.importance = req.importance
        c.confidence = req.confidence
        c.provenance = req.provenance
    CHUNKS.extend(new_chunks)
    return {'document_id': req.document_id, 'chunks_added': len(new_chunks), 'total_chunks': len(CHUNKS)}

@app.post('/v1/memory/query')
def query(req: QueryRequest):
    hits = retrieve(req.query, CHUNKS, top_k=req.top_k, embed_fn=embed)
    return {'query': req.query, 'hits': hits, 'count': len(hits)}

@app.post('/v1/knowledge/edge')
def edge(req: EdgeRequest):
    item = make_edge(req.src, req.predicate, req.dst, req.confidence, req.provenance)
    EDGES.append(item)
    return {'edge': item, 'edge_count': len(EDGES)}

@app.get('/v1/knowledge/edges')
def edges():
    return {'edges': EDGES, 'count': len(EDGES)}
