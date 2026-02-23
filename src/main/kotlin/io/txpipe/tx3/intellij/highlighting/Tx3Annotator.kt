package io.txpipe.tx3.intellij.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import io.txpipe.tx3.intellij.lexer.Tx3TokenTypes
import io.txpipe.tx3.intellij.parser.Tx3ElementTypes
import io.txpipe.tx3.intellij.psi.Tx3File
import io.txpipe.tx3.intellij.psi.impl.*

private val OUTPUT_REQUIRED_FIELDS = setOf("to", "amount")

private val BUILTIN_NAMES = setOf(
    "Ada", "fees", "true", "false",
    "Int", "Bytes", "Bool", "Unit", "UtxoRef", "Address", "Value",
    "AnyAsset", "min_utxo", "tip_slot", "concat"
)

class Tx3Annotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {

            // ── Highlight declaration names ───────────────────────────────────
            is Tx3PartyDeclImpl  -> highlightDeclName(element, holder, Tx3SyntaxHighlighter.KEYWORD_DECL)
            is Tx3PolicyDeclImpl -> highlightDeclName(element, holder, Tx3SyntaxHighlighter.KEYWORD_DECL)
            is Tx3TypeDeclImpl   -> highlightDeclName(element, holder, Tx3SyntaxHighlighter.BUILTIN_TYPE)
            is Tx3RecordDeclImpl -> highlightDeclName(element, holder, Tx3SyntaxHighlighter.BUILTIN_TYPE)
            is Tx3TxDeclImpl     -> highlightDeclName(element, holder, Tx3SyntaxHighlighter.KEYWORD_DECL)

            // Soft-keyword-named declarations: recolor as plain identifier so
            // e.g., a param named 'metadata' doesn't look like the block keyword.
            is Tx3TxParamImpl    -> {
                highlightDeclName(element, holder, Tx3SyntaxHighlighter.IDENTIFIER)
            }
            is Tx3LetBindingImpl -> {
                highlightDeclName(element, holder, Tx3SyntaxHighlighter.IDENTIFIER)
                checkTrailingComma(element, holder)
            }
            is Tx3RecordFieldImpl -> {
                highlightDeclName(element, holder, Tx3SyntaxHighlighter.IDENTIFIER)
                checkTrailingComma(element, holder)
            }

            // ── Re-apply builtin type coloring on type references ───────────────
            is Tx3TypeRefImpl -> {
                val firstChild = element.node.firstChildNode?.psi ?: return
                val key = if (firstChild.node.elementType in Tx3TokenTypes.BUILTIN_TYPES)
                    Tx3SyntaxHighlighter.BUILTIN_TYPE
                else
                    Tx3SyntaxHighlighter.IDENTIFIER
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(firstChild.textRange)
                    .textAttributes(key)
                    .create()
            }

            // ── Validate required block fields ────────────────────────────────
            is Tx3OutputBlockImpl -> validateOutputBlock(element, holder)
            is Tx3InputBlockImpl  -> validateInputBlock(element, holder)

            // ── Warn on unresolved name references ────────────────────────────
            is Tx3NameRefImpl     -> checkNameRef(element, holder)

            // ── Highlight call sites ──────────────────────────────────────────
            is Tx3CallExprImpl    -> highlightCallSite(element, holder)

            // BLOCK_FIELD and VARIANT_CASE map to ASTWrapperPsiElement —
            // match by node element type in a catch-all else branch.
            else -> when (element.node.elementType) {
                Tx3ElementTypes.BLOCK_FIELD,
                Tx3ElementTypes.VARIANT_CASE -> checkTrailingComma(element, holder)
                Tx3ElementTypes.SPREAD_EXPR -> checkSpreadTrailingComma(element, holder)
                else -> { /* no annotation needed */ }
            }
        }
    }

    // ── Declaration Name Highlighting ─────────────────────────────────────────

    private fun highlightDeclName(
        element: PsiElement,
        holder: AnnotationHolder,
        key: TextAttributesKey
    ) {
        // nameIdentifier now returns soft-keyword tokens too, so this always works
        val ident = (element as? Tx3NamedElementBase)?.nameIdentifier ?: return
        // Only recolor if it's actually a soft keyword token (not a plain IDENTIFIER —
        // those are already colored correctly by the syntax highlighter)
        if (key == Tx3SyntaxHighlighter.IDENTIFIER &&
            ident.node.elementType == Tx3TokenTypes.IDENTIFIER) return
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(ident.textRange)
            .textAttributes(key)
            .create()
    }

    // ── Output Block Validation ───────────────────────────────────────────────

    private fun validateOutputBlock(block: Tx3OutputBlockImpl, holder: AnnotationHolder) {
        val fields = block.fields().keys
        val missing = OUTPUT_REQUIRED_FIELDS - fields
        if (missing.isNotEmpty()) {
            val keyword = block.node.firstChildNode?.psi ?: block
            holder.newAnnotation(
                HighlightSeverity.WARNING,
                "Output block is missing required field(s): ${missing.joinToString(", ")}"
            ).range(keyword.textRange).create()
        }
    }

    // ── Input Block Validation ────────────────────────────────────────────────

    private fun validateInputBlock(block: Tx3InputBlockImpl, holder: AnnotationHolder) {
        val fields = block.fields().keys
        if ("from" !in fields && "ref" !in fields) {
            val keyword = block.node.firstChildNode?.psi ?: block
            holder.newAnnotation(
                HighlightSeverity.WARNING,
                "Input block must have either 'from' or 'ref' field"
            ).range(keyword.textRange).create()
        }
    }

    // ── Unresolved Name Reference ─────────────────────────────────────────────

    private fun checkNameRef(ref: Tx3NameRefImpl, holder: AnnotationHolder) {
        val name = ref.referencedName() ?: return
        if (name in BUILTIN_NAMES) return

        val file = ref.containingFile as? Tx3File ?: return

        // File-level names — cached per modification
        val fileLevelNames = CachedValuesManager.getCachedValue(file) {
            val names = buildSet {
                file.partyDeclarations().forEach  { it.name?.let { n -> add(n) } }
                file.policyDeclarations().forEach { it.name?.let { n -> add(n) } }
                file.recordDeclarations().forEach { it.name?.let { n -> add(n) } }
                file.typeDeclarations().forEach   { it.name?.let { n -> add(n) } }
                file.txDeclarations().forEach     { it.name?.let { n -> add(n) } }
                // Env block field names are global constants
                file.assetDeclarations().forEach { it.name?.let { n -> add(n) } }
                file.envFieldNames().forEach { add(it) }
            }
            CachedValueProvider.Result.create(names, PsiModificationTracker.MODIFICATION_COUNT)
        }
        if (name in fileLevelNames) return

        // Tx-local scope — cached per tx declaration
        val containingTx = PsiTreeUtil.getParentOfType(ref, Tx3TxDeclImpl::class.java)
        if (containingTx != null) {
            val txLocalNames = CachedValuesManager.getCachedValue(containingTx) {
                val names = buildSet {
                    containingTx.params().forEach      { it.name?.let { n -> add(n) } }
                    containingTx.inputBlocks().forEach  { it.name?.let { n -> add(n) } }
                    containingTx.outputBlocks().forEach { it.name?.let { n -> add(n) } }
                    PsiTreeUtil.findChildrenOfType(containingTx, Tx3LetBindingImpl::class.java)
                        .forEach { lb -> lb.name?.let { add(it) } }
                }
                CachedValueProvider.Result.create(names, PsiModificationTracker.MODIFICATION_COUNT)
            }
            if (name in txLocalNames) return
        }

        holder.newAnnotation(
            HighlightSeverity.WEAK_WARNING,
            "Unresolved reference: '$name'"
        ).range(ref.textRange).create()
    }

    // ── Call Site Highlighting ────────────────────────────────────────────────

    private fun highlightCallSite(call: Tx3CallExprImpl, holder: AnnotationHolder) {
        val callee = call.node.firstChildNode?.psi ?: return
        val key = if (callee.text in BUILTIN_NAMES) {
            Tx3SyntaxHighlighter.BUILTIN_SYMBOL
        } else {
            Tx3SyntaxHighlighter.IDENTIFIER
        }
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(callee.textRange)
            .textAttributes(key)
            .create()
    }

    // ── Trailing Comma Enforcement ───────────────────────────────────────────

    /**
     * Tx3 requires a trailing comma after every list item. Where the comma ends up
     * in the PSI tree depends on whether the parser consumed it before or after mark.done():
     *
     * - RECORD_FIELD, BLOCK_FIELD, LET_BINDING, VARIANT_CASE:
     *     comma consumed BEFORE mark.done() → comma is the LAST CHILD of the node
     * - TX_PARAM:
     *     comma consumed in the parent loop AFTER mark.done() → comma is a SIBLING
     *
     * So we check both: the last child first, then the next sibling.
     */
    private fun checkTrailingComma(element: PsiElement, holder: AnnotationHolder) {
        // Check the last non-whitespace child (covers RECORD_FIELD, BLOCK_FIELD, LET_BINDING, VARIANT_CASE)
        var child = element.node.lastChildNode
        while (child != null && child.elementType == com.intellij.psi.TokenType.WHITE_SPACE) {
            child = child.treePrev
        }
        if (child?.text == ",") return

        // Check the next non-whitespace sibling (covers TX_PARAM)
        var sibling = element.node.treeNext
        while (sibling != null && sibling.elementType == com.intellij.psi.TokenType.WHITE_SPACE) {
            sibling = sibling.treeNext
        }
        if (sibling?.text == ",") return

        holder.newAnnotation(
            HighlightSeverity.ERROR,
            "Missing trailing comma (required by Tx3)"
        )
            .range(element.textRange)
            .withFix(Tx3AddTrailingCommaFix(element))
            .create()
    }

    // ── Spread Trailing Comma Warning ──────────────────────────────────────────

    private fun checkSpreadTrailingComma(element: PsiElement, holder: AnnotationHolder) {
        var child = element.node.lastChildNode
        while (child != null && child.elementType == com.intellij.psi.TokenType.WHITE_SPACE) {
            child = child.treePrev
        }
        if (child?.text == ",") {
            holder.newAnnotation(
                HighlightSeverity.WARNING,
                "Trailing comma after spread operator is not supported by the upstream Tx3 compiler"
            ).range(child!!.textRange).create()
        }
    }

}