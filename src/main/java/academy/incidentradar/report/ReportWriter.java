package academy.incidentradar.report;

import academy.incidentradar.cli.CliException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class ReportWriter {

    public void write(Path outputPath, String report) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                outputPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            writer.write(report);
        } catch (FileAlreadyExistsException e) {
            throw new CliException("Output file already exists: " + outputPath, e);
        } catch (IOException e) {
            throw new CliException("Cannot write report to: " + outputPath, e);
        }
    }
}
