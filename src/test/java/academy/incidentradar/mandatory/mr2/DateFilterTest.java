package academy.incidentradar.mandatory.mr2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import academy.incidentradar.filter.DateFilter;
import academy.incidentradar.model.LogRecord;
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
        assertEquals(4, DateFilter.filter(records(), null, null).size());
    }

    @Test
    @DisplayName("Только --from: открытая правая граница")
    void onlyFromIsOpenEnded() {
        assertEquals(
                3,
                DateFilter.filter(records(), java.time.LocalDate.parse("2025-03-02"), null)
                        .size());
    }

    @Test
    @DisplayName("Только --to: открытая левая граница")
    void onlyToIsOpenEnded() {
        assertEquals(
                2,
                DateFilter.filter(records(), null, java.time.LocalDate.parse("2025-03-02"))
                        .size());
    }

    @Test
    @DisplayName("Обе границы: период включительно")
    void bothBoundsAreInclusive() {
        assertEquals(
                3,
                DateFilter.filter(
                                records(),
                                java.time.LocalDate.parse("2025-03-02"),
                                java.time.LocalDate.parse("2025-03-04"))
                        .size());
    }
}
