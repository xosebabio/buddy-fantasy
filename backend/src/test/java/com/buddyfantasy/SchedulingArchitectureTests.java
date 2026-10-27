package com.buddyfantasy;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/** RNF-03: with several instances, every scheduled job must run in only one of them at a time. */
@AnalyzeClasses(packages = "com.buddyfantasy", importOptions = ImportOption.DoNotIncludeTests.class)
class SchedulingArchitectureTests {

    /** Public, since ShedLock locks through a Spring proxy, which only intercepts public methods reliably. */
    @ArchTest
    static final ArchRule everyScheduledJobRunsUnderAShedLock = methods().that().areAnnotatedWith(Scheduled.class)
            .should().beAnnotatedWith(SchedulerLock.class)
            .andShould().bePublic();
}
