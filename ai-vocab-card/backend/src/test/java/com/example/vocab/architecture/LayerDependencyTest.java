package com.example.vocab.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.example.vocab")
public class LayerDependencyTest {
    @ArchTest
    static final ArchRule controllers_should_not_access_mappers = noClasses()
            .that().resideInAPackage("..controller..")
            .should().accessClassesThat().resideInAPackage("..mapper..");
}
