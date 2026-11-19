package academy.incidentradar.source;

import academy.incidentradar.cli.CliException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Locale;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

public final class SourceResolver {

    private static final List<String> SUPPORTED_EXTENSIONS = List.of(".log", ".txt");

    public List<String> resolve(String pathSpec) {
        if (isUrl(pathSpec)) {
            validateUrl(pathSpec);
            return List.of(pathSpec);
        }

        if (isGlob(pathSpec)) {
            return expandGlob(pathSpec);
        }

        Path path;
        try {
            path = Path.of(pathSpec);
        } catch (InvalidPathException e) {
            throw new CliException("Invalid file path: " + pathSpec, e);
        }

        if (!Files.exists(path)) {
            throw new CliException("File not found: " + pathSpec);
        }

        if (!Files.isRegularFile(path)) {
            throw new CliException("Not a regular file: " + pathSpec);
        }

        requireSupportedExtension(pathSpec);
        return List.of(pathSpec);
    }

    private List<String> expandGlob(String pattern) {
        Path base;
        PathMatcher matcher;

        try {
            base = globBase(pattern);
            matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        } catch (InvalidPathException | PatternSyntaxException e) {
            throw new CliException("Invalid glob pattern: " + pattern, e);
        }

        if (!Files.isDirectory(base)) {
            throw new CliException("Glob base directory not found: " + base);
        }

        List<String> found;

        try (Stream<Path> stream = Files.walk(base, 1)) {
            found = stream.filter(Files::isRegularFile)
                    .filter(matcher::matches)
                    .filter(path -> hasSupportedExtension(path.toString()))
                    .sorted()
                    .map(Path::toString)
                    .toList();
        } catch (IOException e) {
            throw new CliException("Failed to expand glob: " + pattern, e);
        }

        if (found.isEmpty()) {
            throw new CliException("No files match glob: " + pattern);
        }

        return found;
    }

    private static void validateUrl(String url) {
        URI uri;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new CliException("Invalid URL: " + url, e);
        }

        String scheme = uri.getScheme();

        if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || uri.getHost() == null) {
            throw new CliException("Invalid HTTP URL: " + url);
        }

        String urlPath = uri.getPath();

        if (urlPath == null || !hasSupportedExtension(urlPath)) {
            throw new CliException("URL must point to a .log or .txt file: " + url);
        }
    }

    private static Path globBase(String pattern) {
        int wildcard = firstWildcard(pattern);
        String prefix = pattern.substring(0, wildcard);
        int slash = prefix.lastIndexOf('/');

        if (slash < 0) {
            return Path.of(".");
        }

        String directory = prefix.substring(0, slash);
        return directory.isEmpty() ? Path.of("/") : Path.of(directory);
    }

    private static int firstWildcard(String value) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);

            if (character == '*' || character == '?') {
                return i;
            }
        }

        return value.length();
    }

    private static void requireSupportedExtension(String path) {
        if (!hasSupportedExtension(path)) {
            throw new CliException("Unsupported file extension: " + path);
        }
    }

    private static boolean hasSupportedExtension(String path) {
        String lowerCasePath = path.toLowerCase(Locale.ROOT);

        return SUPPORTED_EXTENSIONS.stream().anyMatch(lowerCasePath::endsWith);
    }

    private static boolean isUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private static boolean isGlob(String value) {
        return value.contains("*") || value.contains("?");
    }
}
