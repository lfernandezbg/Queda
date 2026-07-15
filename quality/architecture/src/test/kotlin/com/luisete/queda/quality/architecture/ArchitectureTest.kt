package com.luisete.queda.quality.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@Suppress("LargeClass", "MaxLineLength", "ComplexMethod", "TooManyFunctions")
class ArchitectureTest {
    private val allProjectClasses =
        ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.luisete.queda")

    private val rootPath = System.getProperty("project.root") ?: throw IllegalStateException("project.root not set")

    @Test
    fun `core model should not depend on android`() {
        noClasses().that().resideInAPackage("..core.model..")
            .should().dependOnClassesThat().resideInAPackage("android..")
            .allowEmptyShould(true)
            .check(allProjectClasses)
    }

    @Test
    fun `core domain should not depend on android or room`() {
        noClasses().that().resideInAPackage("..core.domain..")
            .should().dependOnClassesThat().resideInAPackage("android..")
            .orShould().dependOnClassesThat().resideInAPackage("androidx..")
            .orShould().dependOnClassesThat().resideInAPackage("androidx.room..")
            .allowEmptyShould(true)
            .check(allProjectClasses)
    }

    @Test
    fun `feature modules should be isolated`() {
        slices().matching("com.luisete.queda.feature.(*)..")
            .should().notDependOnEachOther()
            .allowEmptyShould(true)
            .check(allProjectClasses)
    }

    @Test
    fun `feature modules should not depend on core database`() {
        noClasses().that().resideInAPackage("..feature..")
            .should().dependOnClassesThat().resideInAPackage("..core.database..")
            .allowEmptyShould(true)
            .check(allProjectClasses)
    }

    @Test
    fun `no global scope usage`() {
        noClasses().should().dependOnClassesThat().haveFullyQualifiedName("kotlinx.coroutines.GlobalScope")
            .check(allProjectClasses)
    }

    @Test
    fun `no com example package`() {
        noClasses().should().resideInAPackage("com.example..")
            .check(allProjectClasses)
    }

    @Test
    fun `no restricted base classes or patterns`() {
        noClasses().should().haveSimpleName("BaseViewModel")
            .orShould().haveSimpleName("BaseRepository")
            .orShould().haveSimpleName("BaseUseCase")
            .orShould().haveSimpleName("ServiceLocator")
            .check(allProjectClasses)
    }

    @Test
    fun `file system inspections`() {
        val root = File(rootPath)
        val skipDirs = listOf(".idea", ".gradle", "build", ".maestro", "scripts")

        root.walkTopDown().onEnter { !skipDirs.contains(it.name) }.forEach { file ->
            checkFile(file)
        }

        checkReleaseIsolation(root)
        checkAppMainIsolation(root)
    }

    private fun checkFile(file: File) {
        val path = file.path
        if (path.contains("quality${File.separator}architecture")) return

        val ext = file.extension
        if (ext !in listOf("kt", "java", "xml", "kts")) return

        val content by lazy { file.readText() }

        if (file.name == "build.gradle.kts" && path.contains("feature")) {
            assertFeatureIsolation(file, content)
        }

        if (ext in listOf("kt", "java") && path.contains("src${File.separator}main")) {
            assertNoTodo(file, content)
        }

        assertNoRestrictedPatterns(file, content)
    }

    private fun assertFeatureIsolation(
        file: File,
        content: String,
    ) {
        val parentName = file.parentFile?.name
        assertFalse("Feature $parentName depends on feature", content.contains("project(\":feature:"))
        assertFalse("Feature $parentName depends on database", content.contains("project(\":core:database\")"))
    }

    private fun assertNoTodo(
        file: File,
        content: String,
    ) {
        assertFalse("File ${file.path} contains TODO", content.contains("TODO"))
        assertFalse("File ${file.path} contains FIXME", content.contains("FIXME"))
    }

    private fun assertNoRestrictedPatterns(
        file: File,
        content: String,
    ) {
        assertFalse("File ${file.path} contains com.example", content.contains("com.example"))
        assertFalse("File ${file.path} contains allowMainThreadQueries", content.contains("allowMainThreadQueries"))
        assertFalse("File ${file.path} contains fallbackToDestructiveMigration", content.contains("fallbackToDestructiveMigration"))
    }

    private fun checkReleaseIsolation(root: File) {
        val releaseManifestDir = File(root, "app/src/release")
        if (releaseManifestDir.exists()) {
            releaseManifestDir.walkTopDown().forEach { file ->
                assertFalse("Release source set contains E2E: ${file.path}", file.path.contains("e2e"))
            }
        }
    }

    private fun checkAppMainIsolation(root: File) {
        val appMainSrc = File(root, "app/src/main/java/com/luisete/queda")
        if (appMainSrc.exists()) {
            val forbidden = listOf("domain", "data", "database")
            appMainSrc.listFiles()?.forEach { file ->
                if (file.isDirectory && forbidden.contains(file.name)) {
                    throw AssertionError("App module contains forbidden package: ${file.name}")
                }
            }
        }
    }

    @Test
    fun `verify module existence`() {
        val requiredModules =
            listOf(
                ":app", ":core:model", ":core:domain", ":core:database", ":core:data",
                ":core:designsystem", ":core:testing", ":feature:onboarding",
                ":feature:today", ":feature:inventory", ":feature:shopping",
                ":feature:settings", ":quality:architecture", ":benchmark", ":baselineprofile",
            )

        val settingsFile = File(rootPath, "settings.gradle.kts")
        assertTrue("settings.gradle.kts not found", settingsFile.exists())
        val content = settingsFile.readText()

        requiredModules.forEach { module ->
            assertTrue("Module $module missing in settings.gradle.kts", content.contains("include(\"$module\")"))
        }
    }
}
