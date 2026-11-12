package academy.incidentradar;

import academy.incidentradar.cli.IncidentRadarCommand;
import picocli.CommandLine;

public final class Main {

    private Main() {}

    public static void main(String[] args) {
        int exitCode = new CommandLine(new IncidentRadarCommand()).execute(args);
        System.exit(exitCode);
    }
}
