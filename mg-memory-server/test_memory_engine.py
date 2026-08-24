from memory_engine import chunk_text, retrieve, make_edge

text = ('MG-AI stores verified engineering knowledge with provenance. ' * 40) + 'Robot actions pass through a deterministic safety supervisor.'
chunks = chunk_text('doc-1', text, max_chars=400, overlap=60)
assert len(chunks) >= 3
assert all(c.content for c in chunks)

hits = retrieve('deterministic robot safety supervisor', chunks, top_k=3)
assert hits
assert len(hits) <= 3
assert hits[0]['retrieval_score'] >= hits[-1]['retrieval_score']

edge = make_edge('robot', 'requires', 'safety-supervisor', 0.99, {'source':'MG-EMB-003'})
assert edge['predicate'] == 'requires'
assert edge['confidence'] == 0.99
print('memory contract ok')
