package academy.incidentradar.report;

import academy.incidentradar.model.OutputFormat;
import academy.incidentradar.statistics.ResourceCount;
import academy.incidentradar.statistics.Statistics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReportGenerator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String generate(Statistics statistics, List<String> files, OutputFormat format) {
        return switch (format) {
            case JSON -> generateJson(statistics, files);
            case MARKDOWN -> generateMarkdown(statistics, files);
        };
    }

    private String generateJson(Statistics statistics, List<String> files) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("files", files);
        report.put("totalRequestsCount", statistics.totalRequestsCount());
        report.put("responseSizeInBytes", statistics.responseSizeInBytes());
        report.put("resources", statistics.resources());
        report.put("responseCodes", statistics.responseCodes());

        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(report) + System.lineSeparator();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize JSON report", e);
        }
    }

    private String generateMarkdown(Statistics statistics, List<String> files) {
        StringBuilder report = new StringBuilder();

        report.append("# Incident Radar report\n\n");
        report.append("## Files\n\n");
        for (String file : files) {
            report.append("- `").append(markdownCell(file)).append("`\n");
        }

        report.append("\n## Summary\n\n");
        report.append("| Metric | Value |\n");
        report.append("|---|---:|\n");
        report.append("| Total requests | ")
                .append(statistics.totalRequestsCount())
                .append(" |\n");

        var sizes = statistics.responseSizeInBytes();
        report.append("| Average response size (bytes) | ")
                .append(sizes.average())
                .append(" |\n");
        report.append("| Maximum response size (bytes) | ").append(sizes.max()).append(" |\n");
        report.append("| p95 response size (bytes) | ").append(sizes.p95()).append(" |\n");

        report.append("\n## Response codes\n\n");
        report.append("| Status code | Requests |\n");
        report.append("|---:|---:|\n");
        statistics
                .responseCodes()
                .forEach((code, count) -> report.append("| ")
                        .append(code)
                        .append(" | ")
                        .append(count)
                        .append(" |\n"));

        report.append("\n## Top resources\n\n");
        report.append("| Resource | Requests |\n");
        report.append("|---|---:|\n");
        for (ResourceCount resource : statistics.resources()) {
            report.append("| ")
                    .append(markdownCell(resource.resource()))
                    .append(" | ")
                    .append(resource.count())
                    .append(" |\n");
        }

        return report.toString();
    }

    private static String markdownCell(String value) {
        return value.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
    }
}
