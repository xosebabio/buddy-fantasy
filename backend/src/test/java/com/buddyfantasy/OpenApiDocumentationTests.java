package com.buddyfantasy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.buddyfantasy.ApiEndpoints.Endpoint;
import com.buddyfantasy.shared.domain.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static com.buddyfantasy.TestUsers.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every endpoint is documented in the OpenAPI document served at {@code /v3/api-docs}: description, responses,
 * parameters and every field of its bodies with a description and an example. The scans walk the generated document,
 * so the endpoints and bodies added later are covered without listing them.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfig.class)
class OpenApiDocumentationTests {

    /** The operation keys of an OpenAPI path item. */
    private static final Set<String> METHODS = Set.of("get", "put", "post", "delete", "patch");

    @Autowired
    MockMvc mvc;

    @Autowired
    TestFixtures fixtures;

    @Autowired
    ObjectMapper json;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    JsonNode document;

    record Operation(String key, String path, JsonNode node) {
    }

    @BeforeEach
    void readDocument() throws Exception {
        String body = mvc.perform(get("/v3/api-docs").with(as(fixtures.user("ana"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        document = json.readTree(body);
    }

    private Stream<Operation> operations() {
        return fields(document.path("paths"))
                .flatMap(path -> fields(path.getValue())
                        .filter(operation -> METHODS.contains(operation.getKey()))
                        .map(operation -> new Operation(operation.getKey().toUpperCase() + " " + path.getKey(),
                                path.getKey(), operation.getValue())));
    }

    private static Stream<Map.Entry<String, JsonNode>> fields(JsonNode node) {
        return StreamSupport.stream(node.properties().spliterator(), false);
    }

    private static Stream<JsonNode> elements(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false);
    }

    private static boolean hasText(JsonNode node, String field) {
        return !node.path(field).asText().isBlank();
    }

    @Test
    @DisplayName("Every API endpoint is in the OpenAPI document")
    void everyApiEndpointIsInTheDocument() {
        List<Endpoint> documented = operations()
                .map(operation -> new Endpoint(HttpMethod.valueOf(operation.key().split(" ")[0]), operation.path()))
                .toList();

        assertThat(documented).containsExactlyInAnyOrderElementsOf(ApiEndpoints.scan(mappings));
    }

    @Test
    @DisplayName("Every operation has a tag, a summary, a description and documented responses")
    void everyOperationIsDescribed() {
        List<String> missing = new ArrayList<>();
        operations().forEach(operation -> {
            JsonNode node = operation.node();
            if (!hasText(node, "summary") || !hasText(node, "description")) {
                missing.add(operation.key() + ": summary and description (@Operation)");
            }
            if (elements(node.path("tags")).anyMatch(tag -> tag.asText().endsWith("-controller"))) {
                missing.add(operation.key() + ": tag (@Tag on the controller)");
            }
            JsonNode responses = node.path("responses");
            if (fields(responses).noneMatch(response -> response.getKey().startsWith("2"))) {
                missing.add(operation.key() + ": a success response (@ApiResponse)");
            }
            fields(responses)
                    .filter(response -> response.getValue().path("description").asText().equals("OK"))
                    .forEach(response -> missing.add(operation.key() + ": description of the "
                            + response.getKey() + " response (@ApiResponse)"));
            if (operation.path().startsWith("/api/leagues/{id}") && !responses.has("403")) {
                missing.add(operation.key() + ": the 403 of a league endpoint (@ApiResponse)");
            }
        });

        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("Every path and query parameter has a description and an example")
    void everyParameterIsDescribed() {
        List<String> missing = operations()
                .flatMap(operation -> elements(operation.node().path("parameters"))
                        .filter(parameter -> !hasText(parameter, "description") || !parameter.has("example"))
                        .map(parameter -> operation.key() + ": " + parameter.path("name").asText() + " (@Parameter)"))
                .toList();

        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("Every error response has an example of its problem detail")
    void everyErrorResponseHasAnExample() {
        List<String> missing = operations()
                .flatMap(operation -> fields(operation.node().path("responses"))
                        .filter(response -> response.getKey().matches("4\\d\\d|5\\d\\d"))
                        .filter(response -> !response.getKey().equals("401"))
                        .filter(response -> elements(response.getValue().path("content"))
                                .noneMatch(media -> media.has("example") || media.has("examples")))
                        .map(response -> operation.key() + ": " + response.getKey()))
                .toList();

        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("The ErrorCode enum has the codes of every module and is the code of every problem")
    void errorCodeEnumHasEveryCode() {
        Set<String> codes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.buddyfantasy").stream()
                .filter(type -> type.isEnum() && type.isAssignableTo(ErrorCode.class))
                .flatMap(type -> type.getEnumConstants().stream().map(JavaEnumConstant::name))
                .collect(Collectors.toSet());
        List<String> published = elements(document.at("/components/schemas/ErrorCode/enum"))
                .map(JsonNode::asText)
                .toList();

        assertThat(codes).contains("MAX_CHANGES_EXCEEDED", "MATCHDAY_TAKEN", "SHIRT_NUMBER_TAKEN", "ADMIN_CANNOT_LEAVE");
        assertThat(published).doesNotHaveDuplicates().isSorted().containsExactlyInAnyOrderElementsOf(codes);
        assertThat(document.at("/components/schemas/Problem/properties/code/allOf/0/$ref").asText())
                .isEqualTo("#/components/schemas/ErrorCode");
    }

    @Test
    @DisplayName("Every schema and field has a description, and every value field an example")
    void everySchemaFieldIsDescribed() {
        List<String> missing = new ArrayList<>();
        fields(document.path("components").path("schemas")).forEach(schema -> {
            if (!hasText(schema.getValue(), "description")) {
                missing.add(schema.getKey() + ": description (@Schema on the record)");
            }
            fields(schema.getValue().path("properties")).forEach(property -> {
                JsonNode field = property.getValue();
                String name = schema.getKey() + "." + property.getKey();
                if (!hasText(field, "description")) {
                    missing.add(name + ": description");
                }
                boolean value = !field.has("allOf") && !field.has("$ref")
                        && !Set.of("object", "array").contains(field.path("type").asText());
                if (value && !field.has("example")) {
                    missing.add(name + ": example");
                }
            });
        });

        assertThat(missing).isEmpty();
    }
}
