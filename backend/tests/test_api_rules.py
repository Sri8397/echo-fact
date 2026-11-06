import secrets
import time
from typing import Any, Dict, List

import pytest
from fastapi.testclient import TestClient

import auth
import rules.bindings as bindings
from main import app

client = TestClient(app)

INCIDENT_ID = "case-test"
SAMPLE_INCIDENT: Dict[str, Any] = {"id": INCIDENT_ID, "title": "Sample", "sources": []}


@pytest.fixture
def admin_headers() -> Dict[str, str]:
    token = secrets.token_hex(32)
    auth.ACTIVE_TOKENS[token] = time.time() + 600
    yield {"x-admin-token": token}
    auth.ACTIVE_TOKENS.pop(token, None)


@pytest.fixture
def side_effects(monkeypatch: pytest.MonkeyPatch) -> List[str]:
    """Replaces every side effect so a rejected request can prove nothing ran."""
    calls: List[str] = []

    def record(name: str):
        def fake(*args: Any, **kwargs: Any) -> Any:
            calls.append(name)
            return True if name == "delete" else None
        return fake

    async def record_async(*args: Any, **kwargs: Any) -> Any:
        calls.append("async")
        return SAMPLE_INCIDENT

    monkeypatch.setattr(bindings, "delete_incident", record("delete"))
    monkeypatch.setattr(bindings, "process_url_to_incident", record_async)
    monkeypatch.setattr(bindings, "run_agent_pipeline", record("pipeline"))
    return calls


PROTECTED_CALLS = [
    ("delete", f"/api/incidents/{INCIDENT_ID}", None),
    ("post", "/api/incidents/from-url", {"url": "https://example.com/news/a"}),
    ("post", "/api/pipeline/run", None),
]


@pytest.mark.parametrize("method,path,body", PROTECTED_CALLS)
def test_protected_endpoints_reject_missing_or_bad_token(method, path, body, side_effects) -> None:
    expired = {"x-admin-token": "expired"}
    auth.ACTIVE_TOKENS["expired"] = time.time() - 1
    try:
        for headers in ({}, {"x-admin-token": "not-a-real-token"}, expired):
            response = client.request(method.upper(), path, json=body, headers=headers)
            assert response.status_code == 401
            assert response.json()["detail"].startswith("Unauthorized: Admin session token")
    finally:
        auth.ACTIVE_TOKENS.pop("expired", None)
    assert side_effects == []


def test_get_incidents_returns_stored_list(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(bindings, "load_stored_incidents", lambda: [SAMPLE_INCIDENT])

    response = client.get("/api/incidents")

    assert response.status_code == 200
    assert response.json() == [SAMPLE_INCIDENT]


def test_delete_success_shape(admin_headers, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(bindings, "delete_incident", lambda incident_id: True)

    response = client.delete(f"/api/incidents/{INCIDENT_ID}", headers=admin_headers)

    assert response.status_code == 200
    assert response.json() == {"status": "success", "message": f"Incident '{INCIDENT_ID}' deleted."}


def test_delete_missing_incident_is_404(admin_headers, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(bindings, "delete_incident", lambda incident_id: False)

    response = client.delete(f"/api/incidents/{INCIDENT_ID}", headers=admin_headers)

    assert response.status_code == 404
    assert response.json() == {"detail": f"Incident with ID '{INCIDENT_ID}' not found."}


def test_from_url_rejects_non_http_url(admin_headers, side_effects) -> None:
    response = client.post("/api/incidents/from-url", json={"url": "ftp://example.com/a"}, headers=admin_headers)

    assert response.status_code == 400
    assert response.json() == {"detail": "Invalid URL provided. Must start with http:// or https://"}
    assert side_effects == []


def test_from_url_returns_incident(admin_headers, side_effects) -> None:
    response = client.post("/api/incidents/from-url", json={"url": "https://example.com/a"}, headers=admin_headers)

    assert response.status_code == 200
    assert response.json() == SAMPLE_INCIDENT
    assert side_effects == ["async"]


def test_from_url_processing_error_is_500(admin_headers, monkeypatch: pytest.MonkeyPatch) -> None:
    async def broken(url: str) -> Dict[str, Any]:
        raise RuntimeError("boom")

    monkeypatch.setattr(bindings, "process_url_to_incident", broken)

    response = client.post("/api/incidents/from-url", json={"url": "https://example.com/a"}, headers=admin_headers)

    assert response.status_code == 500
    assert response.json() == {"detail": "Failed to process URL into incident: boom"}


def test_pipeline_run_returns_started_shape(admin_headers, monkeypatch: pytest.MonkeyPatch) -> None:
    prompts: List[str] = []

    async def fake_pipeline(prompt: str) -> None:
        prompts.append(prompt)

    monkeypatch.setattr(bindings, "run_agent_pipeline", fake_pipeline)

    response = client.post("/api/pipeline/run", params={"prompt": "hello"}, headers=admin_headers)

    assert response.status_code == 200
    assert response.json() == {
        "status": "pipeline_started",
        "message": "ADK Multi-Agent Pipeline launched in background with prompt: 'hello'.",
    }
    assert prompts == ["hello"]
