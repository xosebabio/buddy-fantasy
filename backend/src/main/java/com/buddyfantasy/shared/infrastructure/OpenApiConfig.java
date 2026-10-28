package com.buddyfantasy.shared.infrastructure;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.buddyfantasy.shared.domain.ErrorCode;
import com.buddyfantasy.shared.domain.UnexpectedException;
import com.buddyfantasy.shared.web.ApiExamples;
import com.buddyfantasy.shared.web.ApiProblem;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

/**
 * The API description served at {@code /v3/api-docs}. Each controller documents its own operations, parameters and
 * responses; this adds what every operation shares, so the controllers do not repeat it: the session and CSRF
 * requirements, the {@code 401} without a session, and the {@link ApiProblem} body and a generic example on every
 * error response that does not bring its own, with its {@code code} as the {@code ErrorCode} enum: the codes of every
 * module (FE-08). The server is the relative {@code /}, not the one springdoc deduces from
 * the request, so the document is the same whatever the host and port (it is versioned in {@code api/openapi.json}).
 */
@Configuration
class OpenApiConfig {

    private static final String SESSION = "session";
    private static final String CSRF = "csrf";
    private static final String ERROR_CODE = "ErrorCode";
    /** Each module keeps its {@link ErrorCode} enum in its domain package; anything else (tests) is left out. */
    private static final Pattern ERROR_CODE_TYPE = Pattern.compile("com\\.buddyfantasy\\.\\w+\\.domain\\.\\w+");
    private static final String PROBLEM_JSON = org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final Set<PathItem.HttpMethod> MUTATING = Set.of(PathItem.HttpMethod.POST, PathItem.HttpMethod.PUT,
            PathItem.HttpMethod.PATCH, PathItem.HttpMethod.DELETE);
    private static final Map<String, String> PROBLEM_EXAMPLES = Map.of(
            "400", ApiExamples.BAD_REQUEST,
            "403", ApiExamples.FORBIDDEN,
            "404", ApiExamples.NOT_FOUND,
            "409", ApiExamples.CONFLICT,
            "500", ApiExamples.INTERNAL_ERROR);

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("buddy-fantasy API")
                        .version("v1")
                        .description("""
                                API del fantasy de una liga de peñas: un administrador registra el equipo real, sus \
                                partidos y estadísticas, y los managers alinean, valoran y compiten en una \
                                clasificación.

                                - **Sesión:** se inicia con Google en `/oauth2/authorization/google` y viaja en la \
                                cookie `SESSION`. Sin sesión, todo `/api/**` responde `401`.
                                - **CSRF:** las peticiones que modifican (`POST`, `PUT`, `DELETE`) copian la cookie \
                                `XSRF-TOKEN` en la cabecera `X-XSRF-TOKEN`; sin ella responden `403`.
                                - **Roles por liga:** cada endpoint de `/api/leagues/{id}` dice su rol mínimo. El \
                                administrador también juega como manager.
                                - **Fechas** en ISO-8601 y UTC; la interfaz las muestra en `Europe/Madrid`.
                                - **Importes** (valores, presupuestos) en millones.
                                - **Errores** en `application/problem+json` (RFC 9457), con un `code` cuando la \
                                interfaz debe distinguir el motivo."""))
                .servers(List.of(new Server().url("/").description("El mismo origen que sirve la interfaz.")))
                .components(new Components()
                        .addSecuritySchemes(SESSION, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name("SESSION")
                                .description("Cookie de sesión tras el login con Google."))
                        .addSecuritySchemes(CSRF, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("X-XSRF-TOKEN")
                                .description("Valor de la cookie `XSRF-TOKEN`, obligatorio en las peticiones que "
                                        + "modifican.")))
                .addSecurityItem(new SecurityRequirement().addList(SESSION));
    }

    @Bean
    OpenApiCustomizer sharedResponses() {
        return openApi -> {
            // swagger-core copies the nullable of the first property that uses a schema to the schema itself
            openApi.getComponents().getSchemas().values().forEach(schema -> schema.setNullable(null));
            ModelConverters.getInstance().read(ApiProblem.class).forEach(openApi.getComponents()::addSchemas);
            withErrorCodes(openApi.getComponents());
            Schema<?> problem = new Schema<>().$ref("#/components/schemas/Problem");
            openApi.getPaths().values().forEach(path -> path.readOperationsMap().forEach((method, operation) -> {
                if (MUTATING.contains(method)) {
                    operation.addSecurityItem(new SecurityRequirement().addList(SESSION).addList(CSRF));
                }
                ApiResponses responses = operation.getResponses();
                responses.putIfAbsent("401", new ApiResponse().description("No hay sesión iniciada."));
                responses.forEach((code, response) -> {
                    if (PROBLEM_EXAMPLES.containsKey(code)) {
                        withProblemBody(response, code, problem);
                    }
                });
            }));
        };
    }

    /**
     * OpenAPI 3.0 ignores whatever sits next to a {@code $ref}, so a property of another schema would lose its
     * description and {@code nullable}: they go next to an {@code allOf} with the reference instead.
     */
    @Bean
    PropertyCustomizer describedReferences() {
        return (property, type) -> {
            if (property == null || property.get$ref() == null || type.getCtxAnnotations() == null) {
                return property;
            }
            return Arrays.stream(type.getCtxAnnotations())
                    .filter(io.swagger.v3.oas.annotations.media.Schema.class::isInstance)
                    .map(io.swagger.v3.oas.annotations.media.Schema.class::cast)
                    .filter(annotation -> !annotation.description().isEmpty())
                    .findFirst()
                    .<Schema>map(annotation -> new ComposedSchema()
                            .addAllOfItem(new Schema<>().$ref(property.get$ref()))
                            .description(annotation.description())
                            .nullable(annotation.nullable() ? Boolean.TRUE : null))
                    .orElse(property);
        };
    }

    /**
     * Publishes the codes as the {@code ErrorCode} enum and makes the {@code code} of the problem reference it, so
     * the client can have a text for each one. Found by scanning the classpath, so {@code shared} does not depend on
     * the modules; a code repeated in several modules means the same in all of them and appears once.
     */
    private static void withErrorCodes(Components components) {
        components.addSchemas(ERROR_CODE, new StringSchema()
                ._enum(errorCodes())
                .description("Motivo de un error para la interfaz. Siempre en los `409` y en los `400` que puede "
                        + "provocar un formulario (un campo obligatorio que falta va en `violations`, sin código); "
                        + "cada endpoint documenta los suyos. Un mismo código significa lo mismo en cualquier "
                        + "endpoint.")
                .example("MAX_CHANGES_EXCEEDED"));
        Map<String, Schema> properties = components.getSchemas().get("Problem").getProperties();
        Schema<?> code = properties.get("code");
        properties.put("code", new ComposedSchema()
                .addAllOfItem(new Schema<>().$ref("#/components/schemas/" + ERROR_CODE))
                .description(code.getDescription())
                .nullable(code.getNullable()));
    }

    /** The names of every {@link ErrorCode}, sorted so the document does not depend on the scan order. */
    static List<String> errorCodes() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(ErrorCode.class));
        return scanner.findCandidateComponents("com.buddyfantasy").stream()
                .map(BeanDefinition::getBeanClassName)
                .filter(name -> ERROR_CODE_TYPE.matcher(name).matches())
                .flatMap(name -> constants(ClassUtils.resolveClassName(name, OpenApiConfig.class.getClassLoader())))
                .distinct()
                .sorted()
                .toList();
    }

    private static Stream<String> constants(Class<?> type) {
        Object[] constants = type.getEnumConstants();
        if (constants == null) {
            throw new UnexpectedException(type.getName() + " implements ErrorCode but is not an enum");
        }
        return Arrays.stream(constants).map(ErrorCode.class::cast).map(ErrorCode::name);
    }

    /**
     * Keeps the examples of the controller, if any, under the media type that errors really have. Without them,
     * springdoc may have put the success body there.
     */
    private static void withProblemBody(ApiResponse response, String code, Schema<?> problem) {
        MediaType body = response.getContent() == null || response.getContent().isEmpty()
                ? new MediaType()
                : response.getContent().values().iterator().next();
        if (body.getExample() == null && (body.getExamples() == null || body.getExamples().isEmpty())) {
            body.addExamples(code, new Example().value(json(PROBLEM_EXAMPLES.get(code))));
        }
        response.content(new Content().addMediaType(PROBLEM_JSON, body.schema(problem)));
    }

    private static Object json(String example) {
        try {
            return Json.mapper().readTree(example);
        } catch (JsonProcessingException e) {
            throw new UnexpectedException("Invalid OpenAPI example: " + example, e);
        }
    }
}
