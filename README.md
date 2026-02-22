# Tx3 Toolkit

**White-glove Tx3 language support for all JetBrains IDEs** (IntelliJ IDEA,
WebStorm, Rider, CLion, etc.)

Tx3 is a DSL for describing UTxO protocol interfaces on
Cardano. → [Tx3 Documentation](https://docs.txpipe.io/tx3/) | [GitHub](https://github.com/tx3-lang/tx3)

---

## Features

### Syntax Highlighting

Full token-level and semantic highlighting with separate configurable colors
for:

- **Declaration keywords** (`party`, `policy`, `record`, `type`, `tx`, `env`,
  `asset`) — bold, distinct color
- **Block keywords** (`input`, `output`, `burn`, `mint`, `locals`,
  `collateral`, `reference`, `signers`, `validity`, `metadata`, `cardano`) —
  teal/blue
- **Field keywords** (`from`, `to`, `amount`, `datum`, `datum_is`, `redeemer`,
  `min_amount`, `ref`, `script`, `hash`, `since_slot`, `until_slot`, `drep`,
  `stake`, `version`, `coin`) — italic purple
- **Control keywords** (`import`, `let`, `if`, `else`, `true`, `false`)
- **Built-in types** (`Int`, `Bytes`, `Bool`, `Unit`, `UtxoRef`, `Address`,
  `Value`, `List`, `Map`) — green
- **Built-in symbols** (`Ada`, `fees`) — gold
- **Operators**, **literals**, **comments** — all individually configurable
- Both **Default (light)** and **Darcula (dark)** themes are included

### Smart Code Completion

Context-aware completion that knows *where you are*:

- **Top level** → suggests `party`, `policy`, `record`, `type`, `tx` snippets
- **Inside `tx { }`** → suggests `input`, `output`, `let`
- **Inside `input { }`** → suggests `from`, `min_amount`, `ref`, `redeemer`,
  `datum` + all declared parties
- **Inside `output { }`** → suggests `to`, `amount`, `datum` + parties, tx
  params, input block names
- **Type positions** → built-in types and user-defined record/type names from
  the same file
- **Expressions** → `Ada(...)` with auto-parens, `fees`, parties, tx params, let
  bindings
- All completions show **type text** and **tail text** for instant documentation

### Inlay Hints (Type Hints)

- **Tx parameter types** shown inline: `quantity/*: Int*/`
- **Record field types** shown after field name: `lock_until/*: Int*/`
- Configurable in Settings → Editor → Inlay Hints → Tx3

### Code Folding

Collapses any `{ ... }` block that spans multiple lines:

- `tx Name(...) { ... }` — shows tx name and param signature
- `type Name { 3 fields }` / `record Name { 3 fields }` — shows field/variant
  count
- `env { 2 fields }` — shows field count
- `locals { ... }` — local variable bindings inside transactions
- `input source { ... }` / `output { ... }` — shows optional block name
- Inline record literals (`State { ... }`) — folds when 2+ fields
- Variant construction (`TypeName::CaseName { ... }`) — folds when 2+ fields
- Variant case bodies inside type declarations
- `policy Name = import(very/long/path/...)` — folds long import paths
- `/* block comments */` — when multi-line
- Consecutive `// line comments` — when 2+ lines

### Structure View

Press **Cmd+7** (macOS) or **Alt+7** (Windows/Linux) to open the file outline
showing:

- All **parties** with party icon
- All **policies** with policy icon
- All **types/records** with their fields (name and type)
- All **transactions** with full param signature and nested input/output blocks
- Click any item to jump directly to its definition

### Error Annotations & Quick-Fixes

Inline semantic warnings:

- **Output block missing `to` or `amount`** — yellow warning
- **Input block missing `from` or `ref`** — yellow warning
- **Unresolved name reference** — weak warning (wavy underline)
- **Bad characters** — red error highlight
- **Quick-fix:** add trailing comma where missing in record and block fields

### Live Templates

Trigger templates by typing the abbreviation and pressing **Tab**:

| Abbreviation | Expands to                                           |
|--------------|------------------------------------------------------|
| `party`      | `party Name;`                                        |
| `policy`     | `policy Name = import(path);`                        |
| `record`     | `record Name { field: Type, }`                       |
| `type`       | `type Name { ... }` (variant/record declaration)     |
| `tx`         | Full tx declaration skeleton                         |
| `inp`        | `input source { from: Party, min_amount: Ada(...) }` |
| `inpref`     | `input locked { ref: utxo, redeemer: () }`           |
| `out`        | `output { to: Party, amount: ... }`                  |
| `outd`       | Output block with datum                              |
| `change`     | Change-back output (`source - Ada(qty) - fees`)      |
| `let`        | `let name = expr;`                                   |
| `transfer`   | Full 2-party transfer protocol                       |

### New File Wizard

**File → New → Tx3 Protocol File** shows a dialog with starter templates:

- **Blank** — empty file with header comment
- **Simple Transfer** — 2-party value transfer
- **Vesting Contract** — full time-locked vesting protocol (lock and unlock)

### Comment Toggling

- **Cmd+/** — toggle `//` line comments
- **Ctrl+Shift+/** — toggle `/* */` block comments

### Auto-close

Braces `{}`, parentheses `()`, and brackets `[]` auto-close and match.

### Code Formatting

**Ctrl+Alt+L** / **Cmd+Alt+L** — format the entire file with:

- Consistent 4-space indentation inside blocks
- Spaces around operators
- Space after commas and colons
- Opening brace on the same line
- Closing brace on its own line

### Spell-check

Identifiers are excluded from spell-checking (blockchain names are intentionally
non-dictionary words). Comments and strings are still spell-checked.

---

## Project Structure

```
tx3-toolkit/
├── build.gradle.kts                           # Gradle build with grammarkit
├── settings.gradle.kts
├── src/main/
│   ├── kotlin/io/txpipe/tx3/intellij/
│   │   ├── Tx3Language.kt                     # Language singleton
│   │   ├── Tx3FileType.kt                     # .tx3 file type
│   │   ├── Tx3Icons.kt                        # Icon registry
│   │   ├── Tx3Commenter.kt                    # // and /* */ comments
│   │   ├── Tx3TypedHandler.kt                 # Auto-closing braces
│   │   ├── Tx3EnterHandler.kt                 # Smart enter key handling
│   │   ├── Tx3FindUsagesProvider.kt           # Alt+F7 find usages
│   │   ├── Tx3ReferenceContributor.kt         # Go-to-definition
│   │   ├── Tx3TemplateContextType.kt          # Live template context
│   │   ├── Tx3SpellcheckingStrategy.kt        # Spell check exclusions
│   │   ├── lexer/
│   │   │   ├── Tx3Lexer.flex                  # JFlex lexer grammar
│   │   │   ├── Tx3TokenTypes.kt               # Token type constants
│   │   │   └── Tx3LexerAdapter.kt             # FlexAdapter wrapper
│   │   ├── parser/
│   │   │   ├── Tx3Parser.kt                   # Recursive descent parser
│   │   │   ├── Tx3ParserDefinition.kt         # IntelliJ parser wiring
│   │   │   └── Tx3ElementTypes.kt             # AST element types
│   │   ├── psi/
│   │   │   ├── Tx3File.kt                     # PSI file root
│   │   │   ├── Tx3NamedElement.kt             # Named element interface
│   │   │   └── impl/Tx3PsiImpls.kt            # All PSI implementations
│   │   ├── highlighting/
│   │   │   ├── Tx3SyntaxHighlighter.kt        # Token → color mapping
│   │   │   ├── Tx3SyntaxHighlighterFactory.kt # Factory + color settings page
│   │   │   ├── Tx3Annotator.kt                # Semantic error annotations
│   │   │   └── Tx3AddTrailingCommaFix.kt      # Quick-fix for trailing commas
│   │   ├── completion/
│   │   │   └── Tx3CompletionContributor.kt    # Context-aware completion
│   │   ├── folding/
│   │   │   └── Tx3FoldingBuilder.kt           # Code folding regions
│   │   ├── structure/
│   │   │   └── Tx3StructureViewFactory.kt     # Structure panel
│   │   ├── hints/
│   │   │   └── Tx3InlayHintsProvider.kt       # Type inlay hints
│   │   ├── formatting/
│   │   │   └── Tx3FormattingModelBuilder.kt   # Auto-formatter
│   │   └── actions/
│   │       └── Tx3NewFileAction.kt            # New file wizard
│   └── resources/
│       ├── META-INF/plugin.xml                # Plugin manifest
│       ├── icons/                             # SVG icons
│       ├── colorschemes/                      # Light + dark theme colors
│       ├── fileTemplates/                     # New file templates
│       └── liveTemplates/Tx3.xml             # Live template snippets
```

---

## Building

### Prerequisites

- JDK 17+
- Gradle 8.6 (wrapper included)
- IntelliJ IDEA (to run/debug the plugin)

### Development Build

```bash
# Generate lexer from JFlex grammar
./gradlew generateLexer

# Build the plugin
./gradlew buildPlugin

# Run a sandboxed IDE with the plugin installed
./gradlew runIde

# Run all tests
./gradlew test
```

The generated JAR/zip will be in `build/distributions/`.

### Code Generation

The JFlex lexer is defined in `src/main/kotlin/.../lexer/Tx3Lexer.flex`. Running
`./gradlew generateLexer` generates `src/main/gen/.../Tx3FlexLexer.java`.

The parser is hand-written (not auto-generated from a grammar file).

### Publishing to JetBrains Marketplace

```bash
export PUBLISH_TOKEN="your-marketplace-token"
./gradlew publishPlugin
```

---

## Language Reference (Quick)

```tx3
// Declare a party (participant in transactions)
party Sender;
party Receiver;

// Declare a policy (on-chain validator script)
policy TimeLock = import(validators/vesting.ak);

// Declare environment variables
env {
    lock_until: Int,
    owner: Bytes,
}

// Declare a record type (used for datums and redeemers)
record State {
    lock_until: Int,
    owner: Bytes,
    beneficiary: Bytes,
}

// Declare a variant type (sum type / enum)
type Action {
    Lock { until: Int },
    Unlock,
}

// Declare a transaction template
tx lock(quantity: Int, until: Int) {

    // Input block: selects UTxOs matching criteria
    input source {
        from: Sender,
        min_amount: Ada(quantity),
    }

    // Output block: creates new UTxOs
    output target {
        to: TimeLock,
        amount: Ada(quantity),
        datum: State {
            lock_until: until,
            owner: Sender,
            beneficiary: Receiver,
        }
    }

    // Change back to sender
    output {
        to: Sender,
        amount: source - Ada(quantity) - fees,
    }
}
```

**Built-in types:** `Int`, `Bytes`, `Bool`, `Unit`, `UtxoRef`, `Address`,
`Value`, `List`, `Map`
**Built-in symbols:** `Ada(lovelace)`, `fees`
**Input fields:** `from`, `min_amount`, `ref`, `redeemer`, `datum`
**Output fields:** `to`, `amount`, `datum`

---

## Contributing

1. Fork the repository
2. Run `./gradlew runIde` to open a dev IDE
3. Make your changes
4. Run `./gradlew test` to ensure tests pass
5. Open a Pull Request

Please follow the [Tx3 language spec](https://docs.txpipe.io/tx3/language) when
updating parser grammar.

---

## License

Apache 2.0 — the same as the Tx3 language itself.
