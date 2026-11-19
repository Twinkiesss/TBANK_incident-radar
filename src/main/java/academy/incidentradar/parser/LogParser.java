package academy.incidentradar.parser;

import academy.incidentradar.model.LogRecord;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParser {

    private static final int REMOTE_ADDRESS_GROUP = 1;
    private static final int REMOTE_USER_GROUP = 3;
    private static final int TIMESTAMP_GROUP = 4;
    private static final int REQUEST_GROUP = 5;
    private static final int STATUS_GROUP = 6;
    private static final int BYTES_GROUP = 7;
    private static final int REFERER_GROUP = 8;
    private static final int USER_AGENT_GROUP = 9;

    private static final int REQUEST_PARTS_COUNT = 3;

    private static final String MISSING_VALUE = "-";

    private static final Pattern LOG_PATTERN = Pattern.compile("^(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+\\[([^]]+)]\\s+"
            + "\"([^\"]*)\"\\s+(\\d{3})\\s+(\\S+)\\s+"
            + "\"([^\"]*)\"\\s+\"([^\"]*)\"$");

    private static final DateTimeFormatter DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd/MMM/yyyy:HH:mm:ss xx")
            .toFormatter(Locale.ENGLISH);

    public Optional<LogRecord> parseLine(String line) {
        Matcher matcher = LOG_PATTERN.matcher(line);

        if (!matcher.matches()) {
            return malformed();
        }

        try {
            String request = matcher.group(REQUEST_GROUP);
            String[] requestParts = request.trim().split("\\s+");

            if (requestParts.length != REQUEST_PARTS_COUNT) {
                return malformed();
            }

            OffsetDateTime timestamp = OffsetDateTime.parse(matcher.group(TIMESTAMP_GROUP), DATE_FORMATTER);

            int status = Integer.parseInt(matcher.group(STATUS_GROUP));

            String bytesText = matcher.group(BYTES_GROUP);
            long bytes = MISSING_VALUE.equals(bytesText) ? 0 : Long.parseLong(bytesText);

            if (bytes < 0) {
                return malformed();
            }

            return Optional.of(new LogRecord(
                    matcher.group(REMOTE_ADDRESS_GROUP),
                    matcher.group(REMOTE_USER_GROUP),
                    timestamp,
                    requestParts[0],
                    requestParts[1],
                    requestParts[2],
                    status,
                    bytes,
                    matcher.group(REFERER_GROUP),
                    matcher.group(USER_AGENT_GROUP)));

        } catch (DateTimeParseException | NumberFormatException exception) {
            return malformed();
        }
    }

    private Optional<LogRecord> malformed() {
        System.err.println("WARN: skipped malformed log line");
        return Optional.empty();
    }
}
