package io.txpipe.tx3.intellij.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import io.txpipe.tx3.intellij.Tx3FileType
import io.txpipe.tx3.intellij.Tx3Icons
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.ListSelectionModel

class Tx3NewFileAction : AnAction(), DumbAware {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val dir = if (virtualFile.isDirectory) virtualFile else virtualFile.parent ?: return
        val psiDir = PsiManager.getInstance(project).findDirectory(dir) ?: return

        val dialog = Tx3NewFileDialog(project)
        if (!dialog.showAndGet()) return

        val rawName = dialog.fileName.trim().ifEmpty { return }
        val fileName = if (rawName.endsWith(".tx3")) rawName else "$rawName.tx3"
        val content = dialog.selectedTemplate.content

        WriteCommandAction.runWriteCommandAction(project) {
            val file = PsiFileFactory.getInstance(project)
                .createFileFromText(fileName, Tx3FileType.INSTANCE, content)
            val added = psiDir.add(file)
            (added as? com.intellij.psi.PsiFile)?.navigate(true)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.text = "Tx3 Protocol File"
        e.presentation.description = "Create a new .tx3 protocol definition file"
        e.presentation.icon = Tx3Icons.FILE
        val vf = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = vf != null && e.project != null
    }
}

data class Tx3Template(val name: String, val content: String)

private val TEMPLATES = listOf(
    Tx3Template("Blank", ""),
    Tx3Template("Simple Transfer", """
party Sender;
party Receiver;

tx transfer(
    quantity: Int,
) {
    input source {
        from: Sender,
        min_amount: Ada(quantity),
    }

    output {
        to: Receiver,
        amount: Ada(quantity),
    }
}
""".trimStart()),
    Tx3Template("Vesting Contract", """
party Beneficiary;
party Vendor;

tx vesting(
    quantity: Int,
    lock_until: Int,
) {
    input source {
        from: Vendor,
        min_amount: Ada(quantity),
    }

    output {
        to: Beneficiary,
        amount: Ada(quantity),
    }

    validity {
        since_slot: lock_until,
    }
}
""".trimStart())
)

private class Tx3NewFileDialog(project: com.intellij.openapi.project.Project) : DialogWrapper(project) {

    private val nameField = JTextField("protocol", 30)
    private val listModel = DefaultListModel<String>().also { m -> TEMPLATES.forEach { m.addElement(it.name) } }
    private val templateList = JBList(listModel).also {
        it.selectionMode = ListSelectionModel.SINGLE_SELECTION
        it.selectedIndex = 0
    }

    val fileName: String get() = nameField.text ?: ""
    val selectedTemplate: Tx3Template get() = TEMPLATES[templateList.selectedIndex.coerceAtLeast(0)]

    init {
        title = "New Tx3 Protocol File"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(8, 8))
        panel.preferredSize = Dimension(350, 200)

        val namePanel = JPanel(BorderLayout(4, 0))
        namePanel.add(JBLabel("File name:"), BorderLayout.WEST)
        namePanel.add(nameField, BorderLayout.CENTER)

        panel.add(namePanel, BorderLayout.NORTH)
        panel.add(JBLabel("Template:"), BorderLayout.WEST)
        panel.add(templateList, BorderLayout.CENTER)
        return panel
    }

    override fun getPreferredFocusedComponent() = nameField
}