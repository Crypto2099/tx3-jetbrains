package io.txpipe.tx3.intellij

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import io.txpipe.tx3.intellij.lexer.Tx3LexerAdapter
import io.txpipe.tx3.intellij.lexer.Tx3TokenTypes
import io.txpipe.tx3.intellij.psi.impl.*

/**
 * Enables "Find Usages" (Alt+F7) for Tx3 named elements:
 * parties, policies, records, and tx declarations.
 */
class Tx3FindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner = DefaultWordsScanner(
        Tx3LexerAdapter(),
        TokenSet.create(Tx3TokenTypes.IDENTIFIER),
        TokenSet.create(Tx3TokenTypes.LINE_COMMENT, Tx3TokenTypes.BLOCK_COMMENT),
        TokenSet.create(*Tx3TokenTypes.LITERALS.toTypedArray()),
    )

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean =
        psiElement is Tx3PartyDeclImpl  ||
        psiElement is Tx3PolicyDeclImpl ||
        psiElement is Tx3RecordDeclImpl ||
        psiElement is Tx3TxDeclImpl     ||
        psiElement is Tx3TxParamImpl    ||
        psiElement is Tx3RecordFieldImpl

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String = when (element) {
        is Tx3PartyDeclImpl  -> "party"
        is Tx3PolicyDeclImpl -> "policy"
        is Tx3RecordDeclImpl -> "record"
        is Tx3TxDeclImpl     -> "transaction template"
        is Tx3TxParamImpl    -> "parameter"
        is Tx3RecordFieldImpl-> "field"
        else                 -> "element"
    }

    override fun getDescriptiveName(element: PsiElement): String =
        (element as? Tx3NamedElementBase)?.name ?: element.text

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        (element as? Tx3NamedElementBase)?.name ?: element.text
}
