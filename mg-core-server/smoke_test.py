#!/usr/bin/env python3
import argparse
import json
import sys
import urllib.request
import urllib.error


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--url', default='http://localhost:8000/v1')
    p.add_argument('--api-key', required=True)
    p.add_argument('--model', default='Qwen/Qwen3.5-4B')
    args = p.parse_args()

    base = args.url.rstrip('/')
    endpoint = base + '/chat/completions'
    payload = {
        'model': args.model,
        'messages': [
            {'role': 'system', 'content': 'You are MG-Core. Reply briefly and accurately.'},
            {'role': 'user', 'content': 'Return exactly: MG-CORE-OK'}
        ],
        'temperature': 0.0,
        'max_tokens': 32,
    }
    req = urllib.request.Request(
        endpoint,
        data=json.dumps(payload).encode('utf-8'),
        headers={
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + args.api_key,
        },
        method='POST',
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            body = json.loads(r.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        print('HTTP error:', e.code, e.read().decode('utf-8', 'replace'))
        return 2
    except Exception as e:
        print('Connection error:', repr(e))
        return 3

    try:
        text = body['choices'][0]['message']['content'].strip()
    except Exception:
        print('Invalid OpenAI-compatible response:', json.dumps(body, ensure_ascii=False))
        return 4

    print('model:', args.model)
    print('response:', text)
    if 'MG-CORE-OK' not in text:
        print('Smoke test failed: expected token not found')
        return 5
    print('MG-Core smoke test PASSED')
    return 0


if __name__ == '__main__':
    sys.exit(main())
