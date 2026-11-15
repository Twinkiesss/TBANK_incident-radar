package academy.incidentradar.statistics;

import academy.incidentradar.model.LogRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Statistics {

    private static final int TOP_RESOURCES_LIMIT = 10;
    private static final double P95_QUANTILE = 0.95;
    private static final int ROUNDING_SCALE = 2;
    private static final int FIRST_INDEX = 0;

    private final long totalRequestsCount;
    private final Map<Integer, Long> responseCodes;
    private final List<ResourceCount> resources;
    private final ResponseSizeStatistics responseSizeInBytes;

    private Statistics(
            long totalRequestsCount,
            Map<Integer, Long> responseCodes,
            List<ResourceCount> resources,
            ResponseSizeStatistics responseSizeInBytes) {
        this.totalRequestsCount = totalRequestsCount;
        this.responseCodes = responseCodes;
        this.resources = resources;
        this.responseSizeInBytes = responseSizeInBytes;
    }

    public static Statistics calculate(List<LogRecord> records) {
        Map<Integer, Long> codes =
                records.stream().collect(Collectors.groupingBy(LogRecord::status, Collectors.counting()));

        Map<Integer, Long> sortedCodes = new LinkedHashMap<>();

        codes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sortedCodes.put(entry.getKey(), entry.getValue()));

        List<ResourceCount> topResources =
                records.stream()
                        .collect(Collectors.groupingBy(LogRecord::resource, Collectors.counting()))
                        .entrySet()
                        .stream()
                        .map(entry -> new ResourceCount(entry.getKey(), entry.getValue()))
                        .sorted(Comparator.comparingLong(ResourceCount::count)
                                .reversed()
                                .thenComparing(ResourceCount::resource))
                        .limit(TOP_RESOURCES_LIMIT)
                        .toList();

        List<Long> sizes =
                records.stream().map(LogRecord::bodyBytesSent).sorted().toList();

        ResponseSizeStatistics sizeStatistics;

        if (sizes.isEmpty()) {
            sizeStatistics = new ResponseSizeStatistics(0.0, 0, 0.0);
        } else {
            double average = sizes.stream().mapToLong(Long::longValue).average().orElse(0.0);

            long max = sizes.get(sizes.size() - 1);

            int p95Index = (int) Math.ceil(P95_QUANTILE * sizes.size()) - 1;

            double p95 = sizes.get(Math.max(FIRST_INDEX, p95Index));

            sizeStatistics = new ResponseSizeStatistics(round(average), max, round(p95));
        }

        return new Statistics(records.size(), sortedCodes, topResources, sizeStatistics);
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(ROUNDING_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public long totalRequestsCount() {
        return totalRequestsCount;
    }

    public Map<Integer, Long> responseCodes() {
        return Map.copyOf(responseCodes);
    }

    public List<ResourceCount> resources() {
        return List.copyOf(resources);
    }

    public ResponseSizeStatistics responseSizeInBytes() {
        return responseSizeInBytes;
    }
}
