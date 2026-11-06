"""YAML rule engine for the backend's multi-step workflows.

A rule file declares `rule`, `description`, `inputs` and `steps`. Each step is exactly one of:

- `condition: <name>` with `args` and `otherwise: {status, detail}`. The rule stops with that
  rejection when the condition returns False.
- `action: <name>` with `args`, an optional `result: <key>` and an optional
  `on_error: {status, detail}`. An exception becomes that rejection, with `{error}` set to its message.
- `rule: <name>` runs another rule's steps on the same context. Its rejection or response ends this rule.
- `respond: <body>` ends the rule with a success body.

Values: `$key` reads the context, a string containing `{key}` is formatted from the context,
anything else is a literal. Every name is checked against the registries and every reference
against the inputs and earlier results when the engine is built, so a bad rule file fails at startup.
"""

from __future__ import annotations

import inspect
import string
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable, Mapping

import yaml

MAX_RULE_DEPTH = 8

STEP_KEYS: dict[str, frozenset[str]] = {
    "condition": frozenset({"condition", "args", "otherwise"}),
    "action": frozenset({"action", "args", "result", "on_error"}),
    "rule": frozenset({"rule"}),
    "respond": frozenset({"respond"}),
}


class RuleConfigError(Exception):
    """A rule file is malformed, or refers to names or inputs that do not exist."""


class _Registry:
    kind: str = "callable"

    def __init__(self) -> None:
        self._fns: dict[str, Callable[..., Any]] = {}

    def register(self, name: str, fn: Callable[..., Any]) -> None:
        if name in self._fns:
            raise RuleConfigError(f"{self.kind} '{name}' is registered twice")
        self._fns[name] = fn

    def __contains__(self, name: object) -> bool:
        return name in self._fns

    def get(self, name: str) -> Callable[..., Any]:
        return self._fns[name]


class ConditionRegistry(_Registry):
    kind = "condition"


class ActionRegistry(_Registry):
    kind = "action"


@dataclass(frozen=True)
class Reject:
    status: int
    detail: str


@dataclass(frozen=True)
class Step:
    kind: str
    target: str
    args: dict[str, Any]
    result: str | None
    reject: Reject | None
    body: Any


@dataclass(frozen=True)
class Rule:
    name: str
    inputs: tuple[str, ...]
    steps: tuple[Step, ...]


@dataclass(frozen=True)
class RuleResult:
    ok: bool
    status_code: int = 200
    body: Any = None
    detail: str = ""


def _parse_reject(raw: Any, where: str) -> Reject:
    if not isinstance(raw, dict) or set(raw) != {"status", "detail"}:
        raise RuleConfigError(f"{where}: rejection must be {{status, detail}}, got {raw!r}")
    return Reject(status=int(raw["status"]), detail=str(raw["detail"]))


def _parse_step(raw: Any, where: str) -> Step:
    if not isinstance(raw, dict):
        raise RuleConfigError(f"{where}: each step must be a mapping")
    kinds = [key for key in STEP_KEYS if key in raw]
    if len(kinds) != 1:
        raise RuleConfigError(f"{where}: a step needs exactly one of {list(STEP_KEYS)}, got {sorted(raw)}")
    kind = kinds[0]
    unknown = set(raw) - STEP_KEYS[kind]
    if unknown:
        raise RuleConfigError(f"{where}: unknown keys {sorted(unknown)} in {kind} step")

    args = raw.get("args") or {}
    if not isinstance(args, dict):
        raise RuleConfigError(f"{where}: args must be a mapping")

    reject: Reject | None = None
    if kind == "condition":
        if "otherwise" not in raw:
            raise RuleConfigError(f"{where}: condition '{raw[kind]}' needs an 'otherwise' rejection")
        reject = _parse_reject(raw["otherwise"], where)
    elif "on_error" in raw:
        reject = _parse_reject(raw["on_error"], where)

    target = "" if kind == "respond" else str(raw[kind])
    result = raw.get("result")
    return Step(kind=kind, target=target, args=args, result=None if result is None else str(result),
                reject=reject, body=raw.get("respond"))


def _parse_rule(path: Path, raw: Any) -> Rule:
    if not isinstance(raw, dict) or "rule" not in raw or not isinstance(raw.get("steps"), list):
        raise RuleConfigError(f"{path.name}: a rule needs 'rule' and a 'steps' list")
    inputs = raw.get("inputs") or []
    if not isinstance(inputs, list) or not all(isinstance(name, str) for name in inputs):
        raise RuleConfigError(f"{path.name}: 'inputs' must be a list of names")
    steps = tuple(_parse_step(step, f"{path.name} step {index}") for index, step in enumerate(raw["steps"], 1))
    return Rule(name=str(raw["rule"]), inputs=tuple(inputs), steps=steps)


def _refs(value: Any) -> set[str]:
    """Names a YAML value reads from the context: `$key` or `{key}` placeholders."""
    if isinstance(value, str):
        if value.startswith("$"):
            return {value[1:]}
        return {field for _, field, _, _ in string.Formatter().parse(value) if field} if "{" in value else set()
    if isinstance(value, dict):
        return set().union(*(_refs(item) for item in value.values()))
    if isinstance(value, list):
        return set().union(*(_refs(item) for item in value))
    return set()


def _unbound(value: Any, allowed: set[str], where: str) -> list[str]:
    try:
        missing = _refs(value) - allowed
    except ValueError as exc:
        return [f"{where}: malformed template {value!r} ({exc})"]
    return [f"{where}: '{name}' is not an input or an earlier result" for name in sorted(missing)]


def _resolve(value: Any, context: Mapping[str, Any]) -> Any:
    if isinstance(value, str):
        if value.startswith("$"):
            return context[value[1:]]
        return value.format_map(context) if "{" in value else value
    if isinstance(value, dict):
        return {key: _resolve(item, context) for key, item in value.items()}
    if isinstance(value, list):
        return [_resolve(item, context) for item in value]
    return value


def _reject(reject: Reject, context: Mapping[str, Any]) -> RuleResult:
    return RuleResult(ok=False, status_code=reject.status, detail=reject.detail.format_map(context))


class RuleEngine:
    """Runs named rules. Build it once at startup: construction validates every rule against the registries."""

    def __init__(self, conditions: ConditionRegistry, actions: ActionRegistry, rules: Mapping[str, Rule]) -> None:
        self.conditions = conditions
        self.actions = actions
        self.rules: dict[str, Rule] = dict(rules)
        self._validate()

    @classmethod
    def from_directory(cls, directory: Path, conditions: ConditionRegistry, actions: ActionRegistry) -> RuleEngine:
        rules: dict[str, Rule] = {}
        for path in sorted(directory.glob("*.yaml")):
            rule = _parse_rule(path, yaml.safe_load(path.read_text(encoding="utf-8")))
            if rule.name in rules:
                raise RuleConfigError(f"{path.name}: rule '{rule.name}' is already defined")
            rules[rule.name] = rule
        return cls(conditions, actions, rules)

    def _validate(self) -> None:
        problems: list[str] = []
        for rule in self.rules.values():
            available = set(rule.inputs)
            for index, step in enumerate(rule.steps, 1):
                problems += self._check_step(step, f"rule '{rule.name}' step {index}", available)
        if problems:
            raise RuleConfigError("Rule engine startup failed:\n  - " + "\n  - ".join(problems))

    def _check_step(self, step: Step, where: str, available: set[str]) -> list[str]:
        if step.kind == "respond":
            return _unbound(step.body, available, where)

        if step.kind == "rule":
            if step.target not in self.rules:
                return [f"{where}: unknown rule '{step.target}'"]
            missing = set(self.rules[step.target].inputs) - available
            available |= self._produces(step.target)
            if missing:
                return [f"{where}: rule '{step.target}' needs {sorted(missing)}, which are not available here"]
            return []

        registry = self.conditions if step.kind == "condition" else self.actions
        if step.target not in registry:
            return [f"{where}: unknown {step.kind} '{step.target}'"]
        problems: list[str] = []
        try:
            inspect.signature(registry.get(step.target)).bind(**dict.fromkeys(step.args))
        except TypeError as exc:
            problems.append(f"{where}: args do not fit {step.kind} '{step.target}' ({exc})")
        problems += _unbound(step.args, available, where)
        if step.reject is not None:
            error_names = {"error"} if step.kind == "action" else set()
            problems += _unbound(step.reject.detail, available | error_names, where)
        if step.kind == "action" and step.result:
            available.add(step.result)
        return problems

    def _produces(self, rule_name: str, seen: frozenset[str] = frozenset()) -> set[str]:
        if rule_name in seen:
            return set()
        produced: set[str] = set()
        for step in self.rules[rule_name].steps:
            if step.kind == "action" and step.result:
                produced.add(step.result)
            elif step.kind == "rule" and step.target in self.rules:
                produced |= self._produces(step.target, seen | {rule_name})
        return produced

    async def run(self, rule_name: str, inputs: Mapping[str, Any]) -> RuleResult:
        rule = self.rules[rule_name]
        missing = [name for name in rule.inputs if name not in inputs]
        if missing:
            raise ValueError(f"rule '{rule_name}' was run without inputs {missing}")
        context: dict[str, Any] = dict(inputs)
        outcome = await self._run_steps(rule, context, depth=0)
        return outcome or RuleResult(ok=True)

    async def _run_steps(self, rule: Rule, context: dict[str, Any], depth: int) -> RuleResult | None:
        if depth > MAX_RULE_DEPTH:
            raise RuntimeError(f"rule '{rule.name}' nests calls deeper than {MAX_RULE_DEPTH}")
        for step in rule.steps:
            if step.kind == "condition":
                assert step.reject is not None
                kwargs = _resolve(step.args, context)
                if not self.conditions.get(step.target)(**kwargs):
                    return _reject(step.reject, context)
            elif step.kind == "action":
                kwargs = _resolve(step.args, context)
                try:
                    value = self.actions.get(step.target)(**kwargs)
                    if inspect.isawaitable(value):
                        value = await value
                except Exception as exc:
                    if step.reject is None:
                        raise
                    return _reject(step.reject, {**context, "error": str(exc)})
                if step.result:
                    context[step.result] = value
            elif step.kind == "rule":
                outcome = await self._run_steps(self.rules[step.target], context, depth + 1)
                if outcome is not None:
                    return outcome
            else:
                return RuleResult(ok=True, body=_resolve(step.body, context))
        return None
