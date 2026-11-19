package academy.incidentradar.mandatory.mr3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import academy.incidentradar.support.CliRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Обязательные тесты: отчёты и коды возврата — blackbox-проверки CLI. */
@DisplayName("MR3. Отчёты и коды возврата")
class ReportAndExitCodeTest {

    private static final Path SAMPLE_LOG = Path.of("samples", "logs", "access-2025-03-01.log");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @DisplayName("JSON-отчёт валиден и содержит все обязательные поля")
    void jsonReportIsValidAndComplete(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("report.json");

        var result = CliRunner.run(
                "--path", SAMPLE_LOG.toString(),
                "--output-format", "json",
                "--output-path", output.toString());

        assertEquals(0, result.exitCode(), result.toString());
        assertTrue(Files.exists(output));

        JsonNode report = OBJECT_MAPPER.readTree(Files.readString(output));
        assertTrue(report.get("files").isArray());
        assertTrue(report.has("totalRequestsCount"));
        assertTrue(report.get("responseSizeInBytes").has("average"));
        assertTrue(report.get("responseSizeInBytes").has("max"));
        assertTrue(report.get("responseSizeInBytes").has("p95"));
        assertTrue(report.get("resources").isArray());
        assertTrue(report.get("responseCodes").isObject());
    }

    @Test
    @DisplayName("Markdown-отчёт содержит заголовок и таблицы")
    void markdownReportHasHeaderAndTables(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("report.md");

        var result = CliRunner.run(
                "--path", SAMPLE_LOG.toString(),
                "--output-format", "markdown",
                "--output-path", output.toString());

        assertEquals(0, result.exitCode(), result.toString());

        String report = Files.readString(output);
        assertTrue(report.contains("# Incident Radar report"));
        assertTrue(report.contains("## Response codes"));
        assertTrue(report.contains("## Top resources"));
        assertTrue(report.contains("|---"));
    }

    @Test
    @DisplayName("Существующий файл отчёта не перезаписывается -> ошибка")
    void existingOutputFileIsNotOverwritten(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("report.json");
        String originalContent = "do not overwrite";
        Files.writeString(output, originalContent);

        var result = CliRunner.run(
                "--path", SAMPLE_LOG.toString(),
                "--output-format", "json",
                "--output-path", output.toString());

        assertEquals(2, result.exitCode(), result.toString());
        assertEquals(originalContent, Files.readString(output));
    }

    @Test
    @DisplayName("Успешный запуск -> exit code 0")
    void successfulRunExitsWithZero() {
        var result = CliRunner.run("--path", SAMPLE_LOG.toString());

        assertEquals(0, result.exitCode(), result.toString());
        assertTrue(result.stdout().contains("# Incident Radar report"));
    }

    @Test
    @DisplayName("Некорректное использование -> exit code 2, а не 1")
    void misuseExitsWithTwoNotOne() {
        var result = CliRunner.run("--path", SAMPLE_LOG.toString(), "--output-format", "xml");

        assertEquals(2, result.exitCode(), result.toString());
    }
}
