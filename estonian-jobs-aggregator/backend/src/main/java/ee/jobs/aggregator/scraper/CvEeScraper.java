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
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CV.ee tööpakkumiste scraper.
 * Kogub IT kategooria tööpakkumisi CV.ee portaalist.
 *
 * Kasutab Java 21 virtual threads rate limiting jaoks.
 */
@Service
public class CvEeScraper {

    private static final Logger log = LoggerFactory.getLogger(CvEeScraper.class);

    // User-Agent päis, et vältida blokeerimist
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    // Regex mustrid palga eraldamiseks
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

    public CvEeScraper(
            ScraperConfig config,
            JobPostingRepository jobPostingRepository,
            SkillExtractionService skillExtractionService
    ) {
        this.config = config;
        this.jobPostingRepository = jobPostingRepository;
        this.skillExtractionService = skillExtractionService;
    }

    /**
     * Ajastatud scraping iga 6 tunni tagant.
     */
    @Scheduled(cron = "${scraper.cron}")
    public void scheduledScrape() {
        if (!config.isEnabled()) {
            log.info("Scraper on keelatud, jätan vahele");
            return;
        }

        log.info("Alustamas ajastatud CV.ee scrapingut");
        scrape();
    }

    /**
     * Käivita scraping manuaalselt.
     * Kasutab virtual threads paralleelseks töötlemiseks.
     */
    @Transactional
    public ScraperResult scrape() {
        log.info("Alustamas CV.ee IT tööpakkumiste scrapingut");

        int newJobs = 0;
        int updatedJobs = 0;
        int errors = 0;
        List<String> errorMessages = new ArrayList<>();

        try {
            // Lae kuulutuste nimekirja leht
            List<String> jobUrls = scrapeJobListings();
            log.info("Leiti {} tööpakkumist", jobUrls.size());

            // Töötle iga kuulutus eraldi (virtual threads rate limiting jaoks)
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (String jobUrl : jobUrls) {
                    try {
                        // Rate limiting - oota enne järgmist päringut
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
                        String errorMsg = "Viga kuulutuse scraapimisel: " + jobUrl + " - " + e.getMessage();
                        errorMessages.add(errorMsg);
                        log.error(errorMsg, e);
                    }
                }
            }

        } catch (Exception e) {
            errors++;
            String errorMsg = "Viga nimekirja laadimisel: " + e.getMessage();
            errorMessages.add(errorMsg);
            log.error(errorMsg, e);
        }

        log.info("CV.ee scraping lõpetatud: {} uut, {} uuendatud, {} viga",
                 newJobs, updatedJobs, errors);

        return new ScraperResult(newJobs, updatedJobs, errors, errorMessages);
    }

    /**
     * Lae kuulutuste nimekiri ja tagasta URL-id.
     */
    private List<String> scrapeJobListings() throws IOException {
        List<String> jobUrls = new ArrayList<>();
        String baseUrl = config.getCvEeUrl();
        int page = 1;
        int maxPages = 10; // Piira lehekülgede arvu

        while (page <= maxPages) {
            // CV.ee kasutab &page= parameetrit otsingu URL-is
            String url = page == 1 ? baseUrl : baseUrl + "&page=" + page;
            log.info("CV.ee laadimine: {}", url);

            try {
                Document doc = Jsoup.connect(url)
                        .userAgent(USER_AGENT)
                        .timeout(30000)
                        .followRedirects(true)
                        .get();

                // Debug: logi kõik leitud lingid
                Elements allLinks = doc.select("a[href]");
                log.info("CV.ee: lehel {} leiti {} linki kokku", page, allLinks.size());

                // CV.ee otsingutulemuste selektorid
                Elements listings = doc.select("a[href*='/vacancy/'], a[href*='/job/']");
                log.info("CV.ee: /vacancy/ või /job/ linke: {}", listings.size());

                if (listings.isEmpty()) {
                    // Proovi alternatiivset selektorit
                    listings = doc.select("a[href*='offer']");
                    log.info("CV.ee: offer linke: {}", listings.size());
                }

                if (listings.isEmpty()) {
                    // Logi mõned näidislingid debug jaoks
                    int count = 0;
                    for (Element link : allLinks) {
                        String href = link.attr("href");
                        if (href.startsWith("/") && !href.contains("login") && !href.contains("register") && count < 10) {
                            log.info("CV.ee näidislink: {}", href);
                            count++;
                        }
                    }
                    log.info("CV.ee lehel {} pole rohkem kuulutusi", page);
                    break;
                }

                for (Element listing : listings) {
                    String href = listing.attr("abs:href");
                    if (href != null && !href.isEmpty()
                        && (href.contains("/vacancy/") || href.contains("/job/") || href.contains("/offer/"))
                        && !jobUrls.contains(href)) {
                        jobUrls.add(href);
                        log.debug("CV.ee lisatud: {}", href);
                    }
                }

                // Rate limiting
                Thread.sleep(config.getRateLimitMs());
                page++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                log.warn("Viga lehe {} laadimisel: {}", page, e.getMessage());
                break;
            }
        }

        return jobUrls;
    }

    /**
     * Scraape üksiku kuulutuse detailid.
     */
    private ScrapedJob scrapeJobDetails(String url) throws IOException {
        log.debug("Scrapimas: {}", url);

        Document doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(30000)
                .get();

        // Eemalda script ja style elemendid
        doc.select("script, style").remove();

        // Parsi pealkiri
        String title = extractTitle(doc);
        if (title == null || title.isEmpty()) {
            log.warn("Pealkirja ei leitud: {}", url);
            return null;
        }

        // Parsi ettevõtte nimi
        String company = extractCompany(doc);

        // Parsi asukoht
        String location = extractLocation(doc);

        // Parsi kirjeldus
        String description = extractDescription(doc);

        // Parsi palk
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

    /**
     * Eralda pealkiri dokumendist.
     */
    private String extractTitle(Document doc) {
        // Proovi erinevaid selektoreid
        Element titleEl = doc.selectFirst("h1.job-title, h1.vacancy-title, h1");
        if (titleEl != null) {
            return titleEl.text().trim();
        }

        // Proovi meta title
        String metaTitle = doc.title();
        if (metaTitle != null && !metaTitle.isEmpty()) {
            // Eemalda CV.ee suffix
            return metaTitle.replaceAll("\\s*[-|]\\s*CV\\.ee.*", "").trim();
        }

        return null;
    }

    /**
     * Eralda ettevõtte nimi.
     */
    private String extractCompany(Document doc) {
        // Proovi erinevaid selektoreid
        String[] selectors = {
            ".company-name",
            ".employer-name",
            "a[href*='company']",
            ".vacancy-company",
            "[itemprop='hiringOrganization']"
        };

        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null && !el.text().isEmpty()) {
                return el.text().trim();
            }
        }

        return "Teadmata";
    }

    /**
     * Eralda asukoht.
     */
    private String extractLocation(Document doc) {
        String[] selectors = {
            ".location",
            ".vacancy-location",
            "[itemprop='jobLocation']",
            ".job-location"
        };

        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null && !el.text().isEmpty()) {
                return el.text().trim();
            }
        }

        // Vaikimisi Tallinn kui asukohta ei leita
        return "Eesti";
    }

    /**
     * Eralda kirjeldus.
     */
    private String extractDescription(Document doc) {
        String[] selectors = {
            ".job-description",
            ".vacancy-description",
            ".description",
            "[itemprop='description']",
            ".content"
        };

        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null && !el.text().isEmpty()) {
                return el.text().trim();
            }
        }

        // Tagasta kogu body tekst kui spetsiifilist kirjeldust ei leia
        return doc.body() != null ? doc.body().text() : "";
    }

    /**
     * Eralda palk dokumendist või kirjeldusest.
     * Tagastab massiivi [min, max].
     */
    private BigDecimal[] extractSalary(Document doc, String description) {
        BigDecimal[] result = {null, null};

        // Proovi leida palga elementi
        String[] selectors = {
            ".salary",
            ".wage",
            ".compensation",
            "[itemprop='baseSalary']"
        };

        String salaryText = null;
        for (String selector : selectors) {
            Element el = doc.selectFirst(selector);
            if (el != null && !el.text().isEmpty()) {
                salaryText = el.text();
                break;
            }
        }

        // Kui elementi ei leita, otsi kirjeldusest
        if (salaryText == null) {
            salaryText = description;
        }

        if (salaryText == null) {
            return result;
        }

        // Proovi leida vahemikku (nt "3000 - 5000 EUR")
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

        // Proovi leida ühte väärtust (nt "3000 EUR")
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

    /**
     * Salvesta või uuenda tööpakkumine andmebaasi.
     */
    private SaveResult saveJobPosting(ScrapedJob job) {
        Optional<JobPosting> existing = jobPostingRepository.findByUrl(job.url());

        JobPosting entity;
        SaveResult result;

        if (existing.isPresent()) {
            // Uuenda olemasolevat
            entity = existing.get();
            result = SaveResult.UPDATED;
            log.debug("Uuendamas olemasolevat kuulutust: {}", job.url());
        } else {
            // Loo uus
            entity = new JobPosting();
            entity.setUrl(job.url());
            result = SaveResult.NEW;
            log.debug("Lisamas uut kuulutust: {}", job.title());
        }

        // Uuenda väljad
        entity.setTitle(job.title());
        entity.setCompany(job.company());
        entity.setLocation(job.location());
        entity.setSalaryMin(job.salaryMin());
        entity.setSalaryMax(job.salaryMax());
        entity.setSalaryCurrency(job.salaryCurrency());
        entity.setDescription(job.description());
        entity.setSource(JobSource.CV_EE);
        entity.setPostedDate(job.postedDate());
        entity.setScrapedAt(LocalDateTime.now());

        // Salvesta
        entity = jobPostingRepository.save(entity);

        // Eralda ja salvesta oskused
        skillExtractionService.extractAndSaveSkills(entity);

        return result;
    }

    /**
     * Scrapetud tööpakkumise record.
     */
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

    /**
     * Salvesta tulemus.
     */
    private enum SaveResult {
        NEW, UPDATED, SKIPPED
    }

    /**
     * Scraperi tulemuse record.
     */
    public record ScraperResult(
        int newJobs,
        int updatedJobs,
        int errors,
        List<String> errorMessages
    ) {}
}
