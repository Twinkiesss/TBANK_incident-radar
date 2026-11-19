package academy.incidentradar.cli;

import academy.incidentradar.filter.DateFilter;
import academy.incidentradar.model.OutputFormat;
import academy.incidentradar.parser.LogParser;
import academy.incidentradar.report.ReportGenerator;
import academy.incidentradar.report.ReportWriter;
import academy.incidentradar.source.SourceReader;
import academy.incidentradar.source.SourceResolver;
import academy.incidentradar.statistics.Statistics;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(
        name = "incident-radar",
        mixinStandardHelpOptions = true,
        description = "Analyzes NGINX logs and builds a statistics report.")
public final class IncidentRadarCommand implements Callable<Integer> {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentRadarCommand.class);

    @Spec
    private @Nullable CommandSpec spec;

    @Option(
            names = {"--path", "-p"},
            required = true,
            description = "File, glob pattern or URL.")
    private @Nullable String path;

    @Parameters(arity = "0..*", paramLabel = "PATH")
    private List<String> additionalPaths = new ArrayList<>();

    @Option(
            names = {"--output-format", "-f"},
            defaultValue = "markdown",
            description = "Report format: json or markdown.")
    private @Nullable String outputFormat;

    @Option(
            names = {"--output-path", "-o"},
            description = "Path to write the report. If omitted, report is printed to stdout.")
    private @Nullable Path outputPath;

    @Option(names = "--from", description = "Start date inclusive (yyyy-MM-dd).")
    private @Nullable String from;

    @Option(names = "--to", description = "End date inclusive (yyyy-MM-dd).")
    private @Nullable String to;

    @Override
    public Integer call() {
        try {
            run();
            return 0;
        } catch (CliException e) {
            if (spec == null) {
                LOG.error("CLI error: {}", e.getMessage());
                return 2;
            }
            throw new ParameterException(spec.commandLine(), e.getMessage(), e);
        }
    }

    private void run() {
        String pathValue = requireNonNull(path, "--path");
        OutputFormat format = OutputFormat.parse(requireNonNull(outputFormat, "--output-format"));

        LocalDate fromDate = parseDate("--from", from);
        LocalDate toDate = parseDate("--to", to);

        if (fromDate != null && toDate != null && !fromDate.isBefore(toDate)) {
            throw new CliException("--from must be strictly before --to");
        }

        validateOutputPath(format);

        SourceResolver resolver = new SourceResolver();
        SourceReader reader = new SourceReader();

        List<String> paths = new ArrayList<>();
        paths.add(pathValue);
        paths.addAll(additionalPaths);

        List<String> sources = new ArrayList<>();
        for (String inputPath : paths) {
            sources.addAll(resolver.resolve(inputPath));
        }

        LOG.info("Resolved {} source(s)", sources.size());

        LogParser parser = new LogParser();
        Stream<String> linesStream = sources.stream().flatMap(reader::read);

        try (linesStream) {
            Statistics stats = Statistics.calculate(linesStream
                    .map(parser::parseLine)
                    .flatMap(Optional::stream)
                    .filter(DateFilter.inRange(fromDate, toDate)));

            String report = new ReportGenerator().generate(stats, sources, format);

            if (outputPath == null) {
                System.out.print(report);
            } else {
                new ReportWriter().write(outputPath, report);
                LOG.info("Report saved to {}", outputPath);
            }

            LOG.info("Total requests: {}", stats.totalRequestsCount());
        }
    }

    private void validateOutputPath(OutputFormat format) {
        Path out = outputPath;
        if (out == null) {
            return;
        }

        if (Files.exists(out)) {
            throw new CliException("Output file already exists: " + out);
        }

        String expected = "." + format.extension();
        Path fileName = out.getFileName();

        if (fileName == null) {
            throw new CliException("Invalid output file path: " + out);
        }

        String name = fileName.toString().toLowerCase(Locale.ROOT);
        if (!name.endsWith(expected)) {
            throw new CliException("Output file extension must be " + expected + " for format " + format.name());
        }
    }

    private static @Nullable LocalDate parseDate(String option, @Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new CliException("Invalid " + option + " date: " + value, e);
        }
    }

    private static String requireNonNull(@Nullable String value, String name) {
        if (value == null) {
            throw new CliException("Missing required argument: " + name);
        }

        return value;
    }
}
