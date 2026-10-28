package com.buddyfantasy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static com.buddyfantasy.TestUsers.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The frontend types are generated from {@value #SPEC} (FE-05), so it always matches {@code /v3/api-docs}. This writes
 * the served document there and fails if it was different, like {@link MigrationChecksumsTests}: running it
 * regenerates the file, and CI fails if a change to the API forgot to commit it. Paths are relative to {@code backend/},
 * the working directory of Maven.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class OpenApiSpecTests {

    static final String SPEC = "api/openapi.json";

    private static final Path FILE = Path.of("..").resolve(SPEC).normalize();

    @Autowired
    MockMvc mvc;

    @Autowired
    TestFixtures fixtures;

    private String served() throws Exception {
        String body = mvc.perform(get("/v3/api-docs").with(as(fixtures.user("ana"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return body.endsWith("\n") ? body : body + "\n";
    }

    private static String versioned() throws IOException {
        return Files.exists(FILE) ? Files.readString(FILE, StandardCharsets.UTF_8) : "";
    }

    @Test
    @DisplayName("The versioned OpenAPI document is up to date")
    void versionedDocumentIsUpToDate() throws Exception {
        String served = served();
        String versioned = versioned();
        if (!served.equals(versioned)) {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, served, StandardCharsets.UTF_8);
        }

        assertThat(versioned)
                .as(SPEC + " was out of date and has been regenerated: commit it (and run npm run api:types)")
                .isEqualTo(served);
    }
}
