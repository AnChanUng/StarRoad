"""Local integration fixture: real FastAPI/catalog, deterministic model substitute."""
import sys
import os
from pathlib import Path
repo = Path(os.environ.get('POLICY_AI_REPO', str(Path(__file__).resolve().parents[2] / 'RAG_Youth-Policy')))
sys.path.insert(0, str(repo))
from youth_policy import api
from types import SimpleNamespace
import uvicorn

from youth_policy.config import load_settings
from youth_policy.service import PolicyService
engine = PolicyService(load_settings())


def ask(policy_id, question, history, mode):
    if question == 'TEST_SERVICE_FAILURE':
        raise RuntimeError('internal fixture failure')
    policy = engine.by_id[policy_id]
    return SimpleNamespace(to_dict=lambda: {
        'text': ('신청 체크리스트' if mode == 'checklist' else '정책 상담') + ' 테스트 응답 [근거 1]',
        'status': 'answered', 'uncited': False, 'elapsed_ms': 25,
        'sources': [{'number': 1, 'title': policy.name, 'url': policy.source_url, 'page': 0, 'text': 'internal body'}],
        'trace': {'internal': True}})


engine.ask = ask
api.service = lambda: engine
uvicorn.run(api.app, host='127.0.0.1', port=18001, log_level='warning')
