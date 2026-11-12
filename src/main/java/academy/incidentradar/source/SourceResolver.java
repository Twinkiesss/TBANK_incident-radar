package academy.incidentradar.source;

import academy.incidentradar.cli.CliException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class SourceResolver {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".log", ".txt");
    private static final int HTTP_OK = 200;

    public List<String> resolve(String pathSpec) throws CliException {
        if (isUrl(pathSpec)) {
            return List.of(pathSpec);
        }
        if (isGlob(pathSpec)) {
            return expandGlob(pathSpec);
        }
        Path p = Path.of(pathSpec);
        if (!Files.exists(p)) {
            throw new CliException("File not found: " + pathSpec);
        }
        if (!Files.isRegularFile(p)) {
            throw new CliException("Not a regular file: " + pathSpec);
        }
        requireSupportedExtension(pathSpec);
        return List.of(pathSpec);
    }

    public BufferedReader read(String source) throws CliException {
        if (isUrl(source)) {
            return readUrl(source);
        }
        try {
            return Files.newBufferedReader(Path.of(source), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CliException("Cannot read file: " + source, e);
        }
    }

    private BufferedReader readUrl(String url) throws CliException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request =
                HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != HTTP_OK) {
                response.body().close();
                throw new CliException("URL returned HTTP " + response.statusCode() + ": " + url);
            }
            return new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new CliException("Cannot read URL: " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CliException("Interrupted while reading URL: " + url, e);
        }
    }

    private List<String> expandGlob(String pattern) throws CliException {
        Path base = globBase(pattern);
        if (!Files.isDirectory(base)) {
            throw new CliException("Glob base directory not found: " + base);
        }
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        List<String> found = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(base, 1)) {
            stream.filter(Files::isRegularFile)
                    .filter(matcher::matches)
                    .filter(p -> hasSupportedExtension(p.toString()))
                    .sorted()
                    .forEach(p -> found.add(p.toString()));
        } catch (IOException e) {
            throw new CliException("Failed to expand glob: " + pattern, e);
        }
        if (found.isEmpty()) {
            throw new CliException("No files match glob: " + pattern);
        }
        return found;
    }

    private static Path globBase(String pattern) {
        int wildcard = firstWildcard(pattern);
        String prefix = pattern.substring(0, wildcard);
        int slash = prefix.lastIndexOf('/');
        if (slash < 0) {
            return Path.of(".");
        }
        String dir = prefix.substring(0, slash);
        return dir.isEmpty() ? Path.of("/") : Path.of(dir);
    }

    private static int firstWildcard(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '*' || c == '?') {
                return i;
            }
        }
        return s.length();
    }

    private void requireSupportedExtension(String path) throws CliException {
        if (!hasSupportedExtension(path)) {
            throw new CliException("Unsupported file extension: " + path);
        }
    }

    private static boolean hasSupportedExtension(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private static boolean isUrl(String s) {
        return s.startsWith("http://") || s.startsWith("https://");
    }

    private static boolean isGlob(String s) {
        return s.contains("*") || s.contains("?");
    }
}
