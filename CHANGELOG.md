# Tx3 Toolkit — Changelog

## [1.0.3] — 2026-02-22

### Fixed

- **Plugin crash on disable/enable** — `ClassCastException` when toggling the
  plugin in a live session. The previous fix (1.0.2) used a `try/catch` that
  could never intercept the error because the JVM enforces parameter type casts
  at the bytecode level before any method body executes. Fixed properly by
  switching to `NoSettings`, eliminating classloader serialization entirely.
- **Plugin icon invisible on light themes** — Added a dark rounded background to
  `pluginIcon.svg` for visibility on light IDE themes and the JetBrains
  Marketplace. Added `pluginIcon_dark.svg` (transparent background) for dark IDE
  themes, following the JetBrains dual-icon convention.

## [1.0.2] — 2026-02-22

### Fixed

- **Startup crash** — `ClassCastException` when IntelliJ passes inlay hint
  `Settings` serialized by a previous classloader instance (e.g., after a plugin
  update or IDE restart without full reinstalling). The provider now falls back
  to default settings instead of crashing.

## [1.0.1] - 2026-02-22

- **Plugin ID rejected** — The plugin ID `io.txpipe.tx3.intellij` was rejected
  by the JetBrains Marketplace validator as it must not contain the word
  `intellij`. Renamed to `io.txpipe.tx3`.
- **`env` block parse error** — Fields inside `env { ... }` blocks were
  incorrectly flagged as errors due to a double-brace consumption bug introduced
  during the `parseRecordFieldBlock` refactor. The opening `{` was being
  consumed twice, causing the first field name to be parsed as an unexpected
  token.
- **Builtin type highlighting lost** — `Int`, `Bytes`, `Bool` and other builtin
  types were losing their syntax highlighting inside `type` and `record`
  declarations. The annotator's semantic pass was overriding lexer-level token
  colors without re-applying them on `TypeRef` nodes.
- **Update plugin icon** — The plugin icon was updated to match the branding
  guidelines from JetBrains to ensure it appears consistently on the marketplace
  and in the plugin manager.

## [1.0.0] — 2026-02-22

### Added

- Syntax highlighting with semantic color tokens for all Tx3 constructs
- Smart code completion for keywords, block fields, types, and user-defined
  symbols
- Type-aware inlay hints showing parameter and record field types inline
- Code folding for `tx`, `type`, `record`, `party`, and `policy` blocks
- Structure view and file outliner for quick navigation
- Live templates for common Tx3 patterns (`tx`, `party`, `record`, `input`,
  `output`)
- Error annotations with quick-fixes for missing trailing commas
- Built-in Tx3 color scheme (dark theme)
- Auto-closing braces, brackets, and parentheses
- Formatter with correct 4-space indentation and block-aware enter handling
- New File action with template selection (Blank, Simple Transfer, Vesting
  Contract)
- Support for all Tx3 block types: `input`, `input*`, `output`, `burn`, `mint`,
  `locals`, `collateral`, `reference`, `signers`, `validity`, `metadata`
- Support for `env`, `asset`, `type`, `record`, `party`, `policy` declarations
- Support for variant types, generic types, and field access expressions