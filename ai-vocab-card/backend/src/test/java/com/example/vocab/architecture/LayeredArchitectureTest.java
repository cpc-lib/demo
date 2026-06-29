package com.example.vocab.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

class LayeredArchitectureTest {
    @Test
    void controllersShouldNotAccessMappersDirectly() {
        var classes = new ClassFileImporter().importPackages("com.example.vocab");
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..controller..")
                .should().accessClassesThat().resideInAPackage("..mapper..")
                .check(classes);
    }
}
