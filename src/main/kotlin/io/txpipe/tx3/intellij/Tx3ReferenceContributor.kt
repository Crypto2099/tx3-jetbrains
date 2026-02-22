package io.txpipe.tx3.intellij

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.Key
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.*
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import io.txpipe.tx3.intellij.lexer.Tx3TokenTypes
import io.txpipe.tx3.intellij.parser.Tx3ElementTypes
import io.txpipe.tx3.intellij.psi.Tx3File
import io.txpipe.tx3.intellij.psi.impl.*

private val FILE_INDEX_KEY = Key.create<CachedValue<Map<String, PsiElement>>>("tx3.ref.file.index")
private val TX_INDEX_KEY   = Key.create<CachedValue<Map<String, PsiElement>>>("tx3.ref.tx.index")

class Tx3ReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val provider = Tx3NameRefReferenceProvider()
        // Match the composite NAME_REF node directly
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(Tx3NameRefImpl::class.java),
            provider
        )
        // IDENTIFIER leaf inside a NAME_REF (what IntelliJ lands on at the caret)
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(Tx3TokenTypes.IDENTIFIER)
                .withParent(Tx3NameRefImpl::class.java),
            Tx3LeafNameRefProvider()
        )
        // TYPE_REF containing a user-defined name (e.g. MyRecord in datum_is: MyRecord)
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement()
                .withElementType(Tx3ElementTypes.TYPE_REF),
            Tx3TypeRefReferenceProvider()
        )
        // IDENTIFIER leaf inside a TYPE_REF (what IntelliJ lands on at the caret)
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(Tx3TokenTypes.IDENTIFIER)
                .withParent(PlatformPatterns.psiElement().withElementType(Tx3ElementTypes.TYPE_REF)),
            Tx3TypeRefLeafProvider()
        )
    }
}

private class Tx3NameRefReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val nameRef = element as? Tx3NameRefImpl ?: return PsiReference.EMPTY_ARRAY
        return arrayOf(Tx3NameReference(nameRef))
    }
}

private class Tx3TypeRefReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        // Only handle TYPE_REF nodes whose child is an IDENTIFIER (user-defined types)
        // Builtin types (Int, Bytes, etc.) have keyword tokens, not IDENTIFIER
        val identNode = element.node.findChildByType(Tx3TokenTypes.IDENTIFIER) ?: return PsiReference.EMPTY_ARRAY
        val name = identNode.text
        return arrayOf(Tx3TypeRefReference(element, name))
    }
}

private class Tx3TypeRefLeafProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val typeRef = element.parent ?: return PsiReference.EMPTY_ARRAY
        return Tx3TypeRefReferenceProvider().getReferencesByElement(typeRef, context)
    }
}

private class Tx3TypeRefReference(
    private val element: PsiElement,
    private val name: String
) : PsiReferenceBase<PsiElement>(element) {

    override fun resolve(): PsiElement? {
        val file = element.containingFile as? Tx3File ?: return null
        // Search type declarations (type keyword) and legacy record declarations
        file.typeDeclarations().forEach   { if (it.name == name) return it }
        file.recordDeclarations().forEach { if (it.name == name) return it }
        return null
    }

    override fun getVariants(): Array<Any> = emptyArray()
}

private class Tx3LeafNameRefProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        // Delegate to the parent NAME_REF composite node's reference
        val nameRef = element.parent as? Tx3NameRefImpl ?: return PsiReference.EMPTY_ARRAY
        return nameRef.references
    }
}

private class Tx3NameReference(private val ref: Tx3NameRefImpl) :
    PsiReferenceBase<Tx3NameRefImpl>(ref) {

    override fun getRangeInElement(): TextRange = TextRange(0, ref.textLength)

    override fun resolve(): PsiElement? {
        val name = ref.referencedName() ?: return null
        val file = ref.containingFile as? Tx3File ?: return null

        // Check tx-local scope first: params, input block names, let bindings.
        // Cache per tx declaration — invalidated on any PSI change.
        val containingTx = PsiTreeUtil.getParentOfType(ref, Tx3TxDeclImpl::class.java)
        if (containingTx != null) {
            val txIndex = CachedValuesManager.getCachedValue(containingTx, TX_INDEX_KEY) {
                val index = buildMap<String, PsiElement> {
                    containingTx.params().forEach       { p -> p.name?.let { put(it, p) } }
                    containingTx.inputBlocks().forEach  { b -> b.name?.let { put(it, b) } }
                    containingTx.outputBlocks().forEach { b -> b.name?.let { put(it, b) } }
                    PsiTreeUtil.findChildrenOfType(containingTx, Tx3LetBindingImpl::class.java)
                        .forEach { lb -> lb.name?.let { put(it, lb) } }
                }
                CachedValueProvider.Result.create(index, PsiModificationTracker.MODIFICATION_COUNT)
            }
            txIndex[name]?.let { return it }
        }

        // File-level declarations — cache the full name→element map once per modification.
        val fileIndex = CachedValuesManager.getCachedValue(file, FILE_INDEX_KEY) {
            val index = buildMap<String, PsiElement> {
                file.partyDeclarations().forEach  { it.name?.let { n -> put(n, it) } }
                file.policyDeclarations().forEach { it.name?.let { n -> put(n, it) } }
                file.recordDeclarations().forEach { it.name?.let { n -> put(n, it) } }
                file.typeDeclarations().forEach   { it.name?.let { n -> put(n, it) } }
                file.assetDeclarations().forEach  { it.name?.let { n -> put(n, it) } }
                file.txDeclarations().forEach     { it.name?.let { n -> put(n, it) } }
            }
            CachedValueProvider.Result.create(index, PsiModificationTracker.MODIFICATION_COUNT)
        }
        return fileIndex[name]
    }

    override fun getVariants(): Array<Any> = emptyArray()
}