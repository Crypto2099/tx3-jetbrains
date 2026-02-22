package io.txpipe.tx3.intellij.structure

import com.intellij.ide.structureView.*
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiFile
import io.txpipe.tx3.intellij.Tx3Icons
import io.txpipe.tx3.intellij.psi.Tx3File
import io.txpipe.tx3.intellij.psi.impl.*
import javax.swing.Icon

/**
 * Populates the Structure panel (⌘7 / View → Tool Windows → Structure) with
 * all top-level Tx3 declarations and their children.
 */
class Tx3StructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        val tx3File = psiFile as? Tx3File ?: return null
        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                Tx3StructureViewModel(tx3File, editor)
        }
    }
}

// ── Structure View Model ───────────────────────────────────────────────────────

private class Tx3StructureViewModel(
    file: Tx3File,
    editor: Editor?
) : StructureViewModelBase(file, editor, Tx3FileNode(file)), StructureViewModel.ElementInfoProvider {

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false
    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean =
        element is Tx3LeafNode
}

// ── File Node ─────────────────────────────────────────────────────────────────

private class Tx3FileNode(private val file: Tx3File) : StructureViewTreeElement {
    override fun getValue(): Any = file
    override fun navigate(requestFocus: Boolean) = (file as NavigatablePsiElement).navigate(requestFocus)
    override fun canNavigate(): Boolean = true
    override fun canNavigateToSource(): Boolean = true

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = file.name
        override fun getIcon(unused: Boolean): Icon = Tx3Icons.FILE
    }

    override fun getChildren(): Array<TreeElement> {
        val children = mutableListOf<TreeElement>()

        file.partyDeclarations().forEach { children.add(Tx3DeclNode(it as Tx3NamedElementBase, Tx3Icons.PARTY, "party")) }
        file.policyDeclarations().forEach { children.add(Tx3DeclNode(it as Tx3NamedElementBase, Tx3Icons.POLICY, "policy")) }
        file.recordDeclarations().forEach { children.add(Tx3RecordNode(it)) }
        file.txDeclarations().forEach { children.add(Tx3TxNode(it)) }

        return children.toTypedArray()
    }
}

// ── Declaration Node ──────────────────────────────────────────────────────────

private class Tx3DeclNode(
    private val element: Tx3NamedElementBase,
    private val icon: Icon,
    private val kind: String
) : StructureViewTreeElement {
    override fun getValue(): Any = element
    override fun navigate(requestFocus: Boolean) = element.navigate(requestFocus)
    override fun canNavigate(): Boolean = true
    override fun canNavigateToSource(): Boolean = true

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = element.name ?: "<unnamed>"
        override fun getLocationString(): String = kind
        override fun getIcon(unused: Boolean): Icon = icon
    }

    override fun getChildren(): Array<TreeElement> = TreeElement.EMPTY_ARRAY
}

// ── Record Node ───────────────────────────────────────────────────────────────

private class Tx3RecordNode(private val record: Tx3RecordDeclImpl) : StructureViewTreeElement {
    override fun getValue(): Any = record
    override fun navigate(requestFocus: Boolean) = record.navigate(requestFocus)
    override fun canNavigate(): Boolean = true
    override fun canNavigateToSource(): Boolean = true

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = record.name ?: "<unnamed>"
        override fun getLocationString(): String = "record"
        override fun getIcon(unused: Boolean): Icon = Tx3Icons.RECORD
    }

    override fun getChildren(): Array<TreeElement> =
        record.fields().map { field ->
            Tx3LeafNode(field, Tx3Icons.FIELD, "${field.name ?: "?"}: ${field.fieldType()?.typeName() ?: "?"}")
        }.toTypedArray()
}

// ── Tx Node ───────────────────────────────────────────────────────────────────

private class Tx3TxNode(private val tx: Tx3TxDeclImpl) : StructureViewTreeElement {
    override fun getValue(): Any = tx
    override fun navigate(requestFocus: Boolean) = tx.navigate(requestFocus)
    override fun canNavigate(): Boolean = true
    override fun canNavigateToSource(): Boolean = true

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String {
            val paramSig = tx.params().joinToString(", ") { p ->
                "${p.name ?: "?"}: ${p.paramType()?.typeName() ?: "?"}"
            }
            return "${tx.name ?: "tx"}($paramSig)"
        }
        override fun getLocationString(): String = "tx"
        override fun getIcon(unused: Boolean): Icon = Tx3Icons.TX
    }

    override fun getChildren(): Array<TreeElement> {
        val children = mutableListOf<TreeElement>()
        tx.inputBlocks().forEach { input ->
            children.add(Tx3LeafNode(input, Tx3Icons.INPUT, "input ${input.name ?: ""}".trim()))
        }
        tx.outputBlocks().forEach { output ->
            children.add(Tx3LeafNode(output, Tx3Icons.OUTPUT, "output ${output.name ?: ""}".trim()))
        }
        return children.toTypedArray()
    }
}

// ── Generic Leaf Node ─────────────────────────────────────────────────────────

private class Tx3LeafNode(
    private val element: Tx3NamedElementBase,
    private val icon: Icon,
    private val label: String
) : StructureViewTreeElement {
    override fun getValue(): Any = element
    override fun navigate(requestFocus: Boolean) = element.navigate(requestFocus)
    override fun canNavigate(): Boolean = true
    override fun canNavigateToSource(): Boolean = true

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = label
        override fun getIcon(unused: Boolean): Icon = icon
    }

    override fun getChildren(): Array<TreeElement> = TreeElement.EMPTY_ARRAY
}