import asyncio
import textwrap
from pathlib import Path

import pytest

from rules.bindings import build_engine
from rules.engine import ActionRegistry, ConditionRegistry, RuleConfigError, RuleEngine, RuleResult


def _write(directory: Path, name: str, text: str) -> None:
    (directory / name).write_text(textwrap.dedent(text), encoding="utf-8")


def _registries() -> tuple[ConditionRegistry, ActionRegistry]:
    conditions = ConditionRegistry()
    conditions.register("is_even", lambda n: n % 2 == 0)
    actions = ActionRegistry()
    actions.register("double", lambda n: n * 2)
    return conditions, actions


def test_unknown_names_fail_at_startup_and_are_all_listed(tmp_path: Path) -> None:
    _write(tmp_path, "bad.yaml", """
        rule: bad
        inputs: [n]
        steps:
          - condition: no_such_condition
            args: {n: $n}
            otherwise: {status: 400, detail: "nope"}
          - action: no_such_action
            args: {n: $n}
            result: out
          - respond: $out
    """)
    conditions, actions = _registries()

    with pytest.raises(RuleConfigError) as info:
        RuleEngine.from_directory(tmp_path, conditions, actions)

    message = str(info.value)
    assert "unknown condition 'no_such_condition'" in message
    assert "unknown action 'no_such_action'" in message


def test_unknown_reference_and_bad_args_fail_at_startup(tmp_path: Path) -> None:
    _write(tmp_path, "refs.yaml", """
        rule: refs
        inputs: [n]
        steps:
          - action: double
            args: {wrong_name: $n}
            result: out
          - respond: {value: $missing}
    """)
    conditions, actions = _registries()

    with pytest.raises(RuleConfigError) as info:
        RuleEngine.from_directory(tmp_path, conditions, actions)

    message = str(info.value)
    assert "args do not fit action 'double'" in message
    assert "'missing' is not an input or an earlier result" in message


def test_condition_and_action_run_and_respond(tmp_path: Path) -> None:
    _write(tmp_path, "ok.yaml", """
        rule: doubled_if_even
        inputs: [n]
        steps:
          - condition: is_even
            args: {n: $n}
            otherwise: {status: 400, detail: "{n} is odd"}
          - action: double
            args: {n: $n}
            result: out
          - respond: {value: $out, label: "x{n}"}
    """)
    engine = RuleEngine.from_directory(tmp_path, *_registries())

    accepted = asyncio.run(engine.run("doubled_if_even", {"n": 4}))
    rejected = asyncio.run(engine.run("doubled_if_even", {"n": 3}))

    assert accepted == RuleResult(ok=True, body={"value": 8, "label": "x4"})
    assert rejected == RuleResult(ok=False, status_code=400, detail="3 is odd")


def test_action_error_maps_to_rejection(tmp_path: Path) -> None:
    _write(tmp_path, "boom.yaml", """
        rule: boom
        inputs: [n]
        steps:
          - action: explode
            args: {n: $n}
            on_error: {status: 500, detail: "failed: {error}"}
          - respond: ok
    """)
    conditions, actions = _registries()

    def explode(n: int) -> int:
        raise ValueError("kaput")

    actions.register("explode", explode)
    engine = RuleEngine.from_directory(tmp_path, conditions, actions)

    result = asyncio.run(engine.run("boom", {"n": 1}))

    assert result == RuleResult(ok=False, status_code=500, detail="failed: kaput")


def test_run_without_declared_input_is_rejected(tmp_path: Path) -> None:
    _write(tmp_path, "needs.yaml", """
        rule: needs
        inputs: [n]
        steps:
          - respond: $n
    """)
    engine = RuleEngine.from_directory(tmp_path, *_registries())

    with pytest.raises(ValueError, match="without inputs"):
        asyncio.run(engine.run("needs", {}))


def test_shipped_rules_load_and_name_every_registered_callable() -> None:
    engine = build_engine()

    assert set(engine.rules) == {
        "admin_auth_gate", "incident_list", "incident_from_url", "incident_delete", "pipeline_run",
    }
