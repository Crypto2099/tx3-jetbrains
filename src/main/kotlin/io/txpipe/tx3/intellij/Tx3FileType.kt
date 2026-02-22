package io.txpipe.tx3.intellij

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * Registers the .tx3 file extension with the Tx3 language.
 */
object Tx3FileType : LanguageFileType(Tx3Language) {
    @JvmField
    val INSTANCE = this

    override fun getName(): String = "Tx3 File"
    override fun getDescription(): String = "Tx3 UTxO protocol definition"
    override fun getDefaultExtension(): String = "tx3"
    override fun getIcon(): Icon = Tx3Icons.FILE
}