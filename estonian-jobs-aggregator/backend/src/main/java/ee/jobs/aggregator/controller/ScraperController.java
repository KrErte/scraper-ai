package ee.jobs.aggregator.controller;

import ee.jobs.aggregator.scraper.CvEeScraper;
import ee.jobs.aggregator.scraper.CvKeskusScraper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Scraperi REST kontroller.
 * Võimaldab manuaalselt scrapingut käivitada.
 */
@RestController
@RequestMapping("/api/scraper")
@CrossOrigin(origins = "*")
public class ScraperController {

    private static final Logger log = LoggerFactory.getLogger(ScraperController.class);

    private final CvEeScraper cvEeScraper;
    private final CvKeskusScraper cvKeskusScraper;

    public ScraperController(CvEeScraper cvEeScraper, CvKeskusScraper cvKeskusScraper) {
        this.cvEeScraper = cvEeScraper;
        this.cvKeskusScraper = cvKeskusScraper;
    }

    /**
     * POST /api/scraper/trigger
     * Käivita kõikide allikate scraping manuaalselt.
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerAllScrapers() {
        log.info("POST /api/scraper/trigger - Kõikide allikate scraping käivitatud");

        Map<String, Object> results = new HashMap<>();

        // CV.ee scraping
        CvEeScraper.ScraperResult cvEeResult = cvEeScraper.scrape();
        results.put("cvEe", cvEeResult);

        // CV Keskus scraping
        CvKeskusScraper.ScraperResult cvKeskusResult = cvKeskusScraper.scrape();
        results.put("cvKeskus", cvKeskusResult);

        // Kokkuvõte
        int totalNew = cvEeResult.newJobs() + cvKeskusResult.newJobs();
        int totalUpdated = cvEeResult.updatedJobs() + cvKeskusResult.updatedJobs();
        int totalErrors = cvEeResult.errors() + cvKeskusResult.errors();

        results.put("summary", Map.of(
            "totalNew", totalNew,
            "totalUpdated", totalUpdated,
            "totalErrors", totalErrors
        ));

        log.info("Scraping lõpetatud: {} uut, {} uuendatud, {} viga",
                totalNew, totalUpdated, totalErrors);

        return ResponseEntity.ok(results);
    }

    /**
     * POST /api/scraper/trigger/cv-ee
     * Käivita ainult CV.ee scraping.
     */
    @PostMapping("/trigger/cv-ee")
    public ResponseEntity<CvEeScraper.ScraperResult> triggerCvEeScrape() {
        log.info("POST /api/scraper/trigger/cv-ee - CV.ee scraping käivitatud");

        CvEeScraper.ScraperResult result = cvEeScraper.scrape();

        log.info("CV.ee scraping lõpetatud: {} uut, {} uuendatud, {} viga",
                result.newJobs(), result.updatedJobs(), result.errors());

        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/scraper/trigger/cv-keskus
     * Käivita ainult CV Keskus scraping.
     */
    @PostMapping("/trigger/cv-keskus")
    public ResponseEntity<CvKeskusScraper.ScraperResult> triggerCvKeskusScrape() {
        log.info("POST /api/scraper/trigger/cv-keskus - CV Keskus scraping käivitatud");

        CvKeskusScraper.ScraperResult result = cvKeskusScraper.scrape();

        log.info("CV Keskus scraping lõpetatud: {} uut, {} uuendatud, {} viga",
                result.newJobs(), result.updatedJobs(), result.errors());

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/scraper/status
     * Tagasta scraperi staatus.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "sources", new String[]{"CV.ee", "CV Keskus"},
            "status", "ready"
        ));
    }
}
