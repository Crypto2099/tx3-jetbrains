package io.txpipe.tx3.intellij.parser

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import io.txpipe.tx3.intellij.lexer.Tx3LexerAdapter
import io.txpipe.tx3.intellij.lexer.Tx3TokenTypes
import io.txpipe.tx3.intellij.psi.Tx3File
import io.txpipe.tx3.intellij.psi.impl.*

class Tx3ParserDefinition : ParserDefinition {

    override fun createLexer(project: Project): Lexer = Tx3LexerAdapter()

    override fun createParser(project: Project): PsiParser = Tx3Parser()

    override fun getFileNodeType(): IFileElementType = Tx3ElementTypes.FILE

    override fun getCommentTokens(): TokenSet = TokenSet.create(
        Tx3TokenTypes.LINE_COMMENT,
        Tx3TokenTypes.BLOCK_COMMENT
    )

    override fun getStringLiteralElements(): TokenSet = TokenSet.create(
        Tx3TokenTypes.STRING_LITERAL,
        Tx3TokenTypes.BYTES_LITERAL
    )

    override fun createElement(node: ASTNode): PsiElement = when (node.elementType) {
        // ── Named PSI types with dedicated impl classes ───────────────────────
        Tx3ElementTypes.PARTY_DECL        -> Tx3PartyDeclImpl(node)
        Tx3ElementTypes.POLICY_DECL       -> Tx3PolicyDeclImpl(node)
        Tx3ElementTypes.RECORD_DECL       -> Tx3RecordDeclImpl(node)
        Tx3ElementTypes.RECORD_FIELD      -> Tx3RecordFieldImpl(node)
        Tx3ElementTypes.TX_DECL           -> Tx3TxDeclImpl(node)
        Tx3ElementTypes.TX_PARAM          -> Tx3TxParamImpl(node)
        Tx3ElementTypes.INPUT_BLOCK       -> Tx3InputBlockImpl(node)
        Tx3ElementTypes.OUTPUT_BLOCK      -> Tx3OutputBlockImpl(node)
        Tx3ElementTypes.BLOCK_FIELD       -> Tx3BlockFieldImpl(node)
        Tx3ElementTypes.LET_BINDING       -> Tx3LetBindingImpl(node)
        Tx3ElementTypes.BINARY_EXPR       -> Tx3BinaryExprImpl(node)
        Tx3ElementTypes.UNARY_EXPR        -> Tx3UnaryExprImpl(node)
        Tx3ElementTypes.CALL_EXPR         -> Tx3CallExprImpl(node)
        Tx3ElementTypes.RECORD_LITERAL    -> Tx3RecordLiteralImpl(node)
        Tx3ElementTypes.RECORD_FIELD_INIT -> Tx3RecordFieldInitImpl(node)
        Tx3ElementTypes.NAME_REF          -> Tx3NameRefImpl(node)
        Tx3ElementTypes.LITERAL           -> Tx3LiteralImpl(node)
        Tx3ElementTypes.TYPE_REF          -> Tx3TypeRefImpl(node)
        Tx3ElementTypes.LIST_TYPE         -> Tx3ListTypeImpl(node)
        Tx3ElementTypes.PARAM_LIST        -> Tx3ParamListImpl(node)
        Tx3ElementTypes.ARG_LIST          -> Tx3ArgListImpl(node)

        // ── New node types — use ASTWrapperPsiElement until dedicated impls needed ──
        Tx3ElementTypes.TYPE_DECL         -> Tx3TypeDeclImpl(node)
        Tx3ElementTypes.VARIANT_CASE      -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.ENV_DECL          -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.ASSET_DECL        -> Tx3AssetDeclImpl(node)
        Tx3ElementTypes.BURN_BLOCK        -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.MINT_BLOCK        -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.LOCALS_BLOCK      -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.COLLATERAL_BLOCK  -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.REFERENCE_BLOCK   -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.SIGNERS_BLOCK     -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.VALIDITY_BLOCK    -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.METADATA_BLOCK    -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.CARDANO_BLOCK     -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.FIELD_ACCESS_EXPR -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.LIST_EXPR         -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.MAP_EXPR          -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.SPREAD_EXPR       -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.VARIANT_EXPR      -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.MAP_TYPE          -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.GENERIC_TYPE      -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.PATH_EXPR         -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.ASSET_EXPR        -> ASTWrapperPsiElement(node)
        Tx3ElementTypes.EXPR              -> ASTWrapperPsiElement(node)

        else -> throw IllegalArgumentException("Unknown element: ${node.elementType}")
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = Tx3File(viewProvider)
}