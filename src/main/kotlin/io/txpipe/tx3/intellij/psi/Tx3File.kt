package io.txpipe.tx3.intellij.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.util.Key
import com.intellij.psi.FileViewProvider
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import io.txpipe.tx3.intellij.Tx3FileType
import io.txpipe.tx3.intellij.Tx3Language
import io.txpipe.tx3.intellij.lexer.Tx3TokenTypes
import io.txpipe.tx3.intellij.parser.Tx3ElementTypes
import io.txpipe.tx3.intellij.psi.impl.*

class Tx3File(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, Tx3Language) {

    override fun getFileType(): FileType = Tx3FileType.INSTANCE
    override fun toString(): String = "Tx3 File"

    fun partyDeclarations(): List<Tx3PartyDeclImpl> =
        CachedValuesManager.getCachedValue(this, PARTY_KEY) {
            CachedValueProvider.Result.create(
                childrenOfType<Tx3PartyDeclImpl>(Tx3ElementTypes.PARTY_DECL),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }

    fun policyDeclarations(): List<Tx3PolicyDeclImpl> =
        CachedValuesManager.getCachedValue(this, POLICY_KEY) {
            CachedValueProvider.Result.create(
                childrenOfType<Tx3PolicyDeclImpl>(Tx3ElementTypes.POLICY_DECL),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }

    fun recordDeclarations(): List<Tx3RecordDeclImpl> =
        CachedValuesManager.getCachedValue(this, RECORD_KEY) {
            CachedValueProvider.Result.create(
                childrenOfType<Tx3RecordDeclImpl>(Tx3ElementTypes.RECORD_DECL),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }

    fun typeDeclarations(): List<Tx3TypeDeclImpl> =
        CachedValuesManager.getCachedValue(this, TYPE_KEY) {
            CachedValueProvider.Result.create(
                childrenOfType<Tx3TypeDeclImpl>(Tx3ElementTypes.TYPE_DECL),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }

    fun assetDeclarations(): List<Tx3AssetDeclImpl> =
        CachedValuesManager.getCachedValue(this, ASSET_KEY) {
            CachedValueProvider.Result.create(
                childrenOfType<Tx3AssetDeclImpl>(Tx3ElementTypes.ASSET_DECL),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }

    fun txDeclarations(): List<Tx3TxDeclImpl> =
        CachedValuesManager.getCachedValue(this, TX_KEY) {
            CachedValueProvider.Result.create(
                childrenOfType<Tx3TxDeclImpl>(Tx3ElementTypes.TX_DECL),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }

    fun envFieldNames(): List<String> =
        CachedValuesManager.getCachedValue(this, ENV_KEY) {
            val names = mutableListOf<String>()
            var child = firstChild
            while (child != null) {
                if (child.node.elementType == Tx3ElementTypes.ENV_DECL) {
                    var fieldNode = child.firstChild
                    while (fieldNode != null) {
                        if (fieldNode.node.elementType == Tx3ElementTypes.RECORD_FIELD) {
                            fieldNode.node.findChildByType(Tx3TokenTypes.IDENTIFIER)
                                ?.text?.let { names.add(it) }
                        }
                        fieldNode = fieldNode.nextSibling
                    }
                }
                child = child.nextSibling
            }
            CachedValueProvider.Result.create(names, PsiModificationTracker.MODIFICATION_COUNT)
        }

    // Walks only direct children — O(n) where n = top-level declarations
    private inline fun <reified T : com.intellij.psi.PsiElement> childrenOfType(
        type: com.intellij.psi.tree.IElementType
    ): List<T> {
        val result = mutableListOf<T>()
        var child = firstChild
        while (child != null) {
            if (child.node.elementType == type && child is T) result.add(child)
            child = child.nextSibling
        }
        return result
    }

    companion object {
        // Each method needs its own Key so CachedValuesManager stores them under
        // distinct slots. Using getCachedValue(this) { } without a Key means all
        // calls share the same slot — the first result wins and others return wrong types.
        private val PARTY_KEY  = Key.create<CachedValue<List<Tx3PartyDeclImpl>>>("tx3.party.decls")
        private val POLICY_KEY = Key.create<CachedValue<List<Tx3PolicyDeclImpl>>>("tx3.policy.decls")
        private val RECORD_KEY = Key.create<CachedValue<List<Tx3RecordDeclImpl>>>("tx3.record.decls")
        private val TYPE_KEY   = Key.create<CachedValue<List<Tx3TypeDeclImpl>>>("tx3.type.decls")
        private val ASSET_KEY  = Key.create<CachedValue<List<Tx3AssetDeclImpl>>>("tx3.asset.decls")
        private val TX_KEY     = Key.create<CachedValue<List<Tx3TxDeclImpl>>>("tx3.tx.decls")
        private val ENV_KEY    = Key.create<CachedValue<List<String>>>("tx3.env.field.names")
    }
}