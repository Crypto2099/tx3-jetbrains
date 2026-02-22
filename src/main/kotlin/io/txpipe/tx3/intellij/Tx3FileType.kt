package io.txpipe.tx3.intellij

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * Registers the .tx3 file extension with the Tx3 language.
 * Kotlin objects already expose a static INSTANCE field to Java automatically.
 */
object Tx3FileType : LanguageFileType(Tx3Language) {
    override fun getName(): String = "Tx3 File"
    override fun getDescription(): String = "Tx3 UTxO protocol definition"
    override fun getDefaultExtension(): String = "tx3"
    override fun getIcon(): Icon = Tx3Icons.FILE
}