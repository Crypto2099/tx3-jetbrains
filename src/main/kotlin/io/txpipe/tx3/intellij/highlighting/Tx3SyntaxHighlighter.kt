package io.txpipe.tx3.intellij.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import io.txpipe.tx3.intellij.lexer.Tx3LexerAdapter
import io.txpipe.tx3.intellij.lexer.Tx3TokenTypes

/**
 * Defines all visual token categories for Tx3 syntax highlighting.
 *
 * Two layers of attributes are used:
 *  1. [TextAttributesKey] — logical key (mapped to colour in a scheme)
 *  2. Default fallback from [DefaultLanguageHighlighterColors]
 *
 * This lets any JetBrains theme automatically look decent, while the
 * dedicated Tx3 colour scheme files can override each key individually.
 */
object Tx3SyntaxHighlighter : SyntaxHighlighterBase() {

    // ── Attribute Keys ────────────────────────────────────────────────────────

    @JvmField
    val KEYWORD_DECL: TextAttributesKey = createTextAttributesKey(
        "TX3_KEYWORD_DECL",
        DefaultLanguageHighlighterColors.KEYWORD
    )

    @JvmField
    val KEYWORD_BLOCK: TextAttributesKey = createTextAttributesKey(
        "TX3_KEYWORD_BLOCK",
        DefaultLanguageHighlighterColors.KEYWORD
    )

    @JvmField
    val KEYWORD_FIELD: TextAttributesKey = createTextAttributesKey(
        "TX3_KEYWORD_FIELD",
        DefaultLanguageHighlighterColors.INSTANCE_METHOD
    )

    @JvmField
    val KEYWORD_CONTROL: TextAttributesKey = createTextAttributesKey(
        "TX3_KEYWORD_CONTROL",
        DefaultLanguageHighlighterColors.KEYWORD
    )

    @JvmField
    val BUILTIN_TYPE: TextAttributesKey = createTextAttributesKey(
        "TX3_BUILTIN_TYPE",
        DefaultLanguageHighlighterColors.CLASS_NAME
    )

    @JvmField
    val BUILTIN_SYMBOL: TextAttributesKey = createTextAttributesKey(
        "TX3_BUILTIN_SYMBOL",
        DefaultLanguageHighlighterColors.STATIC_FIELD
    )

    @JvmField
    val IDENTIFIER: TextAttributesKey = createTextAttributesKey(
        "TX3_IDENTIFIER",
        DefaultLanguageHighlighterColors.IDENTIFIER
    )

    @JvmField
    val OPERATOR: TextAttributesKey = createTextAttributesKey(
        "TX3_OPERATOR",
        DefaultLanguageHighlighterColors.OPERATION_SIGN
    )

    @JvmField
    val NUMBER: TextAttributesKey = createTextAttributesKey(
        "TX3_NUMBER",
        DefaultLanguageHighlighterColors.NUMBER
    )

    @JvmField
    val STRING: TextAttributesKey = createTextAttributesKey(
        "TX3_STRING",
        DefaultLanguageHighlighterColors.STRING
    )

    @JvmField
    val BYTES: TextAttributesKey = createTextAttributesKey(
        "TX3_BYTES",
        DefaultLanguageHighlighterColors.NUMBER
    )

    @JvmField
    val LINE_COMMENT: TextAttributesKey = createTextAttributesKey(
        "TX3_LINE_COMMENT",
        DefaultLanguageHighlighterColors.LINE_COMMENT
    )

    @JvmField
    val BLOCK_COMMENT: TextAttributesKey = createTextAttributesKey(
        "TX3_BLOCK_COMMENT",
        DefaultLanguageHighlighterColors.BLOCK_COMMENT
    )

    @JvmField
    val BRACES: TextAttributesKey = createTextAttributesKey(
        "TX3_BRACES",
        DefaultLanguageHighlighterColors.BRACES
    )

    @JvmField
    val BRACKETS: TextAttributesKey = createTextAttributesKey(
        "TX3_BRACKETS",
        DefaultLanguageHighlighterColors.BRACKETS
    )

    @JvmField
    val PARENS: TextAttributesKey = createTextAttributesKey(
        "TX3_PARENS",
        DefaultLanguageHighlighterColors.PARENTHESES
    )

    @JvmField
    val COMMA: TextAttributesKey = createTextAttributesKey(
        "TX3_COMMA",
        DefaultLanguageHighlighterColors.COMMA
    )

    @JvmField
    val SEMICOLON: TextAttributesKey = createTextAttributesKey(
        "TX3_SEMICOLON",
        DefaultLanguageHighlighterColors.SEMICOLON
    )

    @JvmField
    val COLON: TextAttributesKey = createTextAttributesKey(
        "TX3_COLON",
        DefaultLanguageHighlighterColors.DOT
    )

    @JvmField
    val BAD_CHARACTER: TextAttributesKey = createTextAttributesKey(
        "TX3_BAD_CHARACTER",
        HighlighterColors.BAD_CHARACTER
    )

    // ── Token → Attribute Mapping ─────────────────────────────────────────────

    private val TOKEN_MAP: Map<IElementType, Array<TextAttributesKey>> = buildMap {
        // Top-level declaration keywords
        for (t in Tx3TokenTypes.TOP_LEVEL_KEYWORDS) put(t, arrayOf(KEYWORD_DECL))

        // Block keywords
        for (t in Tx3TokenTypes.BLOCK_KEYWORDS) put(t, arrayOf(KEYWORD_BLOCK))

        // Field keywords
        for (t in Tx3TokenTypes.FIELD_KEYWORDS) put(t, arrayOf(KEYWORD_FIELD))

        // Control keywords
        for (t in Tx3TokenTypes.CONTROL_KEYWORDS) put(t, arrayOf(KEYWORD_CONTROL))

        // Built-in types
        for (t in Tx3TokenTypes.BUILTIN_TYPES) put(t, arrayOf(BUILTIN_TYPE))

        // Built-in symbols
        for (t in Tx3TokenTypes.BUILTIN_SYMBOLS) put(t, arrayOf(BUILTIN_SYMBOL))

        // Operators
        for (t in Tx3TokenTypes.OPERATORS) put(t, arrayOf(OPERATOR))

        // Literals
        put(Tx3TokenTypes.INT_LITERAL,      arrayOf(NUMBER))
        put(Tx3TokenTypes.HEX_LITERAL,      arrayOf(NUMBER))
        put(Tx3TokenTypes.UTXO_REF_LITERAL, arrayOf(NUMBER))
        put(Tx3TokenTypes.ASSET_LITERAL,    arrayOf(NUMBER))
        put(Tx3TokenTypes.STRING_LITERAL,   arrayOf(STRING))
        put(Tx3TokenTypes.BYTES_LITERAL,    arrayOf(BYTES))

        // Comments
        put(Tx3TokenTypes.LINE_COMMENT, arrayOf(LINE_COMMENT))
        put(Tx3TokenTypes.BLOCK_COMMENT, arrayOf(BLOCK_COMMENT))

        // Punctuation
        put(Tx3TokenTypes.LBRACE,   arrayOf(BRACES))
        put(Tx3TokenTypes.RBRACE,   arrayOf(BRACES))
        put(Tx3TokenTypes.LBRACKET, arrayOf(BRACKETS))
        put(Tx3TokenTypes.RBRACKET, arrayOf(BRACKETS))
        put(Tx3TokenTypes.LPAREN,   arrayOf(PARENS))
        put(Tx3TokenTypes.RPAREN,   arrayOf(PARENS))
        put(Tx3TokenTypes.COMMA,    arrayOf(COMMA))
        put(Tx3TokenTypes.SEMICOLON,arrayOf(SEMICOLON))
        put(Tx3TokenTypes.COLON,    arrayOf(COLON))
        put(Tx3TokenTypes.DOT,      arrayOf(COLON))

        // Identifiers
        put(Tx3TokenTypes.IDENTIFIER, arrayOf(IDENTIFIER))

        // Bad character
        put(com.intellij.psi.TokenType.BAD_CHARACTER, arrayOf(BAD_CHARACTER))
    }

    override fun getHighlightingLexer(): Lexer = Tx3LexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        TOKEN_MAP[tokenType] ?: TextAttributesKey.EMPTY_ARRAY
}