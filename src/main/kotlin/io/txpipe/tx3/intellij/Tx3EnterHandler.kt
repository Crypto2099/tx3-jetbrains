package io.txpipe.tx3.intellij

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegateAdapter
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import io.txpipe.tx3.intellij.psi.Tx3File

class Tx3EnterHandler : EnterHandlerDelegateAdapter() {

    override fun preprocessEnter(
        file: PsiFile,
        editor: Editor,
        caretOffsetRef: Ref<Int>,
        caretAdvanceRef: Ref<Int>,
        dataContext: DataContext,
        originalHandler: EditorActionHandler?
    ): EnterHandlerDelegate.Result {
        if (file !is Tx3File) return EnterHandlerDelegate.Result.Continue

        PsiDocumentManager.getInstance(file.project).commitDocument(editor.document)

        val offset = caretOffsetRef.get()

        // Find nesting depth by counting unmatched { above the caret
        val text = editor.document.text.substring(0, offset)
        val depth = text.count { it == '{' } - text.count { it == '}' }

        // At top level (depth == 0) — no indent
        // Inside one block level (depth == 1) — one indent (4 spaces)
        // Deeper — let IntelliJ handle it normally
        return when {
            depth <= 0 -> {
                editor.document.insertString(offset, "\n")
                editor.caretModel.moveToOffset(offset + 1)
                caretOffsetRef.set(offset + 1)
                EnterHandlerDelegate.Result.Stop
            }
            depth == 1 -> {
                editor.document.insertString(offset, "\n    ")
                editor.caretModel.moveToOffset(offset + 5)
                caretOffsetRef.set(offset + 5)
                EnterHandlerDelegate.Result.Stop
            }
            else -> EnterHandlerDelegate.Result.Continue
        }
    }
}