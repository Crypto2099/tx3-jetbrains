import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.2"
    id("org.jetbrains.grammarkit") version "2022.3.2"
}

group = "io.txpipe"
version = "1.0.4"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

intellij {
    version.set("2023.3.2")
    type.set("IC")
    plugins.set(listOf("com.intellij.java"))
}

grammarKit {
    // Use GrammarKit's bundled JFlex and skeleton — pinning jflexRelease bypasses
    // the IntelliJ-specific skeleton and causes reset() / ZZ_CMAP generation errors.
    grammarKitRelease.set("2022.3")
}

// ── Lexer generation ──────────────────────────────────────────────────────────
val generateLexer = tasks.named<GenerateLexerTask>("generateLexer") {
    sourceFile.set(file("src/main/kotlin/io/txpipe/tx3/intellij/lexer/Tx3Lexer.flex"))
    targetDir.set("src/main/gen/io/txpipe/tx3/intellij/lexer/")
    targetClass.set("Tx3FlexLexer")
    purgeOldFiles.set(true)
}

// ── Parser generation (disabled — parser is hand-written) ─────────────────────
val generateParser = tasks.named<GenerateParserTask>("generateParser") {
    enabled = false
}

sourceSets {
    main {
        java.srcDirs("src/main/gen")
        kotlin.srcDirs("src/main/kotlin")
        resources.srcDirs("src/main/resources")
    }
    test {
        kotlin.srcDirs("src/test/kotlin")
        resources.srcDirs("src/test/testData")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    // buildSearchableOptions spins up a full headless IDE to index plugin settings
    // for the Settings search bar. It frequently fails due to sandbox environment
    // issues unrelated to plugin code and is not critical for a language plugin.
    buildSearchableOptions {
        enabled = false
    }

    // Fix: IntelliJ platform and our resources both contribute colorscheme XMLs;
    // keep ours and silently drop duplicates from the platform side.
    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    compileKotlin {
        dependsOn(generateLexer)
        kotlinOptions.jvmTarget = "17"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileJava {
        dependsOn(generateLexer)
    }

    test {
        systemProperty("idea.test.src.dir", "${project.projectDir}/src/test/testData")
    }

    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("253.*")
        changeNotes.set("""
            <h3>1.0.4</h3>
            <ul>
              <li>Added parser support for type aliases (<code>type AssetName = Bytes;</code>)</li>
              <li>Added parser support for union types (<code>type Credential = VerKey | Script;</code>)</li>
              <li>Added parser support for anonymous record types (<code>type Addr = { hash: Bytes, staking: Bytes };</code>)</li>
              <li>Added parser support for variant tuple wrapping (<code>Some(Int)</code>)</li>
              <li>Added parser support for array type suffix (<code>Int[]</code>)</li>
              <li>Added parser support for list indexing (<code>items[0]</code>)</li>
              <li>Added parser support for ternary expressions (<code>flag ? a : b</code>)</li>
              <li>Added warning annotation for trailing comma after spread operator (<code>...source,</code>)</li>
              <li>Fixed lexer crash (InvalidStateException) during incremental re-lexing by switching from %8bit to %unicode</li>
              <li>Fixed field access on soft keywords (e.g. <code>source.amount</code>) failing after <code>.</code></li>
              <li>Fixed false trailing comma errors on transaction parameter declarations</li>
              <li>Added GitHub Actions CI pipeline with build, test, and trix-check jobs</li>
              <li>Added release and upstream compatibility check workflows</li>
              <li>Added Gradle wrapper for reproducible builds</li>
              <li>Added parser and lexer tests covering all new language constructs</li>
            </ul>
            <h3>1.0.3</h3>
            <ul>
              <li>Fixed ClassCastException crash when disabling/enabling the plugin in a live session by switching to NoSettings, eliminating classloader serialization entirely</li>
              <li>Fixed plugin icon invisible on light IDE themes; added pluginIcon_dark.svg for dark themes</li>
              <li>Replaced deprecated getDefaultCommonSettings() with customizeDefaults(); removed unused Tx3CodeStyleSettings class</li>
            </ul>
            <h3>Added in 1.0.3</h3>
            <ul>
              <li>Code folding for type declarations, env blocks, and locals blocks</li>
              <li>Code folding for inline record literals with 2 or more fields</li>
              <li>Code folding for variant construction expressions (TypeName::CaseName { … }) with 2 or more fields</li>
              <li>Dynamic plugin loading — installing and updating no longer requires an IDE restart</li>
            </ul>
            <h3>1.0.2</h3>
            <ul>
              <li>Attempted fix for ClassCastException caused by stale inlay hint Settings across classloader instances (superseded by 1.0.3)</li>
            </ul>
            <h3>1.0.1</h3>
            <ul>
              <li>Fixed plugin ID rejected by JetBrains Marketplace validator (renamed to io.txpipe.tx3)</li>
              <li>Fixed env block fields incorrectly flagged as errors due to double-brace consumption in parser</li>
              <li>Fixed builtin types (Int, Bytes, Bool, etc.) losing syntax highlighting inside type and record declarations</li>
              <li>Updated plugin icon to meet JetBrains branding guidelines</li>
            </ul>
            <h3>1.0.0</h3>
            <ul>
              <li>Initial release</li>
              <li>Syntax highlighting with semantic color tokens for all Tx3 constructs</li>
              <li>Smart code completion for keywords, block fields, types, and user-defined symbols</li>
              <li>Type-aware inlay hints showing parameter and record field types inline</li>
              <li>Code folding for tx, type, record, party, and policy blocks</li>
              <li>Structure view and file outliner for quick navigation</li>
              <li>Live templates for common Tx3 patterns</li>
              <li>Error annotations with quick-fixes for missing trailing commas</li>
              <li>Built-in Tx3 color scheme (dark theme)</li>
              <li>Auto-closing braces, brackets, and parentheses</li>
              <li>Formatter with correct 4-space indentation and block-aware enter handling</li>
              <li>New File action with Blank, Simple Transfer, and Vesting Contract templates</li>
              <li>Support for all Tx3 block types including input*, mint, burn, locals, collateral, and more</li>
              <li>Support for variant types, generic types, asset literals, and UTXO reference literals</li>
            </ul>
        """.trimIndent())
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN") ?: "")
        privateKey.set(System.getenv("PRIVATE_KEY") ?: "")
        password.set(System.getenv("PRIVATE_KEY_PASSWORD") ?: "")
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN") ?: "")
    }
}