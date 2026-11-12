package academy.incidentradar.mandatory.mr1;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.support.CliRunner;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("MR1. Валидация аргументов CLI")
class CliValidationTest {

    private static final Path SAMPLES = Path.of("samples", "logs");

    @Test
    @DisplayName("Не передан обязательный --path -> exit code 2")
    void missingPathExitsWithTwo() {
        var result = CliRunner.run("--output-format", "json");
        assertThat(result.exitCode()).as(result.toString()).isEqualTo(2);
        assertThat(result.stderr()).isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = {"xml", "csv", "txt"})
    @DisplayName("Неподдерживаемый --output-format \"{0}\" -> exit code 2")
    void unsupportedOutputFormatExitsWithTwo(String format) {
        var result =
                CliRunner.run("--path", SAMPLES.resolve("access-2025-03-01.log").toString(), "--output-format", format);
        assertThat(result.exitCode()).as(result.toString()).isEqualTo(2);
        assertThat(result.stderr()).isNotBlank();
    }

    @Test
    @DisplayName("--from больше или равен --to -> exit code 2")
    void fromNotBeforeToExitsWithTwo() {
        var result = CliRunner.run(
                "--path", SAMPLES.resolve("access-2025-03-01.log").toString(),
                "--from", "2025-03-02",
                "--to", "2025-03-01");
        assertThat(result.exitCode()).as(result.toString()).isEqualTo(2);
    }

    @Test
    @DisplayName("Несуществующий локальный файл -> exit code 2")
    void missingLocalFileExitsWithTwo() {
        var result = CliRunner.run("--path", "no/such/file.log");
        assertThat(result.exitCode()).as(result.toString()).isEqualTo(2);
        assertThat(result.stderr()).isNotBlank();
    }

    @Test
    @DisplayName("Расширение файла отчёта не соответствует формату -> exit code 2")
    void outputExtensionMismatchExitsWithTwo(@TempDir Path tmp) {
        Path out = tmp.resolve("report.json");
        var result = CliRunner.run(
                "--path", SAMPLES.resolve("access-2025-03-01.log").toString(),
                "--output-format", "markdown",
                "--output-path", out.toString());
        assertThat(result.exitCode()).as(result.toString()).isEqualTo(2);
        assertThat(Files.exists(out)).isFalse();
    }
}
