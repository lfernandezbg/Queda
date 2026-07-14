package com.luisete.queda.quality.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.Test

class ArchitectureTest {

    private val rootPackage = "com.luisete.queda"

    @Test
    fun `domain should not depend on android`() {
        val importedClasses = ClassFileImporter().importPackages(rootPackage)

        val rule = noClasses()
            .that().resideInAPackage("..core.domain..")
            .should().dependOnClassesThat().resideInAPackage("android..")

        rule.check(importedClasses)
    }

    @Test
    fun `model should not depend on android`() {
        val importedClasses = ClassFileImporter().importPackages(rootPackage)

        val rule = noClasses()
            .that().resideInAPackage("..core.model..")
            .should().dependOnClassesThat().resideInAPackage("android..")

        rule.check(importedClasses)
    }
}
