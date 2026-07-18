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

    private val rootPath = System.getProperty("project.root") ?: error("project.root not set")

    @Test
    fun `ensure classes are actually imported`() {
        val requiredClasses =
            listOf(
                "com.luisete.queda.core.designsystem.QuedaTestTags",
                "com.luisete.queda.core.testing.E2ECommandParser",
                "com.luisete.queda.core.model.quantity.ExactQuantity",
                "com.luisete.queda.core.domain.quantity.QuantityOperations",
                "com.luisete.queda.core.model.product.ProductName",
                "com.luisete.queda.core.domain.inventory.AddExactInventoryItemUseCase",
                "com.luisete.queda.core.database.QuedaDatabase",
                "com.luisete.queda.core.data.inventory.OfflineInventoryRepository",
                "com.luisete.queda.feature.inventory.InventoryViewModel",
            )
        requiredClasses.forEach { fqName ->
            assertTrue(
                "Class $fqName not found in ArchUnit import. Check classpath and dependencies.",
                allProjectClasses.any { it.name == fqName },
            )
        }
    }

    @Test
    fun `core model should not depend on android`() {
        noClasses().that().resideInAPackage("..core.model..")
            .should().dependOnClassesThat().resideInAPackage("android..")
            .check(allProjectClasses)
    }

    @Test
    fun `core domain should not depend on android or room`() {
        noClasses().that().resideInAPackage("..core.domain..")
            .should().dependOnClassesThat().resideInAPackage("android..")
            .orShould().dependOnClassesThat().resideInAPackage("androidx..")
            .orShould().dependOnClassesThat().resideInAPackage("androidx.room..")
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
            .check(allProjectClasses)
    }

    @Test
    fun `no global scope usage`() {
        noClasses().should().dependOnClassesThat().haveFullyQualifiedName("kotlinx.coroutines.GlobalScope")
            .check(allProjectClasses)
    }

    @Test
    fun `no com example package`() {
        noClasses().should().resideInAPackage("com.ex" + "ample..")
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
            assertNoProductImports(file, content)

            if (path.contains("core${File.separator}model") || path.contains("core${File.separator}domain")) {
                assertPureKotlinModule(file, content)
            }
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
        assertFalse("File ${file.path} contains forbidden token", content.contains("TO" + "DO"))
        assertFalse("File ${file.path} contains forbidden token", content.contains("FIX" + "ME"))
    }

    private fun assertNoRestrictedPatterns(
        file: File,
        content: String,
    ) {
        assertFalse("File ${file.path} contains forbidden token", content.contains("com.ex" + "ample"))
        // Restricted patterns checked only if not this test itself
        if (!file.path.contains("ArchitectureTest.kt")) {
            assertFalse("File ${file.path} contains allowMainThreadQueries", content.contains("allowMainThreadQueries"))
            assertFalse("File ${file.path} contains fallbackToDestructiveMigration", content.contains("fallbackToDestructiveMigration"))
        }
    }

    private fun assertNoProductImports(
        file: File,
        content: String,
    ) {
        if (file.path.contains("app${File.separator}src${File.separator}main")) {
            val restricted = listOf("Dao", "RoomDatabase", "Repository")
            restricted.forEach { word ->
                assertFalse(
                    "App main should not import product pattern: $word in ${file.path}",
                    content.contains("import .*$word".toRegex()),
                )
            }
        }
    }

    private fun assertPureKotlinModule(
        file: File,
        content: String,
    ) {
        val forbidden = listOf("android.", "androidx.", "androidx.room")
        forbidden.forEach { pattern ->
            assertFalse("Pure module ${file.path} contains forbidden import: $pattern", content.contains("import $pattern"))
        }
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

    @Test
    fun `quantity domain must not use floating point`() {
        val files =
            requiredKotlinFiles(
                listOf(
                    "core/model/src/main/kotlin/com/luisete/queda/core/model/quantity/",
                    "core/domain/src/main/kotlin/com/luisete/queda/core/domain/quantity/",
                ),
            )

        files.forEach { file ->
            val content = file.readText()
            assertFalse("File ${file.path} uses Double", content.contains("Double"))
            assertFalse("File ${file.path} uses Float", content.contains("Float"))
            assertFalse("File ${file.path} uses toDouble", content.contains("toDouble"))
            assertFalse("File ${file.path} uses toFloat", content.contains("toFloat"))
        }
    }

    @Test
    fun `quantity domain must not use enum ordinal or forbidden abstractions`() {
        val files =
            requiredKotlinFiles(
                listOf(
                    "core/model/src/main/kotlin/com/luisete/queda/core/model/id/",
                    "core/model/src/main/kotlin/com/luisete/queda/core/model/quantity/",
                    "core/domain/src/main/kotlin/com/luisete/queda/core/domain/quantity/",
                    "core/domain/src/main/kotlin/com/luisete/queda/core/domain/result/",
                ),
            )

        files.forEach { file ->
            val content = file.readText()
            assertFalse("File ${file.path} uses enum ordinal", content.contains(".ordinal"))
            assertFalse("File ${file.path} contains InvalidQuantityText", content.contains("InvalidQuantityText"))
            assertFalse("File ${file.path} contains GenericId", content.contains("GenericId"))
            assertFalse("File ${file.path} contains GenericError", content.contains("GenericError"))
        }
    }

    @Test
    fun `design system should be independent`() {
        noClasses().that().resideInAPackage("..core.designsystem..")
            .should().dependOnClassesThat().resideInAPackage("..core.model..")
            .orShould().dependOnClassesThat().resideInAPackage("..core.domain..")
            .orShould().dependOnClassesThat().resideInAPackage("..core.data..")
            .orShould().dependOnClassesThat().resideInAPackage("..core.database..")
            .orShould().dependOnClassesThat().resideInAPackage("..feature..")
            .orShould().dependOnClassesThat().resideInAPackage("..androidx.room..")
            .orShould().dependOnClassesThat().resideInAPackage("..androidx.navigation..")
            .orShould().dependOnClassesThat().haveSimpleNameEndingWith("ViewModel")
            .check(allProjectClasses)
    }

    @Test
    fun `design system production should not contain feature literals or test tags`() {
        val root = File(rootPath)
        val dsProd = File(root, "core/designsystem/src/main/kotlin/com/luisete/queda/core/designsystem")
        assertTrue("Design system prod directory not found", dsProd.exists())

        val forbiddenStrings = listOf("inventory_", "add_exact_item", "loadingTestTag", "supportingTextTestTag")

        dsProd.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val content = file.readText()
            forbiddenStrings.forEach { forbidden ->
                assertFalse(
                    "File ${file.path} contains forbidden feature-specific or test-specific string: $forbidden",
                    content.contains(forbidden),
                )
            }
        }
    }

    @Test
    fun `inventory feature must not reference room entities or dao`() {
        noClasses().that().resideInAPackage("..feature.inventory..")
            .should().dependOnClassesThat().resideInAPackage("..core.database..")
            .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Entity")
            .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Dao")
            .orShould().dependOnClassesThat().haveSimpleName("QuedaDatabase")
            .orShould().dependOnClassesThat().resideInAPackage("androidx.room..")
            .orShould().dependOnClassesThat().resideInAPackage("androidx.sqlite..")
            .check(allProjectClasses)
    }

    @Test
    fun `inventory viewmodels must not reference android context nav controller or room`() {
        noClasses().that().resideInAPackage("..feature.inventory..")
            .and().haveSimpleNameEndingWith("ViewModel")
            .should().dependOnClassesThat().resideInAPackage("android.content..")
            .orShould().dependOnClassesThat().resideInAPackage("androidx.navigation..")
            .orShould().dependOnClassesThat().resideInAPackage("androidx.room..")
            .check(allProjectClasses)
    }

    @Test
    fun `room annotations must remain inside core database`() {
        noClasses().that().resideOutsideOfPackage("..core.database..")
            .should().dependOnClassesThat().resideInAPackage("androidx.room..")
            .check(allProjectClasses)
    }

    @Test
    fun `inventory repository implementation must remain inside core data`() {
        noClasses().that().haveSimpleName("OfflineInventoryRepository")
            .should().resideOutsideOfPackage("..core.data.inventory..")
            .check(allProjectClasses)
    }

    @Test
    fun `inventory domain contracts must not import android room compose hilt or sqlite`() {
        noClasses().that().resideInAPackage("..core.domain.inventory..")
            .should().dependOnClassesThat().resideInAPackage("android..")
            .orShould().dependOnClassesThat().resideInAPackage("androidx.room..")
            .orShould().dependOnClassesThat().resideInAPackage("androidx.compose..")
            .orShould().dependOnClassesThat().resideInAPackage("dagger.hilt..")
            .orShould().dependOnClassesThat().resideInAPackage("androidx.sqlite..")
            .check(allProjectClasses)
    }

    @Test
    fun `app must not reference inventory dao product entity or stock item entity`() {
        val files =
            requiredKotlinFiles(
                listOf(
                    "app/src/main/java/com/luisete/queda/",
                ),
            )
        files.forEach { file ->
            val content = file.readText()
            assertFalse("File ${file.path} references InventoryDao", content.contains("InventoryDao"))
            assertFalse("File ${file.path} references ProductEntity", content.contains("ProductEntity"))
            assertFalse("File ${file.path} references StockItemEntity", content.contains("StockItemEntity"))
        }
    }

    @Test
    fun `production source must not contain fake inventory repository or e2e test control`() {
        val files =
            requiredKotlinFiles(
                listOf(
                    "core/model/src/main/kotlin/",
                    "core/domain/src/main/kotlin/",
                    "core/data/src/main/kotlin/",
                    "feature/inventory/src/main/kotlin/",
                    "core/database/src/main/kotlin/",
                    "app/src/main/java/",
                ),
            )

        files.forEach { file ->
            val content = file.readText()
            assertFalse("Production file ${file.path} contains Fake", file.name.contains("Fake"))
            assertFalse("Production file ${file.path} contains E2ETestControl", content.contains("E2ETestControl"))
        }
    }

    @Test
    fun `inventory feature must not depend on core database via build gradle`() {
        val buildFile = File(rootPath, "feature/inventory/build.gradle.kts")
        assertTrue(buildFile.exists())
        val content = buildFile.readText()
        assertFalse("feature:inventory depends on core:database", content.contains("project(\":core:database\")"))
    }

    private fun requiredKotlinFiles(relativePaths: List<String>): List<File> {
        val root = File(rootPath)
        val result =
            relativePaths.flatMap { relativePath ->
                val directory = File(root, relativePath)
                assertTrue("Required source directory does not exist: " + directory.path, directory.exists())
                assertTrue("Required source path is not a directory: " + directory.path, directory.isDirectory)
                val files = directory.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
                assertTrue("No Kotlin production files found in: " + directory.path, files.isNotEmpty())
                files
            }
        assertTrue("No production Kotlin files were inspected", result.isNotEmpty())
        return result
    }
}
