from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Dict, Any
from memory_engine import Chunk, chunk_text, retrieve, make_edge

app = FastAPI(title='MG-AI Memory & Knowledge API', version='0.1')

# Contract-level in-memory store. Production persistence uses PostgreSQL + pgvector schema.sql.
CHUNKS: List[Chunk] = []
EDGES: List[Dict[str, Any]] = []

class IngestRequest(BaseModel):
    document_id: str
    text: str
    memory_type: str = 'semantic'
    importance: float = 0.5
    confidence: float = 0.5
    provenance: Dict[str, Any] = {}

class QueryRequest(BaseModel):
    query: str
    top_k: int = 5

class EdgeRequest(BaseModel):
    src: str
    predicate: str
    dst: str
    confidence: float = 0.5
    provenance: Dict[str, Any] = {}

@app.get('/health')
def health():
    return {'ok': True, 'chunks': len(CHUNKS), 'edges': len(EDGES)}

@app.post('/v1/memory/ingest')
def ingest(req: IngestRequest):
    new_chunks = chunk_text(req.document_id, req.text)
    for c in new_chunks:
        c.memory_type = req.memory_type
        c.importance = max(0.0, min(1.0, req.importance))
        c.confidence = max(0.0, min(1.0, req.confidence))
        c.provenance = req.provenance
    CHUNKS.extend(new_chunks)
    return {'document_id': req.document_id, 'chunks_added': len(new_chunks), 'total_chunks': len(CHUNKS)}

@app.post('/v1/memory/query')
def query(req: QueryRequest):
    hits = retrieve(req.query, CHUNKS, top_k=max(1, min(20, req.top_k)))
    return {'query': req.query, 'hits': hits, 'count': len(hits)}

@app.post('/v1/knowledge/edge')
def edge(req: EdgeRequest):
    item = make_edge(req.src, req.predicate, req.dst, req.confidence, req.provenance)
    EDGES.append(item)
    return {'edge': item, 'edge_count': len(EDGES)}

@app.get('/v1/knowledge/edges')
def edges():
    return {'edges': EDGES, 'count': len(EDGES)}
