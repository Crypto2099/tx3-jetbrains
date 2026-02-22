package io.txpipe.tx3.intellij.lexer

import com.intellij.lexer.FlexAdapter

/**
 * Adapter that wraps the JFlex-generated [Tx3FlexLexer] in IntelliJ's [FlexAdapter].
 *
 * The generated class [Tx3FlexLexer] is produced from Tx3Lexer.flex by the
 * grammarkit Gradle task; it lives in src/main/gen after code generation.
 */
class Tx3LexerAdapter : FlexAdapter(Tx3FlexLexer(null))
