package academy.incidentradar.model;

import java.time.OffsetDateTime;

public record LogRecord(
        String remoteAddress,
        String remoteUser,
        OffsetDateTime timestamp,
        String method,
        String resource,
        String protocol,
        int status,
        long bodyBytesSent,
        String referer,
        String userAgent) {}
