package io.txpipe.tx3.intellij.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import io.txpipe.tx3.intellij.lexer.Tx3TokenTypes

private val T = Tx3TokenTypes
private val E = Tx3ElementTypes

/**
 * Keywords that are only meaningful as block introducers inside a tx body.
 * They are valid identifiers everywhere else.
 */
private val SOFT_KEYWORDS = setOf(
    Tx3TokenTypes.KW_METADATA,
    Tx3TokenTypes.KW_MINT,
    Tx3TokenTypes.KW_COLLATERAL,
    Tx3TokenTypes.KW_REFERENCE,
    Tx3TokenTypes.KW_SIGNERS,
    Tx3TokenTypes.KW_VALIDITY,
    Tx3TokenTypes.KW_CARDANO,
    Tx3TokenTypes.KW_ENV,
    Tx3TokenTypes.KW_ASSET,
    Tx3TokenTypes.KW_FROM,
    Tx3TokenTypes.KW_TO,
    Tx3TokenTypes.KW_AMOUNT,
    Tx3TokenTypes.KW_DATUM,
    Tx3TokenTypes.KW_REDEEMER,
    Tx3TokenTypes.KW_MIN_AMOUNT,
    Tx3TokenTypes.KW_REF,
    Tx3TokenTypes.KW_HASH,
    Tx3TokenTypes.KW_SCRIPT,
    Tx3TokenTypes.KW_SINCE_SLOT,
    Tx3TokenTypes.KW_UNTIL_SLOT,
    Tx3TokenTypes.KW_DATUM_IS,
    Tx3TokenTypes.KW_DREP,
    Tx3TokenTypes.KW_STAKE,
    Tx3TokenTypes.KW_VERSION,
    Tx3TokenTypes.KW_COIN,
)

class Tx3Parser : PsiParser {

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMark = builder.mark()
        parseFile(builder)
        rootMark.done(root)
        return builder.treeBuilt
    }

    // ── File ──────────────────────────────────────────────────────────────────

    private fun parseFile(b: PsiBuilder) {
        while (!b.eof()) {
            val pos = b.currentOffset
            when (b.tokenType) {
                T.KW_PARTY   -> parsePartyDecl(b)
                T.KW_POLICY  -> parsePolicyDecl(b)
                T.KW_RECORD  -> parseTypeDecl(b, isLegacyRecord = true)
                T.KW_TYPE    -> parseTypeDecl(b, isLegacyRecord = false)
                T.KW_TX      -> parseTxDecl(b)
                T.KW_ENV     -> parseEnvDecl(b)
                T.KW_ASSET   -> parseAssetDecl(b)
                else -> {
                    b.error("Expected a top-level declaration (party, policy, type, tx, env, asset)")
                    b.advanceLexer()
                }
            }
            // Safety: if no token was consumed (parse failed silently), force-advance
            // to prevent infinite loops on malformed input
            if (b.currentOffset == pos && !b.eof()) b.advanceLexer()
        }
    }

    // ── Party ─────────────────────────────────────────────────────────────────

    private fun parsePartyDecl(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'party'
        if (!expect(b, T.IDENTIFIER, "Expected party name")) { mark.done(E.PARTY_DECL); return }
        expect(b, T.SEMICOLON, "Expected ';'")
        mark.done(E.PARTY_DECL)
    }

    // ── Policy ────────────────────────────────────────────────────────────────
    // Three forms:
    //   policy Name = import(path);
    //   policy Name = 0xABC;
    //   policy Name { hash: 0x..., script: 0x..., ref: 0x... }

    private fun parsePolicyDecl(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'policy'
        if (!expect(b, T.IDENTIFIER, "Expected policy name")) { mark.done(E.POLICY_DECL); return }
        when (b.tokenType) {
            T.OP_ASSIGN -> {
                b.advanceLexer()
                when (b.tokenType) {
                    T.KW_IMPORT -> {
                        val callMark = b.mark()
                        b.advanceLexer()
                        expect(b, T.LPAREN, "Expected '('")
                        parsePathExpr(b)
                        expect(b, T.RPAREN, "Expected ')'")
                        callMark.done(E.CALL_EXPR)
                    }
                    T.HEX_LITERAL -> b.advanceLexer()
                    else -> b.error("Expected 'import(path)' or hex literal for policy value")
                }
                expect(b, T.SEMICOLON, "Expected ';'")
            }
            T.LBRACE -> parseBlockBody(b) // policy { hash: ..., script: ..., ref: ... }
            else -> b.error("Expected '=' or '{'")
        }
        mark.done(E.POLICY_DECL)
    }

    private fun parsePathExpr(b: PsiBuilder) {
        val mark = b.mark()
        while (!b.eof() && b.tokenType != T.RPAREN) b.advanceLexer()
        mark.done(E.PATH_EXPR)
    }

    // ── Type / Record ─────────────────────────────────────────────────────────
    // Handles both `type` and legacy `record` keyword.
    // Distinguishes record-style (field: Type) from variant-style (Case { } or Case,).

    private fun parseTypeDecl(b: PsiBuilder, isLegacyRecord: Boolean) {
        val mark = b.mark()
        b.advanceLexer() // consume 'type' or 'record'
        if (!expect(b, T.IDENTIFIER, "Expected type name")) {
            mark.done(if (isLegacyRecord) E.RECORD_DECL else E.TYPE_DECL); return
        }
        if (!expect(b, T.LBRACE, "Expected '{'")) {
            mark.done(if (isLegacyRecord) E.RECORD_DECL else E.TYPE_DECL); return
        }

        // Discriminate: if first non-whitespace is IDENTIFIER followed by '{' or ',' or '}' → variant
        val isVariant = !isLegacyRecord && looksLikeVariantBody(b)

        if (isVariant) {
            while (!b.eof() && b.tokenType != T.RBRACE) {
                val pos = b.currentOffset
                parseVariantCase(b)
                if (b.currentOffset == pos && b.tokenType != T.RBRACE) b.advanceLexer()
            }
        } else {
            while (!b.eof() && b.tokenType != T.RBRACE) {
                val pos = b.currentOffset
                parseRecordField(b)
                if (b.currentOffset == pos && b.tokenType != T.RBRACE) b.advanceLexer()
            }
        }
        expect(b, T.RBRACE, "Expected '}'")
        mark.done(if (isLegacyRecord) E.RECORD_DECL else E.TYPE_DECL)
    }

    /** Returns true if the current position looks like variant cases rather than record fields. */
    private fun looksLikeVariantBody(b: PsiBuilder): Boolean {
        if (b.tokenType != T.IDENTIFIER) return false
        val next = b.lookAhead(1)
        return next == T.LBRACE || next == T.COMMA || next == T.RBRACE
    }

    private fun parseRecordField(b: PsiBuilder) {
        val mark = b.mark()
        if (!expectIdentifier(b, "Expected field name")) { mark.done(E.RECORD_FIELD); return }
        expect(b, T.COLON, "Expected ':'")
        parseTypeRef(b)
        if (b.tokenType == T.COMMA) b.advanceLexer()
        mark.done(E.RECORD_FIELD)
    }

    private fun parseVariantCase(b: PsiBuilder) {
        val mark = b.mark()
        if (!expectIdentifier(b, "Expected variant case name")) { mark.done(E.VARIANT_CASE); return }
        when (b.tokenType) {
            T.LBRACE -> parseRecordFieldBlock(b)
            else -> { /* unit case — no body */ }
        }
        if (b.tokenType == T.COMMA) b.advanceLexer()
        mark.done(E.VARIANT_CASE)
    }

    // ── Env ───────────────────────────────────────────────────────────────────

    private fun parseEnvDecl(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'env'
        if (b.tokenType != T.LBRACE) { b.error("Expected '{'"); mark.done(E.ENV_DECL); return }
        parseRecordFieldBlock(b) // consumes '{' ... '}'
        mark.done(E.ENV_DECL)
    }

    // ── Asset ─────────────────────────────────────────────────────────────────
    // asset StaticAsset = 0xABCDEF."MYTOKEN";
    // asset StaticAsset = 0xABCDEF;  (policy-only asset)

    private fun parseAssetDecl(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'asset'
        if (!expect(b, T.IDENTIFIER, "Expected asset name")) { mark.done(E.ASSET_DECL); return }
        if (!expect(b, T.OP_ASSIGN, "Expected '='")) { mark.done(E.ASSET_DECL); return }
        when (b.tokenType) {
            T.ASSET_LITERAL, T.HEX_LITERAL -> b.advanceLexer()
            else -> b.error("Expected asset identifier (hex literal or hex.\"token\")")
        }
        expect(b, T.SEMICOLON, "Expected ';'")
        mark.done(E.ASSET_DECL)
    }

    // ── Tx ────────────────────────────────────────────────────────────────────

    private fun parseTxDecl(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'tx'
        if (!expect(b, T.IDENTIFIER, "Expected transaction name")) { mark.done(E.TX_DECL); return }
        parseParamList(b)
        if (!expect(b, T.LBRACE, "Expected '{'")) { mark.done(E.TX_DECL); return }
        while (!b.eof() && b.tokenType != T.RBRACE) {
            val pos = b.currentOffset
            when (b.tokenType) {
                T.KW_INPUT      -> parseInputBlock(b)
                T.KW_OUTPUT     -> parseOutputBlock(b)
                T.KW_BURN       -> parseBurnBlock(b)
                T.KW_MINT       -> parseMintBlock(b)
                T.KW_LOCALS     -> parseLocalsBlock(b)
                T.KW_COLLATERAL -> parseCollateralBlock(b)
                T.KW_REFERENCE  -> parseReferenceBlock(b)
                T.KW_SIGNERS    -> parseSignersBlock(b)
                T.KW_VALIDITY   -> parseValidityBlock(b)
                T.KW_METADATA   -> parseMetadataBlock(b)
                T.KW_CARDANO    -> parseCardanoBlock(b)
                T.KW_LET        -> parseLetBinding(b)
                else -> {
                    b.error("Expected a tx body block (input, output, burn, mint, locals, collateral, reference, signers, validity, metadata, cardano, let)")
                    b.advanceLexer()
                }
            }
            if (b.currentOffset == pos && b.tokenType != T.RBRACE) b.advanceLexer()
        }
        expect(b, T.RBRACE, "Expected '}'")
        mark.done(E.TX_DECL)
    }

    private fun parseParamList(b: PsiBuilder) {
        val mark = b.mark()
        if (!expect(b, T.LPAREN, "Expected '('")) { mark.done(E.PARAM_LIST); return }
        while (!b.eof() && b.tokenType != T.RPAREN) {
            val pos = b.currentOffset
            parseTxParam(b)
            if (b.tokenType == T.COMMA) b.advanceLexer()
            if (b.currentOffset == pos && b.tokenType != T.RPAREN) b.advanceLexer()
        }
        expect(b, T.RPAREN, "Expected ')'")
        mark.done(E.PARAM_LIST)
    }

    private fun parseTxParam(b: PsiBuilder) {
        val mark = b.mark()
        if (!expectIdentifier(b, "Expected parameter name")) { mark.done(E.TX_PARAM); return }
        expect(b, T.COLON, "Expected ':'")
        parseTypeRef(b)
        mark.done(E.TX_PARAM)
    }

    // ── Tx Body Blocks ────────────────────────────────────────────────────────

    private fun parseInputBlock(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'input'
        if (b.tokenType == T.OP_MUL) b.advanceLexer() // consume optional '*' for input*
        if (b.tokenType == T.IDENTIFIER) b.advanceLexer()
        parseBlockBody(b)
        mark.done(E.INPUT_BLOCK)
    }

    private fun parseOutputBlock(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'output'
        if (b.tokenType == T.IDENTIFIER) b.advanceLexer()
        parseBlockBody(b)
        mark.done(E.OUTPUT_BLOCK)
    }

    private fun parseBurnBlock(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'burn'
        if (b.tokenType == T.IDENTIFIER) b.advanceLexer()
        parseBlockBody(b)
        mark.done(E.BURN_BLOCK)
    }

    private fun parseMintBlock(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'mint'
        if (b.tokenType == T.IDENTIFIER) b.advanceLexer()
        parseBlockBody(b)
        mark.done(E.MINT_BLOCK)
    }

    private fun parseCollateralBlock(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'collateral'
        parseBlockBody(b)
        mark.done(E.COLLATERAL_BLOCK)
    }

    private fun parseReferenceBlock(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'reference'
        if (b.tokenType == T.IDENTIFIER) b.advanceLexer()
        parseBlockBody(b)
        mark.done(E.REFERENCE_BLOCK)
    }

    // signers { MyParty, 0xABC, } — bare expressions, no key:value
    private fun parseSignersBlock(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'signers'
        if (!expect(b, T.LBRACE, "Expected '{'")) { mark.done(E.SIGNERS_BLOCK); return }
        while (!b.eof() && b.tokenType != T.RBRACE) {
            val pos = b.currentOffset
            parseExpr(b)
            if (b.tokenType == T.COMMA) b.advanceLexer()
            if (b.currentOffset == pos && b.tokenType != T.RBRACE) b.advanceLexer()
        }
        expect(b, T.RBRACE, "Expected '}'")
        mark.done(E.SIGNERS_BLOCK)
    }

    private fun parseValidityBlock(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'validity'
        parseBlockBody(b)
        mark.done(E.VALIDITY_BLOCK)
    }

    // metadata { 1: expr, 2: expr } — integer keys
    private fun parseMetadataBlock(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'metadata'
        if (!expect(b, T.LBRACE, "Expected '{'")) { mark.done(E.METADATA_BLOCK); return }
        while (!b.eof() && b.tokenType != T.RBRACE) {
            val pos = b.currentOffset
            parseMetadataField(b)
            if (b.currentOffset == pos && b.tokenType != T.RBRACE) b.advanceLexer()
        }
        expect(b, T.RBRACE, "Expected '}'")
        mark.done(E.METADATA_BLOCK)
    }

    private fun parseMetadataField(b: PsiBuilder) {
        val mark = b.mark()
        // key can be integer or string literal
        if (b.tokenType == T.INT_LITERAL || b.tokenType == T.STRING_LITERAL) {
            b.advanceLexer()
        } else {
            b.error("Expected integer or string key in metadata block")
        }
        expect(b, T.COLON, "Expected ':'")
        parseExpr(b)
        if (b.tokenType == T.COMMA) b.advanceLexer()
        mark.done(E.BLOCK_FIELD)
    }

    // cardano::block_name { ... }
    private fun parseCardanoBlock(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'cardano'
        if (!expect(b, T.OP_DOUBLE_COLON, "Expected '::'")) { mark.done(E.CARDANO_BLOCK); return }
        if (!expect(b, T.IDENTIFIER, "Expected cardano block name")) { mark.done(E.CARDANO_BLOCK); return }
        parseBlockBody(b)
        mark.done(E.CARDANO_BLOCK)
    }

    // ── Locals Block ──────────────────────────────────────────────────────────

    private fun parseLocalsBlock(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'locals'
        if (!expect(b, T.LBRACE, "Expected '{'")) { mark.done(E.LOCALS_BLOCK); return }
        while (!b.eof() && b.tokenType != T.RBRACE) {
            val pos = b.currentOffset
            parseLocalsField(b)
            if (b.currentOffset == pos && b.tokenType != T.RBRACE) b.advanceLexer()
        }
        expect(b, T.RBRACE, "Expected '}'")
        mark.done(E.LOCALS_BLOCK)
    }

    private fun parseLocalsField(b: PsiBuilder) {
        val mark = b.mark()
        if (!expectIdentifier(b, "Expected local name")) { mark.done(E.LET_BINDING); return }
        expect(b, T.COLON, "Expected ':'")
        parseExpr(b)
        if (b.tokenType == T.COMMA) b.advanceLexer()
        mark.done(E.LET_BINDING)
    }

    // ── Generic Block Body ────────────────────────────────────────────────────

    private fun parseBlockBody(b: PsiBuilder) {
        if (!expect(b, T.LBRACE, "Expected '{'")) return
        while (!b.eof() && b.tokenType != T.RBRACE) {
            val pos = b.currentOffset
            parseBlockField(b)
            if (b.currentOffset == pos && b.tokenType != T.RBRACE) b.advanceLexer()
        }
        expect(b, T.RBRACE, "Expected '}'")
    }

    private val allFieldKeywords = Tx3TokenTypes.FIELD_KEYWORDS

    private fun parseBlockField(b: PsiBuilder) {
        val mark = b.mark()
        if (b.tokenType !in allFieldKeywords && b.tokenType != T.IDENTIFIER) {
            b.error("Expected a field keyword or identifier")
            if (!b.eof()) b.advanceLexer()
            mark.done(E.BLOCK_FIELD)
            return
        }
        b.advanceLexer() // consume field keyword or identifier
        expect(b, T.COLON, "Expected ':'")
        parseExpr(b)
        if (b.tokenType == T.COMMA) b.advanceLexer()
        mark.done(E.BLOCK_FIELD)
    }

    // ── Let Binding ───────────────────────────────────────────────────────────

    private fun parseLetBinding(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume 'let'
        if (!expectIdentifier(b, "Expected variable name")) { mark.done(E.LET_BINDING); return }
        expect(b, T.OP_ASSIGN, "Expected '='")
        parseExpr(b)
        expect(b, T.SEMICOLON, "Expected ';'")
        mark.done(E.LET_BINDING)
    }

    // ── Expressions ───────────────────────────────────────────────────────────

    private fun parseExpr(b: PsiBuilder) = parseLogicalExpr(b)

    // logical: &&, ||
    private fun parseLogicalExpr(b: PsiBuilder) {
        val mark = b.mark()
        parseCompareExpr(b)
        if (b.tokenType == T.OP_AND || b.tokenType == T.OP_OR) {
            while (b.tokenType == T.OP_AND || b.tokenType == T.OP_OR) {
                b.advanceLexer()
                parseCompareExpr(b)
            }
            mark.done(E.BINARY_EXPR)
        } else {
            mark.drop()
        }
    }

    // comparison: ==, !=, <, >, <=, >=
    private fun parseCompareExpr(b: PsiBuilder) {
        val mark = b.mark()
        parseAddExpr(b)
        if (b.tokenType == T.OP_EQ  || b.tokenType == T.OP_NEQ ||
            b.tokenType == T.OP_LT  || b.tokenType == T.OP_GT  ||
            b.tokenType == T.OP_LE  || b.tokenType == T.OP_GE) {
            while (b.tokenType == T.OP_EQ  || b.tokenType == T.OP_NEQ ||
                b.tokenType == T.OP_LT  || b.tokenType == T.OP_GT  ||
                b.tokenType == T.OP_LE  || b.tokenType == T.OP_GE) {
                b.advanceLexer()
                parseAddExpr(b)
            }
            mark.done(E.BINARY_EXPR)
        } else {
            mark.drop()
        }
    }

    private fun parseAddExpr(b: PsiBuilder) {
        val mark = b.mark()
        parseMulExpr(b)
        if (b.tokenType == T.OP_PLUS || b.tokenType == T.OP_MINUS) {
            while (b.tokenType == T.OP_PLUS || b.tokenType == T.OP_MINUS) {
                b.advanceLexer()
                parseMulExpr(b)
            }
            mark.done(E.BINARY_EXPR)
        } else {
            mark.drop() // no operator — don't wrap in a redundant node
        }
    }

    private fun parseMulExpr(b: PsiBuilder) {
        val mark = b.mark()
        parseUnaryExpr(b)
        if (b.tokenType == T.OP_MUL || b.tokenType == T.OP_DIV) {
            while (b.tokenType == T.OP_MUL || b.tokenType == T.OP_DIV) {
                b.advanceLexer()
                parseUnaryExpr(b)
            }
            mark.done(E.BINARY_EXPR)
        } else {
            mark.drop() // no operator — don't wrap in a redundant node
        }
    }

    private fun parseUnaryExpr(b: PsiBuilder) {
        if (b.tokenType == T.OP_NOT || b.tokenType == T.OP_MINUS) {
            val mark = b.mark()
            b.advanceLexer()
            parsePostfixExpr(b)
            mark.done(E.UNARY_EXPR)
        } else {
            parsePostfixExpr(b)
        }
    }

    // Postfix handles field access: expr.field
    private fun parsePostfixExpr(b: PsiBuilder) {
        parsePrimaryExpr(b)
        while (b.tokenType == T.DOT) {
            val mark = b.mark()
            b.advanceLexer() // consume '.'
            expect(b, T.IDENTIFIER, "Expected field name after '.'")
            mark.done(E.FIELD_ACCESS_EXPR)
        }
    }

    private fun parsePrimaryExpr(b: PsiBuilder) {
        when {
            // Spread: ...source
            b.tokenType == T.OP_SPREAD -> {
                val mark = b.mark()
                b.advanceLexer()
                if (b.tokenType == T.IDENTIFIER) b.advanceLexer()
                mark.done(E.SPREAD_EXPR)
            }

            // Unit or parenthesised expression: ()  or  (expr)
            b.tokenType == T.LPAREN -> {
                b.advanceLexer()
                if (b.tokenType == T.RPAREN) { b.advanceLexer(); return }
                parseExpr(b)
                expect(b, T.RPAREN, "Expected ')'")
            }

            // List literal: [expr, expr, ...]
            b.tokenType == T.LBRACKET -> {
                val mark = b.mark()
                b.advanceLexer()
                while (!b.eof() && b.tokenType != T.RBRACKET) {
                    val pos = b.currentOffset
                    parseExpr(b)
                    if (b.tokenType == T.COMMA) b.advanceLexer()
                    if (b.currentOffset == pos && b.tokenType != T.RBRACKET) b.advanceLexer()
                }
                expect(b, T.RBRACKET, "Expected ']'")
                mark.done(E.LIST_EXPR)
            }

            // Map literal: {key: val, key: val}
            // Only when not preceded by a type name (record literals are handled below)
            b.tokenType == T.LBRACE -> {
                val mark = b.mark()
                b.advanceLexer()
                while (!b.eof() && b.tokenType != T.RBRACE) {
                    val pos = b.currentOffset
                    parseExpr(b) // key
                    expect(b, T.COLON, "Expected ':'")
                    parseExpr(b) // value
                    if (b.tokenType == T.COMMA) b.advanceLexer()
                    if (b.currentOffset == pos && b.tokenType != T.RBRACE) b.advanceLexer()
                }
                expect(b, T.RBRACE, "Expected '}'")
                mark.done(E.MAP_EXPR)
            }

            // Variant construction: TypeName::CaseName { ... } or TypeName::CaseName
            isIdentifierOrSoftKeyword(b) && b.lookAhead(1) == T.OP_DOUBLE_COLON -> {
                val mark = b.mark()
                b.advanceLexer() // type name
                b.advanceLexer() // ::
                expect(b, T.IDENTIFIER, "Expected variant case name")
                if (b.tokenType == T.LBRACE) parseRecordFieldInitBlock(b)
                mark.done(E.VARIANT_EXPR)
            }

            // Call or record literal: Name(...) or Name { ... }
            (b.tokenType == T.BUILTIN_ADA || isIdentifierOrSoftKeyword(b)) && peekIsLBraceOrLParen(b) -> {
                parseCallOrRecordLiteral(b)
            }

            // Plain name reference
            isIdentifierOrSoftKeyword(b) || b.tokenType == T.BUILTIN_FEES -> {
                val mark = b.mark()
                b.advanceLexer()
                mark.done(E.NAME_REF)
            }

            // Literals
            b.tokenType in Tx3TokenTypes.LITERALS -> {
                val mark = b.mark()
                b.advanceLexer()
                mark.done(E.LITERAL)
            }

            b.tokenType == T.KW_TRUE || b.tokenType == T.KW_FALSE -> {
                val mark = b.mark()
                b.advanceLexer()
                mark.done(E.LITERAL)
            }

            else -> {
                b.error("Expected an expression")
                if (!b.eof()) b.advanceLexer()
            }
        }
    }

    private fun parseCallOrRecordLiteral(b: PsiBuilder) {
        val mark = b.mark()
        b.advanceLexer() // consume callee name
        if (b.tokenType == T.LBRACE) {
            parseRecordFieldInitBlock(b)
            mark.done(E.RECORD_LITERAL)
        } else {
            parseArgList(b)
            mark.done(E.CALL_EXPR)
        }
    }

    private fun parseRecordFieldInitOrSpread(b: PsiBuilder) {
        if (b.tokenType == T.OP_SPREAD) {
            val mark = b.mark()
            b.advanceLexer() // consume '...'
            if (b.tokenType == T.IDENTIFIER) b.advanceLexer()
            if (b.tokenType == T.COMMA) b.advanceLexer()
            mark.done(E.SPREAD_EXPR)
        } else {
            parseRecordFieldInit(b)
        }
    }

    private fun parseRecordFieldInit(b: PsiBuilder) {
        val mark = b.mark()
        if (isIdentifierOrSoftKeyword(b)) b.advanceLexer() else {
            b.error("Expected field name")
            mark.done(E.RECORD_FIELD_INIT); return
        }
        expect(b, T.COLON, "Expected ':'")
        parseExpr(b)
        if (b.tokenType == T.COMMA) b.advanceLexer()
        mark.done(E.RECORD_FIELD_INIT)
    }

    private fun parseArgList(b: PsiBuilder) {
        val mark = b.mark()
        expect(b, T.LPAREN, "Expected '('")
        while (!b.eof() && b.tokenType != T.RPAREN) {
            val pos = b.currentOffset
            parseExpr(b)
            if (b.tokenType == T.COMMA) b.advanceLexer()
            if (b.currentOffset == pos && b.tokenType != T.RPAREN) b.advanceLexer()
        }
        expect(b, T.RPAREN, "Expected ')'")
        mark.done(E.ARG_LIST)
    }

    // ── Type References ───────────────────────────────────────────────────────

    private val builtinTypeTokens = setOf(
        T.TYPE_INT, T.TYPE_BYTES, T.TYPE_BOOL, T.TYPE_UNIT,
        T.TYPE_UTXO_REF, T.TYPE_ADDRESS, T.TYPE_VALUE
    )

    /** Parses a brace-delimited block of record fields: `{ field: Type, ... }` */
    private fun parseRecordFieldBlock(b: PsiBuilder) {
        b.advanceLexer() // consume '{'
        while (!b.eof() && b.tokenType != T.RBRACE) {
            val pos = b.currentOffset
            parseRecordField(b)
            if (b.currentOffset == pos && b.tokenType != T.RBRACE) b.advanceLexer()
        }
        expect(b, T.RBRACE, "Expected '}'")
    }

    /** Parses a brace-delimited block of record field inits/spreads: `{ field: expr, ...spread }` */
    private fun parseRecordFieldInitBlock(b: PsiBuilder) {
        b.advanceLexer() // consume '{'
        while (!b.eof() && b.tokenType != T.RBRACE) {
            val pos = b.currentOffset
            parseRecordFieldInitOrSpread(b)
            if (b.currentOffset == pos && b.tokenType != T.RBRACE) b.advanceLexer()
        }
        expect(b, T.RBRACE, "Expected '}'")
    }

    private fun parseTypeRef(b: PsiBuilder) {
        val mark = b.mark()
        when (b.tokenType) {
            // [Type] — list type
            T.LBRACKET -> {
                b.advanceLexer()
                parseTypeRef(b)
                expect(b, T.RBRACKET, "Expected ']'")
                mark.done(E.LIST_TYPE)
            }
            // List<Type>
            T.TYPE_LIST -> {
                b.advanceLexer()
                if (b.tokenType == T.OP_LT) {
                    b.advanceLexer()
                    parseTypeRef(b)
                    expect(b, T.OP_GT, "Expected '>'")
                }
                mark.done(E.GENERIC_TYPE)
            }
            // Map<KeyType, ValType>
            T.TYPE_MAP -> {
                b.advanceLexer()
                if (b.tokenType == T.OP_LT) {
                    b.advanceLexer()
                    parseTypeRef(b)
                    if (b.tokenType == T.COMMA) b.advanceLexer()
                    parseTypeRef(b)
                    expect(b, T.OP_GT, "Expected '>'")
                }
                mark.done(E.MAP_TYPE)
            }
            // Builtin scalar types or user-defined type name
            in builtinTypeTokens, T.IDENTIFIER -> {
                b.advanceLexer()
                mark.done(E.TYPE_REF)
            }
            else -> {
                b.error("Expected a type name")
                mark.done(E.TYPE_REF)
            }
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private fun expect(b: PsiBuilder, token: IElementType, error: String): Boolean {
        return if (b.tokenType == token) { b.advanceLexer(); true }
        else { b.error(error); false }
    }

    /**
     * Accepts IDENTIFIER or any soft keyword. Soft keywords are only reserved
     * as tx-body block introducers — they are valid as parameter names, field
     * names, let-binding names, and any other identifier position.
     */
    private fun expectIdentifier(b: PsiBuilder, error: String): Boolean {
        return if (b.tokenType == T.IDENTIFIER || b.tokenType in SOFT_KEYWORDS) {
            b.advanceLexer(); true
        } else {
            b.error(error); false
        }
    }

    private fun isIdentifierOrSoftKeyword(b: PsiBuilder): Boolean =
        b.tokenType == T.IDENTIFIER || b.tokenType in SOFT_KEYWORDS

    private fun peekIsLBraceOrLParen(b: PsiBuilder): Boolean {
        val next = b.lookAhead(1)
        return next == T.LPAREN || next == T.LBRACE
    }


}