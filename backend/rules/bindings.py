"""Named conditions and actions that the YAML rules in this folder call.

Every name used in a rule file must be registered in `build_engine`. Conditions hold the checks,
actions hold the side effects. Routes never call the database or pipeline directly.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

from fastapi import BackgroundTasks

from auth import verify_admin_token
from database import delete_incident, load_stored_incidents
from pipeline import process_url_to_incident, run_agent_pipeline

from .engine import ActionRegistry, ConditionRegistry, RuleEngine

RULES_DIR = Path(__file__).resolve().parent


def admin_token_valid(token: str | None) -> bool:
    return verify_admin_token(token)


def url_is_http(url: str) -> bool:
    return bool(url) and url.startswith(("http://", "https://"))


def incident_was_deleted(deleted: bool) -> bool:
    return deleted


def list_stored_incidents() -> list[dict[str, Any]]:
    return load_stored_incidents()


def delete_stored_incident(incident_id: str) -> bool:
    return delete_incident(incident_id)


async def process_url(url: str) -> dict[str, Any]:
    return await process_url_to_incident(url)


def schedule_pipeline(background_tasks: BackgroundTasks, prompt: str) -> None:
    background_tasks.add_task(run_agent_pipeline, prompt)


def build_engine() -> RuleEngine:
    """Registers every named callable and loads rules/*.yaml. Raises RuleConfigError on any unknown name."""
    conditions = ConditionRegistry()
    conditions.register("admin_token_valid", admin_token_valid)
    conditions.register("url_is_http", url_is_http)
    conditions.register("incident_was_deleted", incident_was_deleted)

    actions = ActionRegistry()
    actions.register("list_stored_incidents", list_stored_incidents)
    actions.register("delete_stored_incident", delete_stored_incident)
    actions.register("process_url", process_url)
    actions.register("schedule_pipeline", schedule_pipeline)

    return RuleEngine.from_directory(RULES_DIR, conditions, actions)
