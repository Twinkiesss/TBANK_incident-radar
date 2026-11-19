package academy.incidentradar.source;

import academy.incidentradar.cli.CliException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class SourceReader {

    private static final int HTTP_OK = 200;

    /** Поток владеет открытым ресурсом. Вызывающий код обязан закрыть возвращённый поток. */
    @SuppressWarnings("StreamResourceLeak")
    public Stream<String> read(String source) {
        if (isUrl(source)) {
            return readUrl(source);
        }

        try {
            return Files.lines(Path.of(source), StandardCharsets.UTF_8);
        } catch (IOException | InvalidPathException e) {
            throw new CliException("Cannot read file: " + source, e);
        }
    }

    /** Поток тела HTTP-ответа передаётся вызывающему коду. Вызывающий код обязан закрыть возвращённый поток. */
    @SuppressWarnings("StreamResourceLeak")
    private Stream<String> readUrl(String url) {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        } catch (IllegalArgumentException e) {
            throw new CliException("Invalid URL: " + url, e);
        }

        try {
            HttpResponse<Stream<String>> response = client.send(request, HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() != HTTP_OK) {
                response.body().close();
                throw new CliException("URL returned HTTP " + response.statusCode() + ": " + url);
            }

            return response.body();
        } catch (IOException e) {
            throw new CliException("Cannot read URL: " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CliException("Interrupted while reading URL: " + url, e);
        }
    }

    private static boolean isUrl(String source) {
        return source.startsWith("http://") || source.startsWith("https://");
    }
}
