package io.txpipe.tx3.intellij

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import io.txpipe.tx3.intellij.psi.Tx3File

/**
 * Auto-closes {, (, [ and handles matching ) ] } deletion.
 */
class Tx3TypedHandler : TypedHandlerDelegate() {

    private val OPEN_TO_CLOSE = mapOf('{' to '}', '(' to ')', '[' to ']')
    private val CLOSE_CHARS = setOf('}', ')', ']')

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file !is Tx3File) return Result.CONTINUE

        val doc = editor.document
        val offset = editor.caretModel.offset

        // Auto-close opening brackets
        if (c in OPEN_TO_CLOSE) {
            val close = OPEN_TO_CLOSE[c]!!
            doc.insertString(offset, close.toString())
            // Leave caret between the pair
            return Result.STOP
        }

        // Skip over existing closing bracket instead of inserting a duplicate
        if (c in CLOSE_CHARS && offset < doc.textLength && doc.text[offset] == c) {
            doc.deleteString(offset - 1, offset) // remove the just-typed char
            editor.caretModel.moveToOffset(offset) // move past the existing one
            return Result.STOP
        }

        return Result.CONTINUE
    }
}