package ee.jobs.aggregator.controller;

import ee.jobs.aggregator.scraper.CvEeScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Scraperi REST kontroller.
 * Võimaldab manuaalselt scrapingut käivitada (ainult dev profiilis).
 */
@RestController
@RequestMapping("/api/scraper")
@Profile("dev") // Ainult arenduskeskkonnas
public class ScraperController {

    private static final Logger log = LoggerFactory.getLogger(ScraperController.class);

    private final CvEeScraper cvEeScraper;

    public ScraperController(CvEeScraper cvEeScraper) {
        this.cvEeScraper = cvEeScraper;
    }

    /**
     * POST /api/scraper/trigger
     * Käivita CV.ee scraping manuaalselt.
     */
    @PostMapping("/trigger")
    public ResponseEntity<CvEeScraper.ScraperResult> triggerScrape() {
        log.info("POST /api/scraper/trigger - Manuaalne scraping käivitatud");

        CvEeScraper.ScraperResult result = cvEeScraper.scrape();

        log.info("Scraping lõpetatud: {} uut, {} uuendatud, {} viga",
                result.newJobs(), result.updatedJobs(), result.errors());

        return ResponseEntity.ok(result);
    }
}
