import ast
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Dict, Any

FORBIDDEN_NAMES = {
    'os','sys','subprocess','socket','requests','urllib','pathlib','shutil','ctypes','multiprocessing','threading','builtins','importlib'
}
FORBIDDEN_NODES = (ast.Import, ast.ImportFrom, ast.With, ast.AsyncWith, ast.Try, ast.Raise, ast.Global, ast.Nonlocal, ast.Lambda)

class SandboxRejected(Exception):
    pass

def validate_python(code: str) -> None:
    if len(code.encode('utf-8')) > 16_000:
        raise SandboxRejected('code_too_large')
    tree = ast.parse(code, mode='exec')
    for node in ast.walk(tree):
        if isinstance(node, FORBIDDEN_NODES):
            raise SandboxRejected(f'forbidden_syntax:{type(node).__name__}')
        if isinstance(node, ast.Name) and node.id in FORBIDDEN_NAMES:
            raise SandboxRejected(f'forbidden_name:{node.id}')
        if isinstance(node, ast.Attribute) and isinstance(node.value, ast.Name) and node.value.id in FORBIDDEN_NAMES:
            raise SandboxRejected(f'forbidden_attribute:{node.value.id}')

def run_python(code: str, timeout_s: float = 2.0) -> Dict[str, Any]:
    validate_python(code)
    with tempfile.TemporaryDirectory(prefix='mgai-sbx-') as td:
        p = Path(td) / 'main.py'
        p.write_text(code, encoding='utf-8')
        try:
            cp = subprocess.run(
                [sys.executable, '-I', '-S', str(p)],
                cwd=td,
                text=True,
                capture_output=True,
                timeout=max(0.2, min(timeout_s, 5.0)),
                env={'PYTHONIOENCODING':'utf-8'}
            )
        except subprocess.TimeoutExpired:
            return {'ok':False,'status':'timeout','stdout':'','stderr':'execution_timeout'}
        stdout = cp.stdout[-8000:]
        stderr = cp.stderr[-8000:]
        return {
            'ok': cp.returncode == 0,
            'status': 'success' if cp.returncode == 0 else 'error',
            'returncode': cp.returncode,
            'stdout': stdout,
            'stderr': stderr,
            'network': 'not_provided_by_contract',
            'filesystem': 'temporary_workdir_only'
        }
