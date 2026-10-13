# Ponytail Rules and Skills

**Summary**: Minimalist senior developer ruleset enforcing YAGNI, standard library first, zero unrequested abstraction, and concise changes.
**Sources**: `DietrichGebert/ponytail`, `.agents/rules/ponytail.md`, `.agents/skills/ponytail/`
**Last updated**: 2026-10-13

---

## Overview

Ponytail introduces a senior developer discipline mindset into AI coding agents: "The best code is the code you never wrote." It curbs agent over-engineering by requiring the smallest complete change that fully satisfies the task.

## The Decision Ladder

Before writing any new code, the agent evaluates the ladder:
1. **Does it need to exist?** Skip unnecessary features, options, and speculative abstractions.
2. **Is it already in this codebase?** Reuse existing helpers, patterns, and services.
3. **Does the standard library or platform do it?** Prefer standard library and existing platform capabilities.
4. **Does an installed dependency solve it?** Never add a new dependency for a few lines.
5. **Can it be a clear one-liner?** Keep it simple and readable.
6. **Otherwise:** Write the minimum necessary working code.

## Configured Tools and Skills

- `.agents/rules/ponytail.md`: Always-on workspace rule enforcing minimal changes and transparency note.
- `.agents/skills/ponytail/SKILL.md`: Main ponytail skill supporting `lite`, `full` (default), and `ultra` modes.
- `.agents/skills/ponytail-review/SKILL.md`: Change review evaluating correctness, security, scale, and lean potential.
- `.agents/skills/ponytail-audit/SKILL.md`: Full-repo audit identifying bugs, risks, test gaps, and bloat.
- `.agents/skills/ponytail-debt/SKILL.md`: Shortcut tracker scanning `shortcut:` comments.
- `.agents/skills/ponytail-gain/SKILL.md`: Scoreboard of measured savings.
- `.agents/skills/ponytail-help/SKILL.md`: Command and usage reference.

## Related pages

- [[index]]
- [[concepts/graphify]]
