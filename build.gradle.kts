import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.2"
    id("org.jetbrains.grammarkit") version "2022.3.2"
}

group = "io.txpipe"
version = "1.0.0"

repositories {
    mavenCentral()
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
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    // Fix: IntelliJ platform and our resources both contribute colorscheme XMLs;
    // keep ours and silently drop duplicates from the platform side.
    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    compileKotlin {
        dependsOn(generateLexer)
        kotlinOptions.jvmTarget = "17"
    }
    compileJava {
        dependsOn(generateLexer)
    }

    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("253.*")
        changeNotes.set("""
            <h3>1.0.0</h3>
            <ul>
              <li>Initial release</li>
              <li>Syntax highlighting with semantic color tokens</li>
              <li>Smart code completion for keywords, types, and user-defined symbols</li>
              <li>Type-aware inlay hints on inputs/outputs</li>
              <li>Code folding for tx, record, party, policy blocks</li>
              <li>Structure view &amp; file outliner</li>
              <li>Live templates for common patterns</li>
              <li>Error annotations and quick fixes for missing trailing commas</li>
              <li>Built-in Tx3 color scheme</li>
              <li>Auto-closing braces, brackets, and parentheses</li>
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