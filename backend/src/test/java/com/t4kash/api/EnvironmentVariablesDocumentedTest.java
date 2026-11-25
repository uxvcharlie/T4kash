package com.t4kash.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evita que .env.example se vuelva a quedar atras: cada variable que lee la
 * configuracion debe estar documentada ahi, aunque sea comentada.
 */
class EnvironmentVariablesDocumentedTest {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Z][A-Z0-9_]+)");
    private static final Pattern ENV_ENTRY = Pattern.compile("(?m)^#?\\s*([A-Z][A-Z0-9_]+)=");

    @Test
    void everyVariableReadByTheConfigurationIsInEnvExample() throws IOException {
        Set<String> used = new TreeSet<>();
        try (Stream<Path> files = Files.list(Path.of("src/main/resources"))) {
            for (Path file : files.filter(f -> f.getFileName().toString().startsWith("application")).toList()) {
                Matcher matcher = PLACEHOLDER.matcher(Files.readString(file));
                while (matcher.find()) {
                    used.add(matcher.group(1));
                }
            }
        }
        Set<String> documented = new TreeSet<>();
        Matcher matcher = ENV_ENTRY.matcher(Files.readString(Path.of(".env.example")));
        while (matcher.find()) {
            documented.add(matcher.group(1));
        }

        assertThat(used).isNotEmpty();
        assertThat(documented)
                .as("Variables que faltan en backend/.env.example")
                .containsAll(used);
    }
}
