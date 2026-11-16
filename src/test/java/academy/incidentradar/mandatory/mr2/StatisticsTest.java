package academy.incidentradar.mandatory.mr2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import academy.incidentradar.model.LogRecord;
import academy.incidentradar.statistics.Statistics;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MR2. Статистика")
class StatisticsTest {

    private LogRecord record(String resource, int status, long bytes) {
        return new LogRecord(
                "127.0.0.1",
                "-",
                OffsetDateTime.parse("2025-03-01T10:00:00Z"),
                "GET",
                resource,
                "HTTP/1.1",
                status,
                bytes,
                "-",
                "Test");
    }

    @Test
    @DisplayName("Распределение кодов ответов подсчитывается правильно")
    void responseCodesAreCounted() {
        List<LogRecord> records = List.of(
                record("/a", 200, 100),
                record("/b", 200, 100),
                record("/c", 404, 100),
                record("/d", 500, 100),
                record("/e", 500, 100),
                record("/f", 500, 100));

        var statistics = Statistics.calculate(records.stream());

        assertEquals(2L, statistics.responseCodes().get(200));
        assertEquals(1L, statistics.responseCodes().get(404));
        assertEquals(3L, statistics.responseCodes().get(500));
    }

    @Test
    @DisplayName("Top-10 ресурсов отсортирован по убыванию количества")
    void topResourcesAreSortedDescending() {
        List<LogRecord> records = List.of(
                record("/a", 200, 100),
                record("/a", 200, 100),
                record("/a", 200, 100),
                record("/b", 200, 100),
                record("/b", 200, 100),
                record("/c", 200, 100));

        var resources = Statistics.calculate(records.stream()).resources();

        assertEquals("/a", resources.get(0).resource());
        assertEquals(3L, resources.get(0).count());
        assertEquals("/b", resources.get(1).resource());
        assertEquals("/c", resources.get(2).resource());
    }

    @Test
    @DisplayName("Размеры 100..500: average 300, max 500, p95 500")
    void responseSizeStatistics() {
        List<LogRecord> records = List.of(
                record("/a", 200, 100),
                record("/b", 200, 200),
                record("/c", 200, 300),
                record("/d", 200, 400),
                record("/e", 200, 500));

        var size = Statistics.calculate(records.stream()).responseSizeInBytes();

        assertEquals(300.0, size.average());
        assertEquals(500L, size.max());
        assertEquals(500.0, size.p95());
    }

    @Test
    @DisplayName("Дробные значения округляются до 2 знаков")
    void fractionalValuesAreRoundedToTwoDecimals() {
        List<LogRecord> records = List.of(record("/a", 200, 100), record("/b", 200, 101), record("/c", 200, 102));

        var size = Statistics.calculate(records.stream()).responseSizeInBytes();

        assertEquals(101.0, size.average());
        assertEquals(102.0, size.p95());
    }

    @Test
    @DisplayName("Метод расчёта p95 — Nearest Rank")
    void p95MethodIsDocumentedAndStable() {
        List<LogRecord> records = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            records.add(record("/test", 200, i * 100));
        }

        var size = Statistics.calculate(records.stream()).responseSizeInBytes();

        // Nearest Rank: ceil(0.95 * 20) = 19.
        assertEquals(1900.0, size.p95());
    }
}
