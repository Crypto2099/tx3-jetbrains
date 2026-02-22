package io.txpipe.tx3.intellij

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType
import io.txpipe.tx3.intellij.psi.Tx3File

/**
 * Marks Tx3 files as a valid live template context.
 * All Tx3 live templates registered under this context will be available
 * when editing .tx3 files.
 */
class Tx3TemplateContextType : TemplateContextType("TX3") {
    override fun getPresentableName(): String = "Tx3"

    override fun isInContext(templateActionContext: TemplateActionContext): Boolean =
        templateActionContext.file is Tx3File
}
