package ee.jobs.aggregator.scraper;

import ee.jobs.aggregator.config.ScraperConfig;
import ee.jobs.aggregator.entity.JobPosting;
import ee.jobs.aggregator.entity.JobSource;
import ee.jobs.aggregator.repository.JobPostingRepository;
import ee.jobs.aggregator.service.SkillExtractionService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CV Keskus tööpakkumiste scraper.
 * Kogub IT kategooria tööpakkumisi cvkeskus.ee portaalist.
 */
@Service
public class CvKeskusScraper {

    private static final Logger log = LoggerFactory.getLogger(CvKeskusScraper.class);

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";

    // Täiendavad HTTP päised, mis aitavad vältida blokeerimist
    private static final String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8";
    private static final String ACCEPT_LANGUAGE = "et-EE,et;q=0.9,en-US;q=0.8,en;q=0.7";

    private static final Pattern SALARY_PATTERN = Pattern.compile(
        "(\\d+[\\s,]?\\d*)\\s*[-–]\\s*(\\d+[\\s,]?\\d*)\\s*(€|EUR)?",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SINGLE_SALARY_PATTERN = Pattern.compile(
        "(\\d+[\\s,]?\\d*)\\s*(€|EUR)",
        Pattern.CASE_INSENSITIVE
    );

    private final ScraperConfig config;
    private final JobPostingRepository jobPostingRepository;
    private final SkillExtractionService skillExtractionService;

    public CvKeskusScraper(
            ScraperConfig config,
            JobPostingRepository jobPostingRepository,
            SkillExtractionService skillExtractionService
    ) {
        this.config = config;
        this.jobPostingRepository = jobPostingRepository;
        this.skillExtractionService = skillExtractionService;
    }

    /**
     * Ajastatud scraping - 5 minutit peale CV.ee.
     */
    @Scheduled(cron = "0 5 */6 * * *")
    public void scheduledScrape() {
        if (!config.isEnabled()) {
            log.info("Scraper on keelatud, jätan CV Keskus vahele");
            return;
        }

        log.info("Alustamas ajastatud CV Keskus scrapingut");
        scrape();
    }

    /**
     * Käivita scraping manuaalselt.
     */
    @Transactional
    public ScraperResult scrape() {
        log.info("Alustamas CV Keskus IT tööpakkumiste scrapingut");

        int newJobs = 0;
        int updatedJobs = 0;
        int errors = 0;
        List<String> errorMessages = new ArrayList<>();

        try {
            List<String> jobUrls = scrapeJobListings();
            log.info("CV Keskus: Leiti {} tööpakkumist", jobUrls.size());

            for (String jobUrl : jobUrls) {
                try {
                    Thread.sleep(config.getRateLimitMs());

                    ScrapedJob scrapedJob = scrapeJobDetails(jobUrl);
                    if (scrapedJob != null) {
                        SaveResult result = saveJobPosting(scrapedJob);
                        if (result == SaveResult.NEW) {
                            newJobs++;
                        } else if (result == SaveResult.UPDATED) {
                            updatedJobs++;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Scraping katkestati");
                    break;
                } catch (Exception e) {
                    errors++;
                    String errorMsg = "CV Keskus viga: " + jobUrl + " - " + e.getMessage();
                    errorMessages.add(errorMsg);
                    log.error(errorMsg, e);
                }
            }

        } catch (Exception e) {
            errors++;
            String errorMsg = "CV Keskus nimekirja viga: " + e.getMessage();
            errorMessages.add(errorMsg);
            log.error(errorMsg, e);
        }

        log.info("CV Keskus scraping lõpetatud: {} uut, {} uuendatud, {} viga",
                 newJobs, updatedJobs, errors);

        return new ScraperResult(newJobs, updatedJobs, errors, errorMessages);
    }

    /**
     * Lae kuulutuste nimekiri ja tagasta URL-id.
     */
    private List<String> scrapeJobListings() throws IOException {
        List<String> jobUrls = new ArrayList<>();
        String baseUrl = config.getCvKeskusUrl();
        int page = 1;
        int maxPages = 10;

        while (page <= maxPages) {
            // CV Keskus kasutab /page/X formaati leheküljenduseks
            String url = page == 1 ? baseUrl : baseUrl + "/page/" + page;
            log.info("CV Keskus laadimine: {}", url);

            try {
                Document doc = Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .header("Accept", ACCEPT)
                        .header("Accept-Language", ACCEPT_LANGUAGE)
                        .header("Cache-Control", "no-cache")
                        .timeout(30000)
                        .followRedirects(true)
                        .get();

                // Debug: logi lehe pealkiri, et kontrollida kas leht laeti
                log.info("CV Keskus lehe pealkiri: {}", doc.title());

                // Debug: logi kõik leitud lingid
                Elements allLinks = doc.select("a[href]");
                log.info("CV Keskus: lehel {} leiti {} linki kokku", page, allLinks.size());

                // Leia kõik tööpakkumiste lingid - cvkeskus kasutab /tookoht/ URL-i
                Elements listings = doc.select("a[href*='/tookoht/']");
                log.info("CV Keskus: /tookoht/ linke: {}", listings.size());

                if (listings.isEmpty()) {
                    // Proovi teisi variante
                    listings = doc.select("a[href*='/toopakkumine/'], a[href*='/vacancy/']");
                    log.info("CV Keskus: /toopakkumine/ või /vacancy/ linke: {}", listings.size());
                }

                if (listings.isEmpty()) {
                    // Logi mõned näidislingid debug jaoks
                    int count = 0;
                    for (Element link : allLinks) {
                        String href = link.attr("href");
                        if (href.contains("cvkeskus") && count < 10) {
                            log.info("CV Keskus näidislink: {}", href);
                            count++;
                        }
                    }
                    log.info("CV Keskus lehel {} pole rohkem kuulutusi", page);
                    break;
                }

                for (Element listing : listings) {
                    String href = listing.attr("abs:href");
                    if (href != null && !href.isEmpty() && !jobUrls.contains(href)) {
                        jobUrls.add(href);
                        log.debug("CV Keskus lisatud: {}", href);
                    }
                }

                Thread.sleep(config.getRateLimitMs());
                page++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                log.warn("CV Keskus viga lehe {} laadimisel: {}", page, e.getMessage());
                break;
            }
        }

        return jobUrls;
    }

    /**
     * Scraape üksiku kuulutuse detailid.
     */
    private ScrapedJob scrapeJobDetails(String url) throws IOException {
        log.debug("CV Keskus scrapimas: {}", url);

        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(30000)
                .followRedirects(true)
                .get();

        doc.select("script, style").remove();

        String title = extractTitle(doc);
        if (title == null || title.isEmpty()) {
            log.warn("CV Keskus pealkirja ei leitud: {}", url);
            return null;
        }

        String company = extractCompany(doc);
        String location = extractLocation(doc);
        String description = extractDescription(doc);
        BigDecimal[] salary = extractSalary(doc, description);

        return new ScrapedJob(
            url,
            title,
            company,
            location,
            salary[0],
            salary[1],
            "EUR",
            description,
            LocalDate.now()
        );
    }

    private String extractTitle(Document doc) {
        Element titleEl = doc.selectFirst("h1.vacancy-title, h1.job-title, .vacancy-header h1, h1");
        if (titleEl != null) {
            return titleEl.text().trim();
        }

        String metaTitle = doc.title();
        if (metaTitle != null && !metaTitle.isEmpty()) {
            return metaTitle.replaceAll("\\s*[-|]\\s*CV\\s*Keskus.*", "").trim();
        }

        return null;
    }

    private String extractCompany(Document doc) {
        String[] selectors = {
            ".company-name",
            ".employer-name",
            ".vacancy-company",
            ".company a",
            "[itemprop='hiringOrganization']",
            ".job-company"
        };

        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null && !el.text().isEmpty()) {
                return el.text().trim();
            }
        }

        return "Teadmata";
    }

    private String extractLocation(Document doc) {
        String[] selectors = {
            ".location",
            ".vacancy-location",
            ".job-location",
            "[itemprop='jobLocation']",
            ".address"
        };

        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null && !el.text().isEmpty()) {
                return el.text().trim();
            }
        }

        return "Eesti";
    }

    private String extractDescription(Document doc) {
        String[] selectors = {
            ".vacancy-description",
            ".job-description",
            ".description",
            "[itemprop='description']",
            ".vacancy-content",
            ".job-content"
        };

        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null && !el.text().isEmpty()) {
                return el.text().trim();
            }
        }

        return doc.body() != null ? doc.body().text() : "";
    }

    private BigDecimal[] extractSalary(Document doc, String description) {
        BigDecimal[] result = {null, null};

        String[] selectors = {
            ".salary",
            ".wage",
            ".compensation",
            "[itemprop='baseSalary']",
            ".vacancy-salary"
        };

        String salaryText = null;
        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null && !el.text().isEmpty()) {
                salaryText = el.text();
                break;
            }
        }

        if (salaryText == null) {
            salaryText = description;
        }

        if (salaryText == null) {
            return result;
        }

        Matcher matcher = SALARY_PATTERN.matcher(salaryText);
        if (matcher.find()) {
            try {
                String minStr = matcher.group(1).replaceAll("[\\s,]", "");
                String maxStr = matcher.group(2).replaceAll("[\\s,]", "");
                result[0] = new BigDecimal(minStr);
                result[1] = new BigDecimal(maxStr);
                return result;
            } catch (NumberFormatException e) {
                log.debug("Ei suutnud palka parsida: {}", matcher.group());
            }
        }

        Matcher singleMatcher = SINGLE_SALARY_PATTERN.matcher(salaryText);
        if (singleMatcher.find()) {
            try {
                String valStr = singleMatcher.group(1).replaceAll("[\\s,]", "");
                result[0] = new BigDecimal(valStr);
                result[1] = result[0];
            } catch (NumberFormatException e) {
                log.debug("Ei suutnud palka parsida: {}", singleMatcher.group());
            }
        }

        return result;
    }

    private SaveResult saveJobPosting(ScrapedJob job) {
        Optional<JobPosting> existing = jobPostingRepository.findByUrl(job.url());

        JobPosting entity;
        SaveResult result;

        if (existing.isPresent()) {
            entity = existing.get();
            result = SaveResult.UPDATED;
            log.debug("CV Keskus uuendamas: {}", job.url());
        } else {
            entity = new JobPosting();
            entity.setUrl(job.url());
            result = SaveResult.NEW;
            log.debug("CV Keskus uus kuulutus: {}", job.title());
        }

        entity.setTitle(job.title());
        entity.setCompany(job.company());
        entity.setLocation(job.location());
        entity.setSalaryMin(job.salaryMin());
        entity.setSalaryMax(job.salaryMax());
        entity.setSalaryCurrency(job.salaryCurrency());
        entity.setDescription(job.description());
        entity.setSource(JobSource.CV_KESKUS);
        entity.setPostedDate(job.postedDate());
        entity.setScrapedAt(LocalDateTime.now());

        entity = jobPostingRepository.save(entity);
        skillExtractionService.extractAndSaveSkills(entity);

        return result;
    }

    private record ScrapedJob(
        String url,
        String title,
        String company,
        String location,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String salaryCurrency,
        String description,
        LocalDate postedDate
    ) {}

    private enum SaveResult {
        NEW, UPDATED, SKIPPED
    }

    public record ScraperResult(
        int newJobs,
        int updatedJobs,
        int errors,
        List<String> errorMessages
    ) {}
}
