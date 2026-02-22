package io.txpipe.tx3.intellij.psi

import com.intellij.psi.PsiNameIdentifierOwner

/**
 * Marker interface for all Tx3 PSI elements that have a name (party, policy, record, tx, etc.).
 * Implementors gain participation in rename refactoring, find-usages, and go-to-definition.
 */
interface Tx3NamedElement : PsiNameIdentifierOwner
