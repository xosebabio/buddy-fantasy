package com.buddyfantasy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import lombok.experimental.UtilityClass;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * Binds the real {@code application.yml} without a Spring context, so unit tests use the configured defaults and
 * limits instead of a copy of them.
 */
@UtilityClass
public class TestApplicationYaml {

    /** Binds {@code prefix} with {@code overrides} (full property names) taking precedence over the file. */
    public <T> T bind(String prefix, Class<T> type, Map<String, Object> overrides) {
        try {
            List<PropertySource<?>> yaml = new YamlPropertySourceLoader()
                    .load("application.yml", new ClassPathResource("application.yml"));
            Binder binder = new Binder(List.of(new MapConfigurationPropertySource(overrides),
                    ConfigurationPropertySources.from(yaml).iterator().next()));
            return binder.bind(prefix, type).get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
