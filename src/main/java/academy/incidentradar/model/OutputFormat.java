package academy.incidentradar.model;

import academy.incidentradar.cli.CliException;
import java.util.Locale;

public enum OutputFormat {
    JSON("json"),
    MARKDOWN("md");

    private final String extension;

    OutputFormat(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    public static OutputFormat parse(String value) throws CliException {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
        return switch (normalized) {
            case "json" -> JSON;
            case "markdown", "md" -> MARKDOWN;
            default -> throw new CliException("Unsupported output format: " + value);
        };
    }
}
