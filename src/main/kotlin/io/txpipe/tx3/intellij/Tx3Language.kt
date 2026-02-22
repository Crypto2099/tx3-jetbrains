package io.txpipe.tx3.intellij

import com.intellij.lang.Language

/**
 * Singleton language object for Tx3.
 * All language-specific extension points reference this language ID.
 */
object Tx3Language : Language("Tx3", "application/x-tx3") {
    // Required to preserve singleton semantics during Java deserialization.
    // Called reflectively by the JVM — suppress the "unused" inspection.
    @Suppress("unused")
    private fun readResolve(): Any = Tx3Language

    override fun getDisplayName(): String = "Tx3"
    override fun isCaseSensitive(): Boolean = true
}