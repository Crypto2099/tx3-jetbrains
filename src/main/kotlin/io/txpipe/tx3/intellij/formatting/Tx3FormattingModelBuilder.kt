package io.txpipe.tx3.intellij.formatting

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import com.intellij.psi.formatter.common.AbstractBlock
import io.txpipe.tx3.intellij.Tx3Language
import io.txpipe.tx3.intellij.lexer.Tx3TokenTypes
import io.txpipe.tx3.intellij.parser.Tx3ElementTypes

class Tx3FormattingModelBuilder : FormattingModelBuilder {

    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val file = formattingContext.psiElement.containingFile
        val spacingBuilder = createSpacingBuilder(settings)
        val rootBlock = Tx3Block(
            node = formattingContext.psiElement.node,
            wrap = null,
            alignment = null,
            indent = Indent.getNoneIndent(),
            spacingBuilder = spacingBuilder,
        )
        return FormattingModelProvider.createFormattingModelForPsiFile(file, rootBlock, settings)
    }

    private fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder =
        SpacingBuilder(settings, Tx3Language)
            // ── Top-level declaration keywords ────────────────────────────
            .after(Tx3TokenTypes.KW_PARTY)      .spaces(1)
            .after(Tx3TokenTypes.KW_POLICY)     .spaces(1)
            .after(Tx3TokenTypes.KW_RECORD)     .spaces(1)
            .after(Tx3TokenTypes.KW_TYPE)       .spaces(1)
            .after(Tx3TokenTypes.KW_TX)         .spaces(1)
            .after(Tx3TokenTypes.KW_ENV)        .spaces(1)
            .after(Tx3TokenTypes.KW_ASSET)      .spaces(1)

            // ── Tx body block keywords ────────────────────────────────────
            .after(Tx3TokenTypes.KW_INPUT)      .spaces(1)
            .after(Tx3TokenTypes.KW_OUTPUT)     .spaces(1)
            .after(Tx3TokenTypes.KW_BURN)       .spaces(1)
            .after(Tx3TokenTypes.KW_MINT)       .spaces(1)
            .after(Tx3TokenTypes.KW_LOCALS)     .spaces(1)
            .after(Tx3TokenTypes.KW_COLLATERAL) .spaces(1)
            .after(Tx3TokenTypes.KW_REFERENCE)  .spaces(1)
            .after(Tx3TokenTypes.KW_SIGNERS)    .spaces(1)
            .after(Tx3TokenTypes.KW_VALIDITY)   .spaces(1)
            .after(Tx3TokenTypes.KW_METADATA)   .spaces(1)
            .after(Tx3TokenTypes.KW_LET)        .spaces(1)
            .after(Tx3TokenTypes.KW_IMPORT)     .spaces(0)

            // ── Cardano namespace operator ────────────────────────────────
            .around(Tx3TokenTypes.OP_DOUBLE_COLON) .spaces(0)

            // ── Around binary operators ───────────────────────────────────
            .around(Tx3TokenTypes.OP_PLUS)      .spaces(1)
            .around(Tx3TokenTypes.OP_MINUS)     .spaces(1)
            .around(Tx3TokenTypes.OP_MUL)       .spaces(1)
            .around(Tx3TokenTypes.OP_DIV)       .spaces(1)
            .around(Tx3TokenTypes.OP_ASSIGN)    .spaces(1)
            .around(Tx3TokenTypes.OP_EQ)        .spaces(1)
            .around(Tx3TokenTypes.OP_NEQ)       .spaces(1)
            .around(Tx3TokenTypes.OP_LT)        .spaces(1)
            .around(Tx3TokenTypes.OP_LE)        .spaces(1)
            .around(Tx3TokenTypes.OP_GT)        .spaces(1)
            .around(Tx3TokenTypes.OP_GE)        .spaces(1)
            .around(Tx3TokenTypes.OP_AND)       .spaces(1)
            .around(Tx3TokenTypes.OP_OR)        .spaces(1)

            // ── Spread: no space between ... and name ─────────────────────
            .after(Tx3TokenTypes.OP_SPREAD)     .spaces(0)

            // ── Dot access: no spaces around '.' ─────────────────────────
            .around(Tx3TokenTypes.DOT)          .spaces(0)

            // ── Colon: no space before, one after ────────────────────────
            .before(Tx3TokenTypes.COLON)        .none()
            .after(Tx3TokenTypes.COLON)         .spaces(1)

            // ── Comma: no space before, one after ────────────────────────
            .before(Tx3TokenTypes.COMMA)        .none()
            .after(Tx3TokenTypes.COMMA)         .spaces(1)

            // ── Semicolon: no space before ────────────────────────────────
            .before(Tx3TokenTypes.SEMICOLON)    .none()

            // ── Parens: no space immediately inside ──────────────────────
            .after(Tx3TokenTypes.LPAREN)        .none()
            .before(Tx3TokenTypes.RPAREN)       .none()

            // ── Brackets: no space immediately inside ─────────────────────
            .after(Tx3TokenTypes.LBRACKET)      .none()
            .before(Tx3TokenTypes.RBRACKET)     .none()

            // ── Space before opening brace ────────────────────────────────
            .before(Tx3TokenTypes.LBRACE)       .spaces(1)

}

// ── Formatting constants (file-level so they are allocated once, not per block) ──

private val INDENTED_CONTAINERS = setOf(
    Tx3ElementTypes.TX_DECL,
    Tx3ElementTypes.RECORD_DECL,
    Tx3ElementTypes.TYPE_DECL,
    Tx3ElementTypes.ENV_DECL,
    Tx3ElementTypes.INPUT_BLOCK,
    Tx3ElementTypes.OUTPUT_BLOCK,
    Tx3ElementTypes.BURN_BLOCK,
    Tx3ElementTypes.MINT_BLOCK,
    Tx3ElementTypes.LOCALS_BLOCK,
    Tx3ElementTypes.COLLATERAL_BLOCK,
    Tx3ElementTypes.REFERENCE_BLOCK,
    Tx3ElementTypes.SIGNERS_BLOCK,
    Tx3ElementTypes.VALIDITY_BLOCK,
    Tx3ElementTypes.METADATA_BLOCK,
    Tx3ElementTypes.CARDANO_BLOCK,
    Tx3ElementTypes.ARG_LIST,
    Tx3ElementTypes.PARAM_LIST,
    Tx3ElementTypes.POLICY_DECL,
    Tx3ElementTypes.VARIANT_CASE,
    Tx3ElementTypes.RECORD_LITERAL,
    Tx3ElementTypes.VARIANT_EXPR,
)

private val STRUCTURAL_CHILD_TOKENS = setOf(
    Tx3TokenTypes.LBRACE,
    Tx3TokenTypes.RBRACE,
    Tx3TokenTypes.KW_TX,
    Tx3TokenTypes.KW_TYPE,
    Tx3TokenTypes.KW_RECORD,
    Tx3TokenTypes.KW_ENV,
    Tx3TokenTypes.KW_ASSET,
    Tx3TokenTypes.KW_POLICY,
    Tx3TokenTypes.KW_INPUT,
    Tx3TokenTypes.KW_OUTPUT,
    Tx3TokenTypes.KW_BURN,
    Tx3TokenTypes.KW_MINT,
    Tx3TokenTypes.KW_LOCALS,
    Tx3TokenTypes.KW_COLLATERAL,
    Tx3TokenTypes.KW_REFERENCE,
    Tx3TokenTypes.KW_SIGNERS,
    Tx3TokenTypes.KW_VALIDITY,
    Tx3TokenTypes.KW_METADATA,
    Tx3TokenTypes.KW_CARDANO,
    Tx3TokenTypes.IDENTIFIER,
    Tx3TokenTypes.LPAREN,
    Tx3TokenTypes.RPAREN,
)

// ── Block ─────────────────────────────────────────────────────────────────────

private class Tx3Block(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    private val indent: Indent,
    private val spacingBuilder: SpacingBuilder,
) : AbstractBlock(node, wrap, alignment) {

    override fun buildChildren(): List<Block> {
        val children = mutableListOf<Block>()
        var child: ASTNode? = node.firstChildNode
        while (child != null) {
            if (child.elementType != com.intellij.psi.TokenType.WHITE_SPACE) {
                children.add(
                    Tx3Block(
                        node = child,
                        wrap = null,
                        alignment = null,
                        indent = computeChildIndent(child),
                        spacingBuilder = spacingBuilder,
                    )
                )
            }
            child = child.treeNext
        }
        return children
    }

    private fun computeChildIndent(child: ASTNode): Indent {
        val parentType = node.elementType
        val childType = child.elementType

        if (childType in STRUCTURAL_CHILD_TOKENS) return Indent.getNoneIndent()
        if (parentType in INDENTED_CONTAINERS) return Indent.getNormalIndent()

        return Indent.getNoneIndent()
    }

    override fun getIndent(): Indent = indent

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        val t1 = (child1 as? Tx3Block)?.node?.elementType
        val t2 = (child2 as? Tx3Block)?.node?.elementType
        // Suppress spaces around < > ONLY when both neighbours are type-system tokens.
        // This covers List<Int>, Map<K,V> while leaving 1 < 2 untouched.
        if ((t1 == Tx3TokenTypes.OP_LT || t2 == Tx3TokenTypes.OP_GT || t2 == Tx3TokenTypes.OP_LT) &&
            isTypeContext(t1, t2)) {
            return Spacing.createSpacing(0, 0, 0, false, 0)
        }
        return spacingBuilder.getSpacing(this, child1, child2)
    }

    /** Returns true when the tokens on either side of < or > are type-system tokens,
     *  not expression tokens. Prevents stripping spaces from binary comparisons. */
    private fun isTypeContext(t1: com.intellij.psi.tree.IElementType?,
                              t2: com.intellij.psi.tree.IElementType?): Boolean {
        val typeTokens = setOf(
            Tx3TokenTypes.TYPE_INT, Tx3TokenTypes.TYPE_BYTES, Tx3TokenTypes.TYPE_BOOL,
            Tx3TokenTypes.TYPE_UNIT, Tx3TokenTypes.TYPE_UTXO_REF, Tx3TokenTypes.TYPE_ADDRESS,
            Tx3TokenTypes.TYPE_VALUE, Tx3TokenTypes.TYPE_LIST, Tx3TokenTypes.TYPE_MAP,
            Tx3ElementTypes.TYPE_REF, Tx3ElementTypes.LIST_TYPE,
            Tx3ElementTypes.MAP_TYPE, Tx3ElementTypes.GENERIC_TYPE,
        )
        // At least one neighbour must be an actual type token (not < > or comma themselves)
        // so that comparison operators like 1 < 2 don't get their spaces stripped
        return t1 in typeTokens || t2 in typeTokens
    }

    override fun isLeaf(): Boolean = node.firstChildNode == null
}

// ── Code Style Settings ───────────────────────────────────────────────────────

class Tx3LanguageCodeStyleSettingsProvider :
    LanguageCodeStyleSettingsProvider() {

    override fun getLanguage() = Tx3Language

    @Deprecated("Deprecated in Java")
    override fun getDefaultCommonSettings(): com.intellij.psi.codeStyle.CommonCodeStyleSettings {
        return com.intellij.psi.codeStyle.CommonCodeStyleSettings(Tx3Language).also { s ->
            val indent = s.initIndentOptions()
            indent.INDENT_SIZE = 4
            indent.TAB_SIZE = 4
            indent.CONTINUATION_INDENT_SIZE = 4
            indent.USE_TAB_CHARACTER = false
        }
    }

    override fun getCodeSample(settingsType: SettingsType): String = """
        party Sender;
        party Receiver;

        type Transfer {
          from: Address,
          to: Address,
          amount: Int,
        }

        tx transfer(quantity: Int) {
          input source {
            from: Sender,
            min_amount: Ada(quantity),
          }
          output {
            to: Receiver,
            amount: Ada(quantity),
          }
          output {
            to: Sender,
            amount: source - Ada(quantity) - fees,
          }
        }
    """.trimIndent()
}