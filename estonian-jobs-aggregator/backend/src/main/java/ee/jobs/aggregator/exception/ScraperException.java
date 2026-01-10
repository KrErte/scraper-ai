package ee.jobs.aggregator.exception;

/**
 * Erand scraperi vigade korral.
 */
public class ScraperException extends RuntimeException {

    private final String url;

    public ScraperException(String message) {
        super(message);
        this.url = null;
    }

    public ScraperException(String message, String url) {
        super(message);
        this.url = url;
    }

    public ScraperException(String message, Throwable cause) {
        super(message, cause);
        this.url = null;
    }

    public ScraperException(String message, String url, Throwable cause) {
        super(message, cause);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
