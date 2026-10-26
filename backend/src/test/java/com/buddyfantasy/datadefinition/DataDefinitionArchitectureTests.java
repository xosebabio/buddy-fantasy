package com.buddyfantasy.datadefinition;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** {@code datadefinition} is a leaf module holding only logic-free enums and records. */
class DataDefinitionArchitectureTests {

    private static final String PACKAGE = "com.buddyfantasy.datadefinition..";
    private static final String[] OTHER_MODULES = {
            "com.buddyfantasy.identity..", "com.buddyfantasy.league..", "com.buddyfantasy.squad..",
            "com.buddyfantasy.match..", "com.buddyfantasy.rating..", "com.buddyfantasy.lineup..",
            "com.buddyfantasy.scoring..", "com.buddyfantasy.market..", "com.buddyfantasy.notification..",
            "com.buddyfantasy.shared.."};

    private static JavaClasses imported;

    @BeforeAll
    static void importClasses() {
        imported = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.buddyfantasy");
    }

    @Test
    void doesNotDependOnAnyOtherModule() {
        noClasses().that().resideInAPackage(PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage(OTHER_MODULES)
                .allowEmptyShould(true)
                .check(imported);
    }

    @Test
    void hasNoSpringBeansNorPersistence() {
        noClasses().that().resideInAPackage(PACKAGE)
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.stereotype..", "org.springframework.beans..",
                        "org.springframework.context..", "jakarta.persistence..")
                .allowEmptyShould(true)
                .check(imported);
    }

    @Test
    void containsOnlyEnumsAndRecords() {
        classes().that().resideInAPackage(PACKAGE)
                .and().haveSimpleNameNotEndingWith("package-info")
                .should().beEnums().orShould().beRecords()
                .allowEmptyShould(true)
                .check(imported);
    }

    @Test
    void isNotEmpty() {
        classes().that().resideInAPackage(PACKAGE)
                .and().haveSimpleNameNotEndingWith("package-info")
                .should().beEnums()
                .check(imported);
    }
}
