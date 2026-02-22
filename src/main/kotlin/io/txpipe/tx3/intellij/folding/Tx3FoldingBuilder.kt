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
                is Tx3RecordDeclImpl  -> foldBlock(element, descriptors, document, typePlaceholder("record", element.name, element.fields().size))
                is Tx3TypeDeclImpl    -> foldBlock(element, descriptors, document, typePlaceholder("type", element.name, element.node.getChildren(null).count { it.elementType == Tx3ElementTypes.RECORD_FIELD || it.elementType == Tx3ElementTypes.VARIANT_CASE }))
                is Tx3InputBlockImpl    -> foldBlock(element, descriptors, document, "input ${element.name ?: ""} { … }")
                is Tx3OutputBlockImpl   -> foldBlock(element, descriptors, document, "output ${element.name ?: ""} { … }")
                is Tx3RecordLiteralImpl -> {
                    val fieldCount = element.node.getChildren(null)
                        .count { it.elementType == Tx3ElementTypes.RECORD_FIELD_INIT }
                    if (fieldCount >= 2) {
                        val typeName = element.node.firstChildNode?.text ?: "…"
                        foldBlock(element, descriptors, document, "$typeName { … }")
                    }
                }
                is Tx3PolicyDeclImpl  -> foldPolicyImport(element, descriptors)
                else -> {
                    val nodeType = element.node.elementType
                    when {
                        // env { ... } — top-level environment block
                        nodeType == Tx3ElementTypes.ENV_DECL -> {
                            flushLineCommentRun()
                            val count = element.node.getChildren(null).count { it.elementType == Tx3ElementTypes.RECORD_FIELD }
                            foldBlock(element, descriptors, document, "env { $count field${if (count != 1) "s" else ""} }")
                        }
                        // locals { ... } — let-bindings block inside a tx
                        nodeType == Tx3ElementTypes.LOCALS_BLOCK -> {
                            flushLineCommentRun()
                            foldBlock(element, descriptors, document, "locals { … }")
                        }
                        // variant cases that have a record body { ... }
                        nodeType == Tx3ElementTypes.VARIANT_CASE &&
                            element.node.findChildByType(Tx3TokenTypes.LBRACE) != null -> {
                            flushLineCommentRun()
                            val caseName = element.node.firstChildNode?.text ?: "case"
                            foldBlock(element, descriptors, document, "$caseName { … }")
                        }
                        // variant construction expression: TypeName::CaseName { field: val, ... }
                        nodeType == Tx3ElementTypes.VARIANT_EXPR &&
                            element.node.findChildByType(Tx3TokenTypes.LBRACE) != null -> {
                            flushLineCommentRun()
                            val fieldCount = element.node.getChildren(null)
                                .count { it.elementType == Tx3ElementTypes.RECORD_FIELD_INIT }
                            if (fieldCount >= 2) {
                                val typeName = element.node.firstChildNode?.text ?: ""
                                val caseName = element.node.getChildren(null)
                                    .firstOrNull { it.elementType == Tx3TokenTypes.IDENTIFIER }?.text ?: ""
                                val label = if (typeName.isNotEmpty() && caseName.isNotEmpty())
                                    "$typeName::$caseName" else typeName.ifEmpty { caseName }
                                foldBlock(element, descriptors, document, "$label { … }")
                            }
                        }
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

    private fun typePlaceholder(keyword: String, name: String?, count: Int): String {
        val label = name ?: keyword
        return if (count > 0) "$keyword $label { $count field${if (count != 1) "s" else ""} }"
        else "$keyword $label { … }"
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    override fun getPlaceholderText(node: ASTNode): String = "{ … }"
}