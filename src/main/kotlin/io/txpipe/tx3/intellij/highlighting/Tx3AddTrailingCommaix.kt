package io.txpipe.tx3.intellij.highlighting

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

/**
 * Quick fix that inserts a trailing comma immediately after the given PSI element.
 * Used wherever Tx3 mandates trailing commas (record fields, block fields, tx params, etc.).
 */
class Tx3AddTrailingCommaFix(element: PsiElement) :
    LocalQuickFixAndIntentionActionOnPsiElement(element) {

    override fun getFamilyName(): String = "Add trailing comma"
    override fun getText(): String = "Add trailing comma"

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        // Simpler: just insert text directly via the document
        if (editor != null) {
            val offset = startElement.textRange.endOffset
            editor.document.insertString(offset, ",")
        } else {
            // Fall back to document approach via PsiDocumentManager
            val doc = com.intellij.psi.PsiDocumentManager.getInstance(project)
                .getDocument(file) ?: return
            doc.insertString(startElement.textRange.endOffset, ",")
            com.intellij.psi.PsiDocumentManager.getInstance(project).commitDocument(doc)
        }
    }
}