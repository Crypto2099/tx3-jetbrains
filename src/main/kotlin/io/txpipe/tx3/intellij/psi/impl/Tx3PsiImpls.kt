package io.txpipe.tx3.intellij.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.IncorrectOperationException
import io.txpipe.tx3.intellij.lexer.Tx3TokenTypes
import io.txpipe.tx3.intellij.parser.Tx3ElementTypes
import io.txpipe.tx3.intellij.psi.Tx3NamedElement

// ── Base class for named declarations ────────────────────────────────────────

abstract class Tx3NamedElementBase(node: ASTNode) : ASTWrapperPsiElement(node), Tx3NamedElement {

    override fun getNameIdentifier(): PsiElement? {
        // Try a real IDENTIFIER first; fall back to any soft-keyword token that
        // was accepted in an identifier position (e.g., a param named 'metadata').
        node.findChildByType(Tx3TokenTypes.IDENTIFIER)?.psi?.let { return it }
        for (child in node.getChildren(null)) {
            if (child.elementType in Tx3TokenTypes.SOFT_KEYWORD_TOKENS) return child.psi
        }
        return null
    }

    override fun getName(): String? = nameIdentifier?.text

    override fun setName(name: String): PsiElement {
        val identifier = nameIdentifier
            ?: throw IncorrectOperationException("No identifier to rename")
        val newNode = createIdentifierNode(name)
        node.replaceChild(identifier.node, newNode)
        return this
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()

    private fun createIdentifierNode(name: String): ASTNode {
        val dummyFile = com.intellij.psi.PsiFileFactory.getInstance(project)
            .createFileFromText(
                "dummy.tx3",
                io.txpipe.tx3.intellij.Tx3FileType,
                "party $name;"
            )
        return dummyFile.node.findChildByType(Tx3TokenTypes.IDENTIFIER)
            ?: throw IncorrectOperationException("Could not create identifier node")
    }
}

// ── Top-level Declarations ────────────────────────────────────────────────────

class Tx3PartyDeclImpl(node: ASTNode) : Tx3NamedElementBase(node) {
    override fun toString(): String = "Tx3PartyDecl($name)"
}

class Tx3PolicyDeclImpl(node: ASTNode) : Tx3NamedElementBase(node) {
    override fun toString(): String = "Tx3PolicyDecl($name)"

}

class Tx3TypeDeclImpl(node: ASTNode) : Tx3NamedElementBase(node) {
    override fun toString(): String = "Tx3TypeDecl($name)"
}

class Tx3AssetDeclImpl(node: ASTNode) : Tx3NamedElementBase(node) {
    override fun toString(): String = "Tx3AssetDecl($name)"
}

class Tx3RecordDeclImpl(node: ASTNode) : Tx3NamedElementBase(node) {
    override fun toString(): String = "Tx3RecordDecl($name)"

    // Use .psi to get the existing cached wrapper — never construct new wrappers
    fun fields(): List<Tx3RecordFieldImpl> = CachedValuesManager.getCachedValue(this) {
        val result = node.getChildren(null)
            .filter { it.elementType == Tx3ElementTypes.RECORD_FIELD }
            .mapNotNull { it.psi as? Tx3RecordFieldImpl }
        CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT)
    }
}

class Tx3RecordFieldImpl(node: ASTNode) : Tx3NamedElementBase(node) {
    override fun toString(): String = "Tx3RecordField($name)"

    fun fieldType(): Tx3TypeRefImpl? =
        node.findChildByType(Tx3ElementTypes.TYPE_REF)?.psi as? Tx3TypeRefImpl
}

class Tx3TxDeclImpl(node: ASTNode) : Tx3NamedElementBase(node) {
    override fun toString(): String = "Tx3TxDecl($name)"

    fun params(): List<Tx3TxParamImpl> = CachedValuesManager.getCachedValue(this) {
        val result = node.findChildByType(Tx3ElementTypes.PARAM_LIST)
            ?.getChildren(null)
            ?.filter { it.elementType == Tx3ElementTypes.TX_PARAM }
            ?.mapNotNull { it.psi as? Tx3TxParamImpl }
            ?: emptyList()
        CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT)
    }

    fun inputBlocks(): List<Tx3InputBlockImpl> = CachedValuesManager.getCachedValue(this) {
        val result = node.getChildren(null)
            .filter { it.elementType == Tx3ElementTypes.INPUT_BLOCK }
            .mapNotNull { it.psi as? Tx3InputBlockImpl }
        CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT)
    }

    fun outputBlocks(): List<Tx3OutputBlockImpl> = CachedValuesManager.getCachedValue(this) {
        val result = node.getChildren(null)
            .filter { it.elementType == Tx3ElementTypes.OUTPUT_BLOCK }
            .mapNotNull { it.psi as? Tx3OutputBlockImpl }
        CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT)
    }
}

class Tx3TxParamImpl(node: ASTNode) : Tx3NamedElementBase(node) {
    override fun toString(): String = "Tx3TxParam($name)"

    fun paramType(): Tx3TypeRefImpl? =
        node.findChildByType(Tx3ElementTypes.TYPE_REF)?.psi as? Tx3TypeRefImpl
}

// ── Block Implementations ─────────────────────────────────────────────────────

class Tx3InputBlockImpl(node: ASTNode) : Tx3NamedElementBase(node) {
    override fun toString(): String = "Tx3InputBlock(${name ?: "<anon>"})"

    fun fields(): Map<String, ASTNode> = CachedValuesManager.getCachedValue(this) {
        val result = collectBlockFields(node)
        CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT)
    }
}

class Tx3OutputBlockImpl(node: ASTNode) : Tx3NamedElementBase(node) {
    override fun toString(): String = "Tx3OutputBlock(${name ?: "<anon>"})"

    fun fields(): Map<String, ASTNode> = CachedValuesManager.getCachedValue(this) {
        val result = collectBlockFields(node)
        CachedValueProvider.Result.create(result, PsiModificationTracker.MODIFICATION_COUNT)
    }
}

private fun collectBlockFields(node: ASTNode): Map<String, ASTNode> =
    node.getChildren(null)
        .filter { it.elementType == Tx3ElementTypes.BLOCK_FIELD }
        .mapNotNull { fieldNode ->
            val key = fieldNode.firstChildNode?.text ?: return@mapNotNull null
            key to fieldNode
        }.toMap()

class Tx3BlockFieldImpl(node: ASTNode) : ASTWrapperPsiElement(node)

// ── Expression Implementations ────────────────────────────────────────────────

class Tx3BinaryExprImpl(node: ASTNode) : ASTWrapperPsiElement(node)
class Tx3UnaryExprImpl(node: ASTNode) : ASTWrapperPsiElement(node)

class Tx3CallExprImpl(node: ASTNode) : ASTWrapperPsiElement(node)

class Tx3RecordLiteralImpl(node: ASTNode) : ASTWrapperPsiElement(node)

class Tx3RecordFieldInitImpl(node: ASTNode) : ASTWrapperPsiElement(node)

class Tx3NameRefImpl(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun referencedName(): String? = node.firstChildNode?.text
}

class Tx3LiteralImpl(node: ASTNode) : ASTWrapperPsiElement(node)

class Tx3LetBindingImpl(node: ASTNode) : Tx3NamedElementBase(node)

// ── Type Implementations ──────────────────────────────────────────────────────

class Tx3TypeRefImpl(node: ASTNode) : ASTWrapperPsiElement(node) {
    fun typeName(): String? = node.firstChildNode?.text
}

class Tx3ListTypeImpl(node: ASTNode) : ASTWrapperPsiElement(node)

// ── Misc ──────────────────────────────────────────────────────────────────────

class Tx3ParamListImpl(node: ASTNode) : ASTWrapperPsiElement(node)
class Tx3ArgListImpl(node: ASTNode) : ASTWrapperPsiElement(node)