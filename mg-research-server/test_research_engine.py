from research_engine import Source, rank_sources, research_packet

def test_ranking_and_deduplication():
    a = Source('https://example.org/a','A','supports result',authority=0.9,relevance=0.9,freshness=0.8,independence=0.9,evidence_quality=0.9)
    b = Source('https://example.net/b','B','does not support result',authority=0.6,relevance=0.8,freshness=0.7,independence=0.8,evidence_quality=0.7)
    ranked = rank_sources([b,a,a])
    assert len(ranked) == 2
    assert ranked[0].url == a.url


def test_packet_provenance_and_conflict():
    sources = [
        Source('https://one.example/x','One','yes, supports claim',authority=0.8,relevance=0.9),
        Source('https://two.example/y','Two','no, does not support claim',authority=0.8,relevance=0.9),
    ]
    packet = research_packet('test query', sources)
    assert packet['provenance']['source_count'] == 2
    assert packet['provenance']['independent_domains'] == 2
    assert packet['contradictions']

if __name__ == '__main__':
    test_ranking_and_deduplication()
    test_packet_provenance_and_conflict()
    print('MG Research Engine contract tests passed')
