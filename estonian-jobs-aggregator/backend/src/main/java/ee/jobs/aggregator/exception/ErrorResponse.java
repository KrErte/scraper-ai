package ee.jobs.aggregator.exception;

import java.time.LocalDateTime;

/**
 * Vea vastuse DTO.
 * Ühtne formaat kõigi API vigade jaoks.
 */
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path
) {
    /**
     * Loo ErrorResponse praeguse ajatempliga.
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(
            LocalDateTime.now(),
            status,
            error,
            message,
            path
        );
    }
}
