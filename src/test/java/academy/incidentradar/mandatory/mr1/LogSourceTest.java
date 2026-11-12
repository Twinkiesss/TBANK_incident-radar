package academy.incidentradar.mandatory.mr1;

import static org.assertj.core.api.Assertions.assertThat;

import academy.incidentradar.support.CliRunner;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MR1. Источники логов")
class LogSourceTest {

    private static final Path SAMPLES = Path.of("samples", "logs");
    private static final String LOG_BODY =
            "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET /downloads/product_1 HTTP/1.1\" 304 0 \"-\" \"Debian APT-HTTP/1.3\"\n";

    private HttpServer server;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/log.log", exchange -> {
            byte[] body = LOG_BODY.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.createContext("/missing.log", exchange -> exchange.sendResponseHeaders(404, -1));
        server.start();
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String url(String path) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + path;
    }

    @Test
    @DisplayName("Локальный файл читается")
    void readsLocalFile() {
        var result =
                CliRunner.run("--path", SAMPLES.resolve("access-2025-03-01.log").toString());
        assertThat(result.exitCode()).as(result.toString()).isZero();
    }

    @Test
    @DisplayName("Glob-маска: в отчёт попадают все подошедшие файлы")
    void readsAllFilesMatchingGlob() {
        String glob = SAMPLES.resolve("access-2025-03-0").toString() + "*" + ".log";
        var result = CliRunner.run("--path", glob);
        assertThat(result.exitCode()).as(result.toString()).isZero();
    }

    @Test
    @DisplayName("Лог читается по URL")
    void readsRemoteFile() {
        var result = CliRunner.run("--path", url("/log.log"));
        assertThat(result.exitCode()).as(result.toString()).isZero();
    }

    @Test
    @DisplayName("URL вернул 404 -> exit code 2")
    void remote404ExitsWithTwo() {
        var result = CliRunner.run("--path", url("/missing.log"));
        assertThat(result.exitCode()).as(result.toString()).isEqualTo(2);
        assertThat(result.stderr()).isNotBlank();
    }
}
