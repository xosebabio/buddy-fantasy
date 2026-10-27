package com.buddyfantasy;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.assertj.core.api.Assertions.assertThat;

class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(BuddyFantasyApplication.class);

    @Test
    void verifiesModuleStructure() {
        modules.verify();
    }

    @Test
    void matchDependsOnlyOnLeagueSquadAndDataDefinition() {
        var match = modules.getModuleByName("match").orElseThrow();

        assertThat(match.getDirectDependencies(modules).uniqueModules().map(ApplicationModule::getName))
                .isSubsetOf("league", "squad", "datadefinition", "shared")
                .contains("league", "squad", "datadefinition");
    }

    @Test
    void squadAndLeagueDoNotDependOnMatch() {
        for (String name : new String[] {"squad", "league"}) {
            var module = modules.getModuleByName(name).orElseThrow();

            assertThat(module.getAllDependencies(modules).containsModuleNamed("match")).as(name).isFalse();
        }
    }

    @Test
    void lineupDependsOnlyOnLeagueSquadMatchAndDataDefinition() {
        var lineup = modules.getModuleByName("lineup").orElseThrow();

        assertThat(lineup.getDirectDependencies(modules).uniqueModules().map(ApplicationModule::getName))
                .isSubsetOf("league", "squad", "match", "datadefinition", "shared")
                .contains("league", "match", "datadefinition");
    }

    @Test
    void noModuleItDependsOnDependsOnLineup() {
        for (String name : new String[] {"league", "squad", "match", "datadefinition"}) {
            var module = modules.getModuleByName(name).orElseThrow();

            assertThat(module.getAllDependencies(modules).containsModuleNamed("lineup")).as(name).isFalse();
        }
    }

    @Test
    void writesModuleDocumentation() {
        new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
    }
}
