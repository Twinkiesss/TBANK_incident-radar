package academy.incidentradar.filter;

import academy.incidentradar.model.LogRecord;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class DateFilter {

    private DateFilter() {}

    public static List<LogRecord> filter(
            Collection<LogRecord> records, @Nullable LocalDate from, @Nullable LocalDate to) {
        if (from != null && to != null && !from.isBefore(to)) {
            throw new IllegalArgumentException("--from must be earlier than --to");
        }

        return records.stream()
                .filter(record -> {
                    LocalDate date = record.timestamp().toLocalDate();

                    boolean afterFrom = from == null || !date.isBefore(from);

                    boolean beforeTo = to == null || !date.isAfter(to);

                    return afterFrom && beforeTo;
                })
                .toList();
    }
}
