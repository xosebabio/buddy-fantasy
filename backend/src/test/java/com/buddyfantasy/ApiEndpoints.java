package com.buddyfantasy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Every {@code /api/**} endpoint registered in the app, read from the MVC mappings, so the security scans cover the
 * endpoints added later without listing them. Inject the mappings with
 * {@code @Qualifier("requestMappingHandlerMapping")}.
 */
public final class ApiEndpoints {

    private static final Pattern VARIABLE = Pattern.compile("\\{([^}]+)}");

    private ApiEndpoints() {
    }

    public record Endpoint(HttpMethod method, String pattern) {

        /** The pattern with every path variable replaced by a random id. */
        public String path() {
            return VARIABLE.matcher(pattern).replaceAll(variable -> UUID.randomUUID().toString());
        }

        /** The pattern with each path variable replaced by its value; fails on a variable without one. */
        public String path(Map<String, ?> variables) {
            Matcher matcher = VARIABLE.matcher(pattern);
            StringBuilder path = new StringBuilder();
            while (matcher.find()) {
                Object value = variables.get(matcher.group(1));
                if (value == null) {
                    throw new AssertionError("No value for {" + matcher.group(1) + "} in " + this);
                }
                matcher.appendReplacement(path, value.toString());
            }
            return matcher.appendTail(path).toString();
        }

        @Override
        public String toString() {
            return method + " " + pattern;
        }
    }

    public static List<Endpoint> scan(RequestMappingHandlerMapping mappings) {
        return scan(mappings, handler -> true);
    }

    /** The endpoints whose handler reads a {@code @RequestBody}. */
    public static List<Endpoint> withBody(RequestMappingHandlerMapping mappings) {
        return scan(mappings, handler -> Arrays.stream(handler.getMethodParameters())
                .anyMatch(parameter -> parameter.hasParameterAnnotation(RequestBody.class)));
    }

    private static List<Endpoint> scan(RequestMappingHandlerMapping mappings, Predicate<HandlerMethod> filter) {
        List<Endpoint> endpoints = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> mapping : mappings.getHandlerMethods().entrySet()) {
            if (!filter.test(mapping.getValue())) {
                continue;
            }
            RequestMappingInfo info = mapping.getKey();
            for (String pattern : info.getPathPatternsCondition().getPatternValues()) {
                if (!pattern.startsWith("/api/")) {
                    continue;
                }
                for (var method : info.getMethodsCondition().getMethods()) {
                    endpoints.add(new Endpoint(HttpMethod.valueOf(method.name()), pattern));
                }
            }
        }
        return endpoints;
    }
}
