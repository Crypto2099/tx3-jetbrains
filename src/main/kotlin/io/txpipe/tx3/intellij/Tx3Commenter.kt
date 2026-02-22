package io.txpipe.tx3.intellij

import com.intellij.lang.CodeDocumentationAwareCommenter
import com.intellij.psi.PsiComment
import com.intellij.psi.tree.IElementType
import io.txpipe.tx3.intellij.lexer.Tx3TokenTypes

/**
 * Enables Cmd+/ (line comment) and Ctrl+Shift+/ (block comment) in Tx3 files.
 */
class Tx3Commenter : CodeDocumentationAwareCommenter {
    override fun getLineCommentPrefix(): String = "//"
    override fun getBlockCommentPrefix(): String = "/*"
    override fun getBlockCommentSuffix(): String = "*/"
    override fun getCommentedBlockCommentPrefix(): String = "/*"
    override fun getCommentedBlockCommentSuffix(): String = "*/"
    override fun getLineCommentTokenType(): IElementType = Tx3TokenTypes.LINE_COMMENT
    override fun getBlockCommentTokenType(): IElementType = Tx3TokenTypes.BLOCK_COMMENT
    override fun getDocumentationCommentTokenType(): IElementType? = null
    override fun getDocumentationCommentPrefix(): String? = null
    override fun getDocumentationCommentLinePrefix(): String? = null
    override fun getDocumentationCommentSuffix(): String? = null
    override fun isDocumentationComment(element: PsiComment): Boolean = false
}
