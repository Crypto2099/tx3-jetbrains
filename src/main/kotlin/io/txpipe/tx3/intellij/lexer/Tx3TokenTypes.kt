package io.txpipe.tx3.intellij.lexer

import com.intellij.psi.tree.IElementType
import io.txpipe.tx3.intellij.Tx3Language

class Tx3TokenType(debugName: String) : IElementType(debugName, Tx3Language)

object Tx3TokenTypes {

    // ── Comments ──────────────────────────────────────────────────────────────
    @JvmField val LINE_COMMENT  = Tx3TokenType("LINE_COMMENT")
    @JvmField val BLOCK_COMMENT = Tx3TokenType("BLOCK_COMMENT")

    // ── Top-level Declaration Keywords ────────────────────────────────────────
    @JvmField val KW_PARTY   = Tx3TokenType("party")
    @JvmField val KW_POLICY  = Tx3TokenType("policy")
    @JvmField val KW_RECORD  = Tx3TokenType("record")   // legacy alias — prefer 'type'
    @JvmField val KW_TYPE    = Tx3TokenType("type")
    @JvmField val KW_TX      = Tx3TokenType("tx")
    @JvmField val KW_ENV     = Tx3TokenType("env")
    @JvmField val KW_ASSET   = Tx3TokenType("asset")

    // ── Tx Body Block Keywords ────────────────────────────────────────────────
    @JvmField val KW_INPUT      = Tx3TokenType("input")
    @JvmField val KW_OUTPUT     = Tx3TokenType("output")
    @JvmField val KW_BURN       = Tx3TokenType("burn")
    @JvmField val KW_MINT       = Tx3TokenType("mint")
    @JvmField val KW_LOCALS     = Tx3TokenType("locals")
    @JvmField val KW_COLLATERAL = Tx3TokenType("collateral")
    @JvmField val KW_REFERENCE  = Tx3TokenType("reference")
    @JvmField val KW_SIGNERS    = Tx3TokenType("signers")
    @JvmField val KW_VALIDITY   = Tx3TokenType("validity")
    @JvmField val KW_METADATA   = Tx3TokenType("metadata")
    @JvmField val KW_CARDANO    = Tx3TokenType("cardano")

    // ── Field Keywords ────────────────────────────────────────────────────────
    @JvmField val KW_FROM        = Tx3TokenType("from")
    @JvmField val KW_TO          = Tx3TokenType("to")
    @JvmField val KW_AMOUNT      = Tx3TokenType("amount")
    @JvmField val KW_DATUM       = Tx3TokenType("datum")
    @JvmField val KW_DATUM_IS    = Tx3TokenType("datum_is")
    @JvmField val KW_REDEEMER    = Tx3TokenType("redeemer")
    @JvmField val KW_MIN_AMOUNT  = Tx3TokenType("min_amount")
    @JvmField val KW_REF         = Tx3TokenType("ref")
    @JvmField val KW_SCRIPT      = Tx3TokenType("script")
    @JvmField val KW_HASH        = Tx3TokenType("hash")
    @JvmField val KW_SINCE_SLOT  = Tx3TokenType("since_slot")
    @JvmField val KW_UNTIL_SLOT  = Tx3TokenType("until_slot")
    @JvmField val KW_DREP        = Tx3TokenType("drep")
    @JvmField val KW_STAKE       = Tx3TokenType("stake")
    @JvmField val KW_VERSION     = Tx3TokenType("version")
    @JvmField val KW_COIN        = Tx3TokenType("coin")

    // ── Control Keywords ──────────────────────────────────────────────────────
    @JvmField val KW_IMPORT = Tx3TokenType("import")
    @JvmField val KW_LET    = Tx3TokenType("let")
    @JvmField val KW_IF     = Tx3TokenType("if")
    @JvmField val KW_ELSE   = Tx3TokenType("else")
    @JvmField val KW_TRUE   = Tx3TokenType("true")
    @JvmField val KW_FALSE  = Tx3TokenType("false")

    // ── Built-in Types ────────────────────────────────────────────────────────
    @JvmField val TYPE_INT      = Tx3TokenType("Int")
    @JvmField val TYPE_BYTES    = Tx3TokenType("Bytes")
    @JvmField val TYPE_BOOL     = Tx3TokenType("Bool")
    @JvmField val TYPE_UNIT     = Tx3TokenType("Unit")
    @JvmField val TYPE_UTXO_REF = Tx3TokenType("UtxoRef")
    @JvmField val TYPE_ADDRESS  = Tx3TokenType("Address")
    @JvmField val TYPE_VALUE    = Tx3TokenType("Value")
    @JvmField val TYPE_LIST     = Tx3TokenType("List")
    @JvmField val TYPE_MAP      = Tx3TokenType("Map")

    // ── Built-in Constructors / Values ────────────────────────────────────────
    @JvmField val BUILTIN_ADA  = Tx3TokenType("Ada")
    @JvmField val BUILTIN_FEES = Tx3TokenType("fees")

    // ── Operators ─────────────────────────────────────────────────────────────
    @JvmField val OP_PLUS         = Tx3TokenType("+")
    @JvmField val OP_MINUS        = Tx3TokenType("-")
    @JvmField val OP_MUL          = Tx3TokenType("*")
    @JvmField val OP_DIV          = Tx3TokenType("/")
    @JvmField val OP_EQ           = Tx3TokenType("==")
    @JvmField val OP_NEQ          = Tx3TokenType("!=")
    @JvmField val OP_LT           = Tx3TokenType("<")
    @JvmField val OP_LE           = Tx3TokenType("<=")
    @JvmField val OP_GT           = Tx3TokenType(">")
    @JvmField val OP_GE           = Tx3TokenType(">=")
    @JvmField val OP_AND          = Tx3TokenType("&&")
    @JvmField val OP_OR           = Tx3TokenType("||")
    @JvmField val OP_NOT          = Tx3TokenType("!")
    @JvmField val OP_ASSIGN       = Tx3TokenType("=")
    @JvmField val OP_DOUBLE_COLON = Tx3TokenType("::")
    @JvmField val OP_SPREAD       = Tx3TokenType("...")

    // ── Punctuation ───────────────────────────────────────────────────────────
    @JvmField val LBRACE    = Tx3TokenType("{")
    @JvmField val RBRACE    = Tx3TokenType("}")
    @JvmField val LPAREN    = Tx3TokenType("(")
    @JvmField val RPAREN    = Tx3TokenType(")")
    @JvmField val LBRACKET  = Tx3TokenType("[")
    @JvmField val RBRACKET  = Tx3TokenType("]")
    @JvmField val COMMA     = Tx3TokenType(",")
    @JvmField val SEMICOLON = Tx3TokenType(";")
    @JvmField val COLON     = Tx3TokenType(":")
    @JvmField val DOT       = Tx3TokenType(".")

    // ── Literals ──────────────────────────────────────────────────────────────
    @JvmField val INT_LITERAL      = Tx3TokenType("INT_LITERAL")
    @JvmField val HEX_LITERAL      = Tx3TokenType("HEX_LITERAL")
    @JvmField val STRING_LITERAL   = Tx3TokenType("STRING_LITERAL")
    @JvmField val BYTES_LITERAL    = Tx3TokenType("BYTES_LITERAL")
    @JvmField val UTXO_REF_LITERAL = Tx3TokenType("UTXO_REF_LITERAL")
    @JvmField val ASSET_LITERAL    = Tx3TokenType("ASSET_LITERAL")

    // ── Identifier ────────────────────────────────────────────────────────────
    @JvmField val IDENTIFIER = Tx3TokenType("IDENTIFIER")

    // ── Convenience sets ─────────────────────────────────────────────────────

    val TOP_LEVEL_KEYWORDS = setOf(
        KW_PARTY, KW_POLICY, KW_RECORD, KW_TYPE, KW_TX, KW_ENV, KW_ASSET
    )

    val BLOCK_KEYWORDS = setOf(
        KW_INPUT, KW_OUTPUT, KW_BURN, KW_MINT, KW_LOCALS,
        KW_COLLATERAL, KW_REFERENCE, KW_SIGNERS, KW_VALIDITY, KW_METADATA, KW_CARDANO
    )

    val FIELD_KEYWORDS = setOf(
        KW_FROM, KW_TO, KW_AMOUNT, KW_DATUM, KW_DATUM_IS, KW_REDEEMER,
        KW_MIN_AMOUNT, KW_REF, KW_SCRIPT, KW_HASH, KW_SINCE_SLOT, KW_UNTIL_SLOT,
        KW_DREP, KW_STAKE, KW_VERSION, KW_COIN
    )

    val CONTROL_KEYWORDS = setOf(KW_IMPORT, KW_LET, KW_IF, KW_ELSE, KW_TRUE, KW_FALSE)

    val BUILTIN_TYPES = setOf(
        TYPE_INT, TYPE_BYTES, TYPE_BOOL, TYPE_UNIT,
        TYPE_UTXO_REF, TYPE_ADDRESS, TYPE_VALUE, TYPE_LIST, TYPE_MAP
    )

    val BUILTIN_SYMBOLS = setOf(BUILTIN_ADA, BUILTIN_FEES)

    val OPERATORS = setOf(
        OP_PLUS, OP_MINUS, OP_MUL, OP_DIV, OP_EQ, OP_NEQ,
        OP_LT, OP_LE, OP_GT, OP_GE, OP_AND, OP_OR, OP_NOT,
        OP_ASSIGN, OP_DOUBLE_COLON, OP_SPREAD
    )

    val LITERALS = setOf(
        INT_LITERAL, HEX_LITERAL, STRING_LITERAL,
        BYTES_LITERAL, UTXO_REF_LITERAL, ASSET_LITERAL
    )

    /**
     * Keywords that are only reserved as tx-body block introducers.
     * In all other positions (param names, field names, let bindings) they
     * behave as plain identifiers.
     */
    val SOFT_KEYWORD_TOKENS = setOf(
        KW_METADATA, KW_MINT, KW_COLLATERAL, KW_REFERENCE,
        KW_SIGNERS, KW_VALIDITY, KW_CARDANO, KW_ENV, KW_ASSET,
        KW_FROM, KW_TO, KW_AMOUNT, KW_DATUM, KW_REDEEMER,
        KW_MIN_AMOUNT, KW_REF, KW_HASH, KW_SCRIPT,
        KW_SINCE_SLOT, KW_UNTIL_SLOT, KW_DATUM_IS,
        KW_DREP, KW_STAKE, KW_VERSION, KW_COIN
    )
}