package ee.jobs.aggregator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Scraperi konfiguratsioonikllass.
 * Loeb seaded application.yml failist.
 */
@Configuration
@ConfigurationProperties(prefix = "scraper")
public class ScraperConfig {

    // Kas scraper on lubatud
    private boolean enabled = true;

    // Rate limiting millisekundites
    private int rateLimitMs = 2000;

    // Cron väljend ajastatud käivitamiseks
    private String cron = "0 0 */6 * * *";

    // CV.ee IT kategooria URL
    private String cvEeUrl = "https://www.cv.ee/en/search?categories%5B0%5D=INFORMATION_TECHNOLOGY";

    // CV Keskus IT kategooria URL
    private String cvKeskusUrl = "https://www.cvkeskus.ee/toopakkumised-infotehnoloogia-valdkonnas";

    // Getters ja setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRateLimitMs() {
        return rateLimitMs;
    }

    public void setRateLimitMs(int rateLimitMs) {
        this.rateLimitMs = rateLimitMs;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getCvEeUrl() {
        return cvEeUrl;
    }

    public void setCvEeUrl(String cvEeUrl) {
        this.cvEeUrl = cvEeUrl;
    }

    public String getCvKeskusUrl() {
        return cvKeskusUrl;
    }

    public void setCvKeskusUrl(String cvKeskusUrl) {
        this.cvKeskusUrl = cvKeskusUrl;
    }
}
