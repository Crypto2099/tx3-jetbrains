package io.txpipe.tx3.intellij.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import io.txpipe.tx3.intellij.Tx3Icons
import io.txpipe.tx3.intellij.psi.Tx3File
import io.txpipe.tx3.intellij.psi.impl.*

/**
 * Context-aware code completion for Tx3.
 *
 * Completion categories:
 *  - **Top-level** — `party`, `policy`, `record`, `tx` snippets
 *  - **Tx body** — `input`, `output`, `let` when inside a `tx` block
 *  - **Input fields** — `from`, `min_amount`, `ref`, `redeemer`, `datum`
 *  - **Output fields** — `to`, `amount`, `datum`
 *  - **Types** — built-in types + user-defined record names
 *  - **Expressions** — party names, tx params, input block names, `Ada(...)`, `fees`
 */
class Tx3CompletionContributor : CompletionContributor() {

    init {
        // ── Top-level declarations ──────────────────────────────────────────
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            TopLevelCompletionProvider()
        )
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        // Extend the identifier end to ensure we replace the whole identifier
        context.dummyIdentifier = CompletionInitializationContext.DUMMY_IDENTIFIER_TRIMMED
    }
}

// ── Top-level / Contextual Provider ───────────────────────────────────────────

private class TopLevelCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val file = position.containingFile as? Tx3File ?: return

        // Determine context by walking up the PSI tree
        val inputBlock = PsiTreeUtil.getParentOfType(position, Tx3InputBlockImpl::class.java)
        val outputBlock = PsiTreeUtil.getParentOfType(position, Tx3OutputBlockImpl::class.java)
        val txDecl = PsiTreeUtil.getParentOfType(position, Tx3TxDeclImpl::class.java)
        when {
            // ── Inside an input block body ──────────────────────────────────
            inputBlock != null -> {
                addInputFieldKeywords(result)
                addPartiesAndPolicies(file, result)
                addBuiltinExpressions(result)
            }

            // ── Inside an output block body ─────────────────────────────────
            outputBlock != null -> {
                addOutputFieldKeywords(result)
                addPartiesAndPolicies(file, result)
                addTxScopeExpressions(txDecl, file, result)
                addBuiltinExpressions(result)
            }

            // ── Inside a tx body (but not in a block) ───────────────────────
            txDecl != null -> {
                addTxBodyKeywords(result)
            }

            // ── After a colon — type position ───────────────────────────────
            isInTypePosition(position) -> {
                addBuiltinTypes(result)
                addUserDefinedRecordTypes(file, result)
            }

            // ── Top-level ───────────────────────────────────────────────────
            else -> {
                addTopLevelKeywords(result)
                // Also show party/policy/record names for expression contexts
                addPartiesAndPolicies(file, result)
            }
        }
    }

    // ── Field keyword groups ──────────────────────────────────────────────────

    private fun addInputFieldKeywords(result: CompletionResultSet) {
        listOf(
            "from"       to ": ",
            "min_amount" to ": Ada(",
            "ref"        to ": ",
            "redeemer"   to ": ",
            "datum"      to ": ",
        ).forEach { (kw, tail) ->
            result.addElement(
                LookupElementBuilder.create(kw)
                    .withIcon(Tx3Icons.INPUT)
                    .withTypeText("input field")
                    .withTailText(tail, true)
                    .bold()
                    .withInsertHandler(ColonSpaceInsertHandler)
            )
        }
    }

    private fun addOutputFieldKeywords(result: CompletionResultSet) {
        listOf(
            "to"     to ": ",
            "amount" to ": Ada(",
            "datum"  to ": ",
        ).forEach { (kw, tail) ->
            result.addElement(
                LookupElementBuilder.create(kw)
                    .withIcon(Tx3Icons.OUTPUT)
                    .withTypeText("output field")
                    .withTailText(tail, true)
                    .bold()
                    .withInsertHandler(ColonSpaceInsertHandler)
            )
        }
    }

    private fun addTxBodyKeywords(result: CompletionResultSet) {
        result.addElement(snippet("input", "input $1 {\n    from: $2,\n    min_amount: Ada($3),\n}", "input block", Tx3Icons.INPUT))
        result.addElement(snippet("output", "output $1 {\n    to: $2,\n    amount: $3,\n}", "output block", Tx3Icons.OUTPUT))
        result.addElement(snippet("let", "let $1 = $2;", "let binding", Tx3Icons.FIELD))
    }

    // ── Top-level snippets ────────────────────────────────────────────────────

    private fun addTopLevelKeywords(result: CompletionResultSet) {
        result.addElement(snippet("party",  "party $1;",          "party declaration",  Tx3Icons.PARTY))
        result.addElement(snippet("policy", "policy $1 = import($2);", "policy declaration", Tx3Icons.POLICY))
        result.addElement(snippet("record", "record $1 {\n    $2: $3,\n}", "record declaration", Tx3Icons.RECORD))
        result.addElement(snippet("tx",
            "tx $1($2: $3) {\n    input $4 {\n        from: $5,\n        min_amount: Ada($2),\n    }\n    output {\n        to: $6,\n        amount: Ada($2),\n    }\n    output {\n        to: $5,\n        amount: $4 - Ada($2) - fees,\n    }\n}",
            "transaction template",
            Tx3Icons.TX
        ))
    }

    // ── Built-in types ────────────────────────────────────────────────────────

    private fun addBuiltinTypes(result: CompletionResultSet) {
        listOf(
            "Int"     to "integer numeric value",
            "Bytes"   to "raw byte string",
            "Bool"    to "boolean true/false",
            "Unit"    to "unit type ()",
            "UtxoRef" to "UTxO reference (txHash#index)",
            "Address" to "Cardano address",
            "Value"   to "multi-asset value",
        ).forEach { (type, doc) ->
            result.addElement(
                LookupElementBuilder.create(type)
                    .withIcon(Tx3Icons.FIELD)
                    .withTypeText("built-in type")
                    .withTailText(" — $doc", true)
                    .bold()
            )
        }
    }

    private fun addUserDefinedRecordTypes(file: Tx3File, result: CompletionResultSet) {
        file.recordDeclarations().forEach { record ->
            val name = (record as? Tx3NamedElementBase)?.name ?: return@forEach
            result.addElement(
                LookupElementBuilder.create(name)
                    .withIcon(Tx3Icons.RECORD)
                    .withTypeText("record")
                    .withPsiElement(record)
            )
        }
    }

    // ── File-level symbol completions ─────────────────────────────────────────

    private fun addPartiesAndPolicies(file: Tx3File, result: CompletionResultSet) {
        file.partyDeclarations().forEach { party ->
            val name = (party as? Tx3NamedElementBase)?.name ?: return@forEach
            result.addElement(
                LookupElementBuilder.create(name)
                    .withIcon(Tx3Icons.PARTY)
                    .withTypeText("party")
                    .withPsiElement(party)
            )
        }
        file.policyDeclarations().forEach { policy ->
            val name = (policy as? Tx3NamedElementBase)?.name ?: return@forEach
            result.addElement(
                LookupElementBuilder.create(name)
                    .withIcon(Tx3Icons.POLICY)
                    .withTypeText("policy")
                    .withPsiElement(policy)
            )
        }
    }

    private fun addTxScopeExpressions(
        txDecl: Tx3TxDeclImpl?,
        file: Tx3File,
        result: CompletionResultSet
    ) {
        txDecl?.params()?.forEach { param ->
            val name = param.name ?: return@forEach
            val typeName = param.paramType()?.typeName() ?: "?"
            result.addElement(
                LookupElementBuilder.create(name)
                    .withIcon(Tx3Icons.PARAM)
                    .withTypeText(typeName)
            )
        }
        txDecl?.inputBlocks()?.forEach { block ->
            val name = block.name ?: return@forEach
            result.addElement(
                LookupElementBuilder.create(name)
                    .withIcon(Tx3Icons.INPUT)
                    .withTypeText("input UTxO set")
            )
        }
        addPartiesAndPolicies(file, result)
    }

    private fun addBuiltinExpressions(result: CompletionResultSet) {
        result.addElement(
            LookupElementBuilder.create("Ada")
                .withIcon(BUILTIN_ICON)
                .withTypeText("asset constructor")
                .withTailText("(lovelace: Int)", true)
                .withInsertHandler { ctx, _ ->
                    ctx.document.insertString(ctx.tailOffset, "()")
                    ctx.editor.caretModel.moveToOffset(ctx.tailOffset - 1)
                }
        )
        result.addElement(
            LookupElementBuilder.create("fees")
                .withIcon(BUILTIN_ICON)
                .withTypeText("built-in: estimated tx fees")
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun snippet(
        keyword: String,
        template: String,
        typeText: String,
        icon: javax.swing.Icon
    ): LookupElementBuilder =
        LookupElementBuilder.create(keyword)
            .withIcon(icon)
            .withTypeText(typeText)
            .bold()
            .withInsertHandler(LiveTemplateInsertHandler(template))

    private fun isInTypePosition(element: PsiElement): Boolean {
        // Check if the previous significant token was a colon
        var prev = element.prevSibling
        while (prev != null) {
            val text = prev.text.trim()
            if (text.isNotEmpty()) return text == ":"
            prev = prev.prevSibling
        }
        return false
    }
}

// ── Insert Handlers ───────────────────────────────────────────────────────────

private object ColonSpaceInsertHandler : InsertHandler<com.intellij.codeInsight.lookup.LookupElement> {
    override fun handleInsert(context: InsertionContext, @Suppress("UNUSED_PARAMETER") item: com.intellij.codeInsight.lookup.LookupElement) {
        val doc = context.document
        val offset = context.tailOffset
        // Insert an ":" if not already present
        if (offset < doc.textLength && doc.getText(com.intellij.openapi.util.TextRange(offset, offset + 1)) != ":") {
            doc.insertString(offset, ": ")
            context.editor.caretModel.moveToOffset(offset + 2)
        }
    }
}

/**
 * Insert handler that expands a keyword into a multi-line snippet.
 * Uses $1, $2... as placeholder markers (simplified — for full live template
 * support, wire into IntelliJ's TemplateManager).
 */
private class LiveTemplateInsertHandler(private val template: String) :
    InsertHandler<com.intellij.codeInsight.lookup.LookupElement> {

    override fun handleInsert(context: InsertionContext, item: com.intellij.codeInsight.lookup.LookupElement) {
        val editor = context.editor
        val doc = context.document

        // Replace the typed keyword with a template (strip $N placeholders for now)
        val stripped = template.replace(Regex("\\$\\d+"), "")
        doc.replaceString(context.startOffset, context.tailOffset, stripped)

        // Move the caret to the first $1 position (after stripping, just go to start + keyword length)
        val caretTarget = context.startOffset + item.lookupString.length + 1
        if (caretTarget <= doc.textLength) {
            editor.caretModel.moveToOffset(caretTarget)
        }
    }
}

// Placeholder icon for builtins
private val BUILTIN_ICON = Tx3Icons.FIELD