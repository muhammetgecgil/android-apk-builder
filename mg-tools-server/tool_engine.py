from dataclasses import dataclass, asdict
from typing import Callable, Dict, Any
import ast, math, operator

@dataclass
class ToolSpec:
    name: str
    risk: str
    permission: str
    description: str

TOOLS = {
    'calculator': ToolSpec('calculator','low','execute','Safe arithmetic calculator'),
    'memory_query': ToolSpec('memory_query','low','read','Query MG-AI memory service'),
    'research': ToolSpec('research','medium','network','Call MG-AI research service'),
    'python_sandbox': ToolSpec('python_sandbox','medium','execute','Execute code only in an isolated worker'),
    'robot_task': ToolSpec('robot_task','high','robot_mission','Submit only a high-level robot task; never actuator commands'),
}

ALLOWED_BINOPS = {ast.Add: operator.add, ast.Sub: operator.sub, ast.Mult: operator.mul, ast.Div: operator.truediv,
                  ast.Pow: operator.pow, ast.Mod: operator.mod, ast.FloorDiv: operator.floordiv}
ALLOWED_UNARY = {ast.UAdd: operator.pos, ast.USub: operator.neg}

class PermissionErrorDenied(Exception): pass

def authorize(tool_name: str, permissions: set[str]) -> ToolSpec:
    spec = TOOLS.get(tool_name)
    if not spec:
        raise KeyError('unknown_tool')
    if spec.permission not in permissions:
        raise PermissionErrorDenied(f'missing_permission:{spec.permission}')
    return spec

def safe_calc(expr: str) -> float:
    node = ast.parse(expr, mode='eval')
    def ev(n):
        if isinstance(n, ast.Expression): return ev(n.body)
        if isinstance(n, ast.Constant) and isinstance(n.value,(int,float)): return n.value
        if isinstance(n, ast.BinOp) and type(n.op) in ALLOWED_BINOPS: return ALLOWED_BINOPS[type(n.op)](ev(n.left),ev(n.right))
        if isinstance(n, ast.UnaryOp) and type(n.op) in ALLOWED_UNARY: return ALLOWED_UNARY[type(n.op)](ev(n.operand))
        raise ValueError('unsupported_expression')
    value = ev(node)
    if isinstance(value, complex) or not math.isfinite(float(value)):
        raise ValueError('non_finite_result')
    return float(value)

def manifest() -> Dict[str, Any]:
    return {'tools':[asdict(v) for v in TOOLS.values()], 'robot_rule':'LLM cannot issue actuator-level commands'}
