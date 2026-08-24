import json
import os
import urllib.request
from typing import List
from memory_engine import lexical_embedding

DEFAULT_MODEL = os.getenv('MG_EMBEDDING_MODEL', 'Qwen/Qwen3-Embedding-0.6B')
DEFAULT_ENDPOINT = os.getenv('MG_EMBEDDING_ENDPOINT', '').rstrip('/')
DEFAULT_API_KEY = os.getenv('MG_EMBEDDING_API_KEY', '')


def embed(text: str) -> List[float]:
    """Production path: OpenAI-compatible /v1/embeddings endpoint.
    Contract/CI fallback: deterministic lexical embedding when endpoint is unset.
    """
    if not DEFAULT_ENDPOINT:
        return lexical_embedding(text, dims=1024)

    url = DEFAULT_ENDPOINT
    if not url.endswith('/v1/embeddings'):
        url += '/v1/embeddings'
    payload = json.dumps({'model': DEFAULT_MODEL, 'input': text}).encode('utf-8')
    req = urllib.request.Request(url, data=payload, method='POST')
    req.add_header('Content-Type', 'application/json')
    if DEFAULT_API_KEY:
        req.add_header('Authorization', 'Bearer ' + DEFAULT_API_KEY)
    with urllib.request.urlopen(req, timeout=60) as response:
        body = json.loads(response.read().decode('utf-8'))
    data = body.get('data') or []
    if not data or not data[0].get('embedding'):
        raise RuntimeError('Embedding endpoint returned no vector')
    vector = [float(x) for x in data[0]['embedding']]
    if len(vector) != 1024:
        raise RuntimeError(f'Expected 1024 embedding dimensions, got {len(vector)}')
    return vector
