package io.txpipe.tx3.intellij.parser

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import io.txpipe.tx3.intellij.Tx3Language

class Tx3ElementType(debugName: String) : IElementType(debugName, Tx3Language)

object Tx3ElementTypes {

    // ── File Root ─────────────────────────────────────────────────────────────
    @JvmField val FILE = IFileElementType(Tx3Language)

    // ── Top-level Declarations ────────────────────────────────────────────────
    /** `party Foo;` */
    @JvmField val PARTY_DECL = Tx3ElementType("PARTY_DECL")

    /** `policy Bar = import(...);` or `policy Bar = 0xABC;` or `policy Bar { ... }` */
    @JvmField val POLICY_DECL = Tx3ElementType("POLICY_DECL")

    /** `record State { ... }` — legacy keyword, maps to same node as TYPE_DECL */
    @JvmField val RECORD_DECL = Tx3ElementType("RECORD_DECL")

    /** `type MyRecord { field: Type, }` or `type MyVariant { Case1 { }, Case2, }` */
    @JvmField val TYPE_DECL = Tx3ElementType("TYPE_DECL")

    /** A field inside a record-style type: `fieldName: Type,` */
    @JvmField val RECORD_FIELD = Tx3ElementType("RECORD_FIELD")

    /** A case inside a variant-style type: `Case1 { ... }` or `Case2,` */
    @JvmField val VARIANT_CASE = Tx3ElementType("VARIANT_CASE")

    /** `tx transfer(qty: Int) { ... }` */
    @JvmField val TX_DECL = Tx3ElementType("TX_DECL")

    /** A parameter in a tx signature: `qty: Int` */
    @JvmField val TX_PARAM = Tx3ElementType("TX_PARAM")

    /** `env { field: Type, }` — environment/context declaration */
    @JvmField val ENV_DECL = Tx3ElementType("ENV_DECL")

    /** `asset StaticAsset = 0xABC."TOKEN";` */
    @JvmField val ASSET_DECL = Tx3ElementType("ASSET_DECL")

    // ── Tx Body Blocks ────────────────────────────────────────────────────────
    /** `input source { ... }` */
    @JvmField val INPUT_BLOCK = Tx3ElementType("INPUT_BLOCK")

    /** `output named_output { ... }` */
    @JvmField val OUTPUT_BLOCK = Tx3ElementType("OUTPUT_BLOCK")

    /** `burn { amount: ..., redeemer: ... }` */
    @JvmField val BURN_BLOCK = Tx3ElementType("BURN_BLOCK")

    /** `mint { amount: ..., redeemer: ... }` */
    @JvmField val MINT_BLOCK = Tx3ElementType("MINT_BLOCK")

    /** `locals { name: expr, }` — local value bindings */
    @JvmField val LOCALS_BLOCK = Tx3ElementType("LOCALS_BLOCK")

    /** `collateral { ref: ... }` */
    @JvmField val COLLATERAL_BLOCK = Tx3ElementType("COLLATERAL_BLOCK")

    /** `reference name { ref: ... }` */
    @JvmField val REFERENCE_BLOCK = Tx3ElementType("REFERENCE_BLOCK")

    /** `signers { Party, 0xABC, }` — bare value list */
    @JvmField val SIGNERS_BLOCK = Tx3ElementType("SIGNERS_BLOCK")

    /** `validity { since_slot: ..., until_slot: ... }` */
    @JvmField val VALIDITY_BLOCK = Tx3ElementType("VALIDITY_BLOCK")

    /** `metadata { 1: ..., 2: ... }` — integer-keyed map */
    @JvmField val METADATA_BLOCK = Tx3ElementType("METADATA_BLOCK")

    /** `cardano::withdrawal { ... }` etc. */
    @JvmField val CARDANO_BLOCK = Tx3ElementType("CARDANO_BLOCK")

    /** A field assignment inside a block: `from: Sender,` */
    @JvmField val BLOCK_FIELD = Tx3ElementType("BLOCK_FIELD")

    /** `let x = expr;` inside a tx */
    @JvmField val LET_BINDING = Tx3ElementType("LET_BINDING")

    // ── Expressions ───────────────────────────────────────────────────────────
    @JvmField val EXPR             = Tx3ElementType("EXPR")
    @JvmField val BINARY_EXPR      = Tx3ElementType("BINARY_EXPR")
    @JvmField val UNARY_EXPR       = Tx3ElementType("UNARY_EXPR")
    @JvmField val CALL_EXPR        = Tx3ElementType("CALL_EXPR")
    @JvmField val RECORD_LITERAL   = Tx3ElementType("RECORD_LITERAL")
    @JvmField val RECORD_FIELD_INIT= Tx3ElementType("RECORD_FIELD_INIT")
    @JvmField val NAME_REF         = Tx3ElementType("NAME_REF")
    @JvmField val LITERAL          = Tx3ElementType("LITERAL")
    @JvmField val PATH_EXPR        = Tx3ElementType("PATH_EXPR")
    @JvmField val ASSET_EXPR       = Tx3ElementType("ASSET_EXPR")

    /** `source.field1` — field access */
    @JvmField val FIELD_ACCESS_EXPR = Tx3ElementType("FIELD_ACCESS_EXPR")

    /** `[1, 2, 3]` — list literal */
    @JvmField val LIST_EXPR = Tx3ElementType("LIST_EXPR")

    /** `{1: "v1", 2: "v2"}` — map literal */
    @JvmField val MAP_EXPR = Tx3ElementType("MAP_EXPR")

    /** `...source` — spread in record literal */
    @JvmField val SPREAD_EXPR = Tx3ElementType("SPREAD_EXPR")

    /** `MyVariant::Case1 { ... }` — variant construction */
    @JvmField val VARIANT_EXPR = Tx3ElementType("VARIANT_EXPR")

    // ── Types ─────────────────────────────────────────────────────────────────
    @JvmField val TYPE_REF     = Tx3ElementType("TYPE_REF")
    @JvmField val LIST_TYPE    = Tx3ElementType("LIST_TYPE")
    @JvmField val MAP_TYPE     = Tx3ElementType("MAP_TYPE")
    @JvmField val GENERIC_TYPE = Tx3ElementType("GENERIC_TYPE")

    // ── Miscellaneous ─────────────────────────────────────────────────────────
    @JvmField val PARAM_LIST = Tx3ElementType("PARAM_LIST")
    @JvmField val ARG_LIST   = Tx3ElementType("ARG_LIST")
}