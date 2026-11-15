package academy.incidentradar.cli;

import academy.incidentradar.filter.DateFilter;
import academy.incidentradar.model.LogRecord;
import academy.incidentradar.model.OutputFormat;
import academy.incidentradar.parser.LogParser;
import academy.incidentradar.source.SourceResolver;
import academy.incidentradar.statistics.Statistics;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
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
            description = "Path to write the report.")
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
            CommandSpec s = spec;
            if (s == null) {
                LOG.error("CLI error: {}", e.getMessage());
                return 2;
            }
            throw new ParameterException(s.commandLine(), e.getMessage(), e);
        }
    }

    private void run() throws CliException {
        String pathValue = requireNonNull(path, "--path");
        OutputFormat format = OutputFormat.parse(requireNonNull(outputFormat, "--output-format"));
        LocalDate fromDate = parseDate("--from", from);
        LocalDate toDate = parseDate("--to", to);

        if (fromDate != null && toDate != null && !fromDate.isBefore(toDate)) {
            throw new CliException("--from must be strictly before --to");
        }

        validateOutputPath(format);

        SourceResolver resolver = new SourceResolver();

        List<String> paths = new ArrayList<>();
        paths.add(pathValue);
        paths.addAll(additionalPaths);

        List<String> sources = new ArrayList<>();
        for (String inputPath : paths) {
            sources.addAll(resolver.resolve(inputPath));
        }

        LOG.info("Resolved {} source(s)", sources.size());

        LogParser parser = new LogParser();
        List<LogRecord> records = new ArrayList<>();

        for (String source : sources) {
            try (var reader = resolver.read(source)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    parser.parseLine(line).ifPresent(records::add);
                }
            } catch (IOException e) {
                throw new CliException("Failed to read: " + source, e);
            }
        }

        List<LogRecord> filtered = DateFilter.filter(records, fromDate, toDate);
        Statistics stats = Statistics.calculate(filtered);

        LOG.info("Total requests: {}", stats.totalRequestsCount());
        LOG.info("Response codes: {}", stats.responseCodes());
        LOG.info("Top resources: {}", stats.resources());
        LOG.info(
                "Size avg={}, max={}, p95={}",
                stats.responseSizeInBytes().average(),
                stats.responseSizeInBytes().max(),
                stats.responseSizeInBytes().p95());
    }

    private void validateOutputPath(OutputFormat format) throws CliException {
        Path out = outputPath;
        if (out == null) {
            return;
        }
        if (Files.exists(out)) {
            throw new CliException("Output file already exists: " + out);
        }
        String expected = "." + format.extension();
        String name = out.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.endsWith(expected)) {
            throw new CliException("Output file extension must be " + expected + " for format " + format.name());
        }
    }

    private static @Nullable LocalDate parseDate(String option, @Nullable String value) throws CliException {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new CliException("Invalid " + option + " date: " + value, e);
        }
    }

    private static String requireNonNull(@Nullable String value, String name) throws CliException {
        if (value == null) {
            throw new CliException("Missing required argument: " + name);
        }
        return value;
    }
}
