# jbh-notification

The rest of this file is written by change `jbh-notification-create-service` (JBH-74).

## Pipeline config (kkpa-orchestrate-change)

- **Modules-Root:** `..` (single-module repo: the module is this repo, `jbh-notification`.
  The Maven modules `jbh-notification-contracts`, `jbh-notification-infra` and
  `jbh-notification-app` are layers of that one module.)
- **Doc-Pack:** `CLAUDE.md`, `../jbh-personal-finance/.claude/standards/code-quality.md`,
  `../jbh-personal-finance/.claude/standards/naming.md`,
  `../jbh-personal-finance/.claude/architecture/hexagonal-layers.md`
- **Complexity-Check:** `mvn -q pmd:check` (uses `pmd-ruleset.xml`, copied from `jbh-personal-finance`)
- **Design-Gates:** `database`
- **Git-Strategy:** `auto`
