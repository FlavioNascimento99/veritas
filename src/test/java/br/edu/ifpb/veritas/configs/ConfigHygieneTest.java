package br.edu.ifpb.veritas.configs;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigHygieneTest {

    private String readMainResource(String relativePath) throws Exception {
        Path file = Path.of("src/main/resources", relativePath);
        assertTrue(Files.exists(file), "Arquivo não encontrado: " + file.toAbsolutePath());
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    @Test
    void basePropertiesNaoContemCredenciaisNemDdlAuto() throws Exception {
        String base = readMainResource("application.properties");

        assertFalse(base.contains("spring.datasource.url"));
        assertFalse(base.contains("spring.datasource.username"));
        assertFalse(base.contains("spring.datasource.password"));
        assertFalse(base.contains("ddl-auto"));
        assertFalse(base.contains("jdbc:postgresql"));
        assertFalse(base.contains("localhost:5432"));
    }

    @Test
    void prodProfileExigeConfiguracaoPorVariaveisDeAmbiente() throws Exception {
        String prod = readMainResource("application-prod.yml");

        assertTrue(prod.contains("${PGHOST}"));
        assertTrue(prod.contains("${PGUSER}"));
        assertTrue(prod.contains("${PGPASSWORD}"));
        assertFalse(prod.contains("password: admin"));
        assertFalse(prod.contains("localhost"));
    }

    @Test
    void localProfileSegueEstiloDeVariaveisDeAmbiente() throws Exception {
        String local = readMainResource("application-local.properties");

        assertTrue(local.contains("${PGHOST:localhost}"));
        assertTrue(local.contains("${PGUSER:postgres}"));
        assertTrue(local.contains("${PGPASSWORD:admin}"));
    }
}