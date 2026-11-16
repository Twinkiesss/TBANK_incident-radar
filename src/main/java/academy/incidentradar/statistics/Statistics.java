package academy.incidentradar.statistics;

import academy.incidentradar.model.LogRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Stream;

public final class Statistics {

    private static final int TOP_RESOURCES_LIMIT = 10;
    private static final double P95_QUANTILE = 0.95;
    private static final int ROUNDING_SCALE = 2;

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

    public static Statistics calculate(Stream<LogRecord> records) {
        return records.collect(
                Collector.of(Accumulator::new, Accumulator::add, Accumulator::combine, Accumulator::finish));
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
        return responseCodes;
    }

    public List<ResourceCount> resources() {
        return resources;
    }

    public ResponseSizeStatistics responseSizeInBytes() {
        return responseSizeInBytes;
    }

    private static final class Accumulator {

        private long total;
        private final Map<Integer, Long> codes = new TreeMap<>();
        private final Map<String, Long> resourceCounts = new LinkedHashMap<>();
        private final List<Long> sizes = new ArrayList<>();

        private void add(LogRecord record) {
            total++;
            codes.merge(record.status(), 1L, Long::sum);
            resourceCounts.merge(record.resource(), 1L, Long::sum);
            sizes.add(record.bodyBytesSent());
        }

        private Accumulator combine(Accumulator other) {
            total += other.total;

            other.codes.forEach((code, count) -> codes.merge(code, count, Long::sum));

            other.resourceCounts.forEach((resource, count) -> resourceCounts.merge(resource, count, Long::sum));

            sizes.addAll(other.sizes);
            return this;
        }

        private Statistics finish() {
            List<ResourceCount> topResources = resourceCounts.entrySet().stream()
                    .map(entry -> new ResourceCount(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparingLong(ResourceCount::count)
                            .reversed()
                            .thenComparing(ResourceCount::resource))
                    .limit(TOP_RESOURCES_LIMIT)
                    .toList();

            Map<Integer, Long> sortedCodes = new LinkedHashMap<>(codes);

            ResponseSizeStatistics sizeStatistics;

            if (sizes.isEmpty()) {
                sizeStatistics = new ResponseSizeStatistics(0.0, 0, 0.0);
            } else {
                sizes.sort(Long::compareTo);

                double sum = 0.0;
                for (long size : sizes) {
                    sum += size;
                }

                double average = sum / sizes.size();
                long max = sizes.get(sizes.size() - 1);

                int p95Index = (int) Math.ceil(P95_QUANTILE * sizes.size()) - 1;
                double p95 = sizes.get(p95Index);

                sizeStatistics = new ResponseSizeStatistics(round(average), max, round(p95));
            }

            return new Statistics(total, sortedCodes, topResources, sizeStatistics);
        }
    }
}
