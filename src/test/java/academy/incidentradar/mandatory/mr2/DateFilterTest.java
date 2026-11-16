package academy.incidentradar.mandatory.mr2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import academy.incidentradar.filter.DateFilter;
import academy.incidentradar.model.LogRecord;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MR2. Фильтр по датам")
class DateFilterTest {

    private LogRecord record(String date) {
        return new LogRecord(
                "127.0.0.1", "-", OffsetDateTime.parse(date), "GET", "/test", "HTTP/1.1", 200, 100, "-", "Test");
    }

    private List<LogRecord> records() {
        return List.of(
                record("2025-03-01T10:00:00Z"),
                record("2025-03-02T10:00:00Z"),
                record("2025-03-03T10:00:00Z"),
                record("2025-03-04T10:00:00Z"));
    }

    @Test
    @DisplayName("Без --from и --to обрабатываются все записи")
    void noFilterKeepsEverything() {
        var predicate = DateFilter.inRange(null, null);
        assertEquals(4, records().stream().filter(predicate).count());
    }

    @Test
    @DisplayName("Только --from: открытая правая граница")
    void onlyFromIsOpenEnded() {
        var predicate = DateFilter.inRange(LocalDate.parse("2025-03-02"), null);
        assertEquals(3, records().stream().filter(predicate).count());
    }

    @Test
    @DisplayName("Только --to: открытая левая граница")
    void onlyToIsOpenEnded() {
        var predicate = DateFilter.inRange(null, LocalDate.parse("2025-03-02"));
        assertEquals(2, records().stream().filter(predicate).count());
    }

    @Test
    @DisplayName("Обе границы: период включительно")
    void bothBoundsAreInclusive() {
        var predicate = DateFilter.inRange(LocalDate.parse("2025-03-02"), LocalDate.parse("2025-03-04"));
        assertEquals(3, records().stream().filter(predicate).count());
    }
}
