package io.txpipe.tx3.intellij.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import io.txpipe.tx3.intellij.lexer.Tx3TokenTypes
import io.txpipe.tx3.intellij.parser.Tx3ElementTypes
import io.txpipe.tx3.intellij.psi.impl.*

class Tx3FoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean
    ): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()

        // Single pass over the tree — collect everything at once
        var lineCommentRunStart: ASTNode? = null
        var lineCommentRunEnd: ASTNode? = null
        var lineCommentRunCount = 0

        fun flushLineCommentRun() {
            if (lineCommentRunCount >= 2 && lineCommentRunStart != null && lineCommentRunEnd != null) {
                val range = TextRange(
                    lineCommentRunStart!!.startOffset,
                    lineCommentRunEnd!!.startOffset + lineCommentRunEnd!!.textLength
                )
                descriptors.add(FoldingDescriptor(lineCommentRunStart!!, range, null,
                    "// … ($lineCommentRunCount lines)"))
            }
            lineCommentRunStart = null; lineCommentRunEnd = null; lineCommentRunCount = 0
        }

        PsiTreeUtil.processElements(root) { element ->
            when (element) {
                is Tx3TxDeclImpl      -> foldBlock(element, descriptors, document, txPlaceholder(element))
                is Tx3RecordDeclImpl  -> foldBlock(element, descriptors, document, recordPlaceholder(element))
                is Tx3InputBlockImpl  -> foldBlock(element, descriptors, document, "input ${element.name ?: ""} { … }")
                is Tx3OutputBlockImpl -> foldBlock(element, descriptors, document, "output ${element.name ?: ""} { … }")
                is Tx3PolicyDeclImpl  -> foldPolicyImport(element, descriptors)
                else -> {
                    val nodeType = element.node.elementType
                    when {
                        // Block comments — fold if multi-line
                        nodeType == Tx3TokenTypes.BLOCK_COMMENT -> {
                            flushLineCommentRun()
                            val range = element.textRange
                            if (document.getLineNumber(range.startOffset) !=
                                document.getLineNumber(range.endOffset - 1)) {
                                descriptors.add(FoldingDescriptor(element.node, range, null, "/* … */"))
                            }
                        }
                        // Line comments — accumulate runs
                        nodeType == Tx3TokenTypes.LINE_COMMENT -> {
                            if (lineCommentRunStart == null) lineCommentRunStart = element.node
                            lineCommentRunEnd = element.node
                            lineCommentRunCount++
                        }
                        // Anything non-whitespace breaks a line comment run
                        nodeType != com.intellij.psi.TokenType.WHITE_SPACE ||
                            element.text.count { it == '\n' } > 1 -> {
                            flushLineCommentRun()
                        }
                    }
                }
            }
            true
        }
        flushLineCommentRun()

        return descriptors.toTypedArray()
    }

    private fun foldBlock(
        element: PsiElement,
        descriptors: MutableList<FoldingDescriptor>,
        document: Document,
        placeholder: String
    ) {
        val lbrace = element.node.findChildByType(Tx3TokenTypes.LBRACE) ?: return
        val rbrace = element.node.findChildByType(Tx3TokenTypes.RBRACE) ?: return
        val start = lbrace.startOffset
        val end = rbrace.startOffset + 1
        if (document.getLineNumber(start) == document.getLineNumber(end - 1)) return
        descriptors.add(FoldingDescriptor(element.node, TextRange(start, end), null, placeholder))
    }

    private fun foldPolicyImport(
        element: Tx3PolicyDeclImpl,
        descriptors: MutableList<FoldingDescriptor>
    ) {
        val callExpr = element.node.findChildByType(Tx3ElementTypes.CALL_EXPR) ?: return
        val lparen = callExpr.findChildByType(Tx3TokenTypes.LPAREN) ?: return
        val rparen = callExpr.findChildByType(Tx3TokenTypes.RPAREN) ?: return
        val range = TextRange(lparen.startOffset + 1, rparen.startOffset)
        if (range.length > 20) {
            descriptors.add(FoldingDescriptor(callExpr, range, null, "…"))
        }
    }

    private fun txPlaceholder(element: Tx3TxDeclImpl): String {
        val name = element.name ?: "tx"
        val paramNames = element.params().mapNotNull { it.name }
        val params = if (paramNames.isEmpty()) "()" else "(${paramNames.joinToString(", ")})"
        return "tx $name$params { … }"
    }

    private fun recordPlaceholder(element: Tx3RecordDeclImpl): String {
        val name = element.name ?: "record"
        val count = element.fields().size
        return "record $name { $count field${if (count != 1) "s" else ""} }"
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    override fun getPlaceholderText(node: ASTNode): String = "{ … }"
}