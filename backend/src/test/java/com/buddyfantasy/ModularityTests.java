package com.buddyfantasy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static org.assertj.core.api.Assertions.assertThat;

class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(BuddyFantasyApplication.class);

    @Test
    @DisplayName("The module structure is valid")
    void verifiesModuleStructure() {
        modules.verify();
    }

    @Test
    @DisplayName("match depends only on league, squad and datadefinition")
    void matchDependsOnlyOnLeagueSquadAndDataDefinition() {
        var match = modules.getModuleByName("match").orElseThrow();

        assertThat(match.getDirectDependencies(modules).uniqueModules().map(ApplicationModule::getName))
                .isSubsetOf("league", "squad", "datadefinition", "shared")
                .contains("league", "squad", "datadefinition");
    }

    @Test
    @DisplayName("squad and league do not depend on match")
    void squadAndLeagueDoNotDependOnMatch() {
        for (String name : new String[] {"squad", "league"}) {
            var module = modules.getModuleByName(name).orElseThrow();

            assertThat(module.getAllDependencies(modules).containsModuleNamed("match")).as(name).isFalse();
        }
    }

    @Test
    @DisplayName("lineup depends only on league, squad, match and datadefinition")
    void lineupDependsOnlyOnLeagueSquadMatchAndDataDefinition() {
        var lineup = modules.getModuleByName("lineup").orElseThrow();

        assertThat(lineup.getDirectDependencies(modules).uniqueModules().map(ApplicationModule::getName))
                .isSubsetOf("league", "squad", "match", "datadefinition", "shared")
                .contains("league", "match", "datadefinition");
    }

    @Test
    @DisplayName("No module that lineup depends on depends back on lineup")
    void noModuleItDependsOnDependsOnLineup() {
        for (String name : new String[] {"league", "squad", "match", "datadefinition"}) {
            var module = modules.getModuleByName(name).orElseThrow();

            assertThat(module.getAllDependencies(modules).containsModuleNamed("lineup")).as(name).isFalse();
        }
    }

    @Test
    @DisplayName("scoring depends only on league, squad, match, lineup and datadefinition")
    void scoringDependsOnlyOnLeagueSquadMatchLineupAndDataDefinition() {
        var scoring = modules.getModuleByName("scoring").orElseThrow();

        assertThat(scoring.getDirectDependencies(modules).uniqueModules().map(ApplicationModule::getName))
                .isSubsetOf("league", "squad", "match", "lineup", "datadefinition", "shared")
                .contains("league", "datadefinition");
    }

    @Test
    @DisplayName("No module that scoring depends on depends back on scoring")
    void noModuleItDependsOnDependsOnScoring() {
        for (String name : new String[] {"league", "squad", "match", "lineup", "datadefinition"}) {
            var module = modules.getModuleByName(name).orElseThrow();

            assertThat(module.getAllDependencies(modules).containsModuleNamed("scoring")).as(name).isFalse();
        }
    }

    @Test
    @DisplayName("Writes the module documentation")
    void writesModuleDocumentation() {
        new Documenter(modules).writeModulesAsPlantUml().writeIndividualModulesAsPlantUml();
    }
}
