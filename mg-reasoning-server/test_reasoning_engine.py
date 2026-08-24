from reasoning_engine import decompose_task, contract_reason


def main():
    plan = decompose_task('Bir problemi çöz')
    assert len(plan) >= 5
    result = contract_reason('task','candidate','critique','revision',2)
    assert result['verification']['checks']['non_empty'] is True
    assert result['verification']['checks']['has_critique'] is True
    assert result['verification']['checks']['has_evidence'] is True
    assert result['verification']['status'] == 'verified'
    print('MG reasoning contract OK')

if __name__ == '__main__':
    main()
