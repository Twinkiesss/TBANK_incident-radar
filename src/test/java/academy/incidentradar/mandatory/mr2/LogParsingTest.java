package academy.incidentradar.mandatory.mr2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import academy.incidentradar.model.LogRecord;
import academy.incidentradar.parser.LogParser;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MR2. Парсер строки лога")
class LogParsingTest {

    private static final String VALID_LINE = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] "
            + "\"GET /downloads/product_1 HTTP/1.1\" "
            + "304 0 \"-\" \"Debian APT-HTTP/1.3\"";

    private final LogParser parser = new LogParser();

    @Test
    @DisplayName("Эталонная строка разбирается на корректные поля")
    void referenceLineIsParsedCorrectly() {
        Optional<LogRecord> result = parser.parseLine(VALID_LINE);

        assertTrue(result.isPresent());

        LogRecord record = result.get();
        assertEquals("93.180.71.3", record.remoteAddress());
        assertEquals(304, record.status());
        assertEquals(0, record.bodyBytesSent());
        assertEquals("/downloads/product_1", record.resource());
    }

    @Test
    @DisplayName("Метод, ресурс и протокол разбираются в три отдельных поля")
    void requestIsSplitIntoThreeFields() {
        LogRecord record = parser.parseLine(VALID_LINE).orElseThrow();

        assertEquals("GET", record.method());
        assertEquals("/downloads/product_1", record.resource());
        assertEquals("HTTP/1.1", record.protocol());
    }

    @Test
    @DisplayName("Битая строка пропускается, программа не падает")
    void malformedLineIsSkippedWithWarning() {
        assertDoesNotThrow(() -> {
            Optional<LogRecord> result = parser.parseLine("this is not a valid log line");

            assertTrue(result.isEmpty());
        });
    }
}
