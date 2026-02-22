package io.txpipe.tx3.intellij

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Central icon registry for the Tx3 plugin.
 *
 * Icons live in src/main/resources/icons/ as SVG files.
 * IntelliJ automatically scales SVG icons for HiDPI.
 */
object Tx3Icons {
    @JvmField val FILE: Icon = load("/icons/tx3_file.svg")

    // Structure view icons
    @JvmField val TX: Icon = load("/icons/tx3_tx.svg")
    @JvmField val PARTY: Icon = load("/icons/tx3_party.svg")
    @JvmField val POLICY: Icon = load("/icons/tx3_policy.svg")
    @JvmField val RECORD: Icon = load("/icons/tx3_record.svg")
    @JvmField val INPUT: Icon = load("/icons/tx3_input.svg")
    @JvmField val OUTPUT: Icon = load("/icons/tx3_output.svg")
    @JvmField val FIELD: Icon = load("/icons/tx3_field.svg")
    @JvmField val PARAM: Icon = load("/icons/tx3_param.svg")

    private fun load(path: String): Icon = IconLoader.getIcon(path, Tx3Icons::class.java)
}
