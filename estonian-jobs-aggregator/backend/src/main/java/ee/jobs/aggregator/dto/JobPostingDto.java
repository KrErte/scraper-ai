package ee.jobs.aggregator.dto;

import ee.jobs.aggregator.entity.JobPosting;
import ee.jobs.aggregator.entity.JobSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tööpakkumise DTO (Data Transfer Object).
 * Kasutab Java 21 record süntaksit.
 */
public record JobPostingDto(
    Long id,
    String title,
    String company,
    String location,
    BigDecimal salaryMin,
    BigDecimal salaryMax,
    String salaryCurrency,
    String description,
    String url,
    JobSource source,
    LocalDate postedDate,
    LocalDateTime scrapedAt,
    LocalDate expiresAt,
    Set<String> skills
) {
    /**
     * Teisenda entiteet DTO-ks.
     */
    public static JobPostingDto fromEntity(JobPosting entity) {
        return new JobPostingDto(
            entity.getId(),
            entity.getTitle(),
            entity.getCompany(),
            entity.getLocation(),
            entity.getSalaryMin(),
            entity.getSalaryMax(),
            entity.getSalaryCurrency(),
            entity.getDescription(),
            entity.getUrl(),
            entity.getSource(),
            entity.getPostedDate(),
            entity.getScrapedAt(),
            entity.getExpiresAt(),
            entity.getSkills().stream()
                .map(skill -> skill.getName())
                .collect(Collectors.toSet())
        );
    }

    /**
     * Teisenda entiteet DTO-ks ilma kirjelduseta (nimekirja jaoks).
     */
    public static JobPostingDto fromEntityShort(JobPosting entity) {
        return new JobPostingDto(
            entity.getId(),
            entity.getTitle(),
            entity.getCompany(),
            entity.getLocation(),
            entity.getSalaryMin(),
            entity.getSalaryMax(),
            entity.getSalaryCurrency(),
            null, // Kirjeldust nimekirjas pole vaja
            entity.getUrl(),
            entity.getSource(),
            entity.getPostedDate(),
            entity.getScrapedAt(),
            entity.getExpiresAt(),
            entity.getSkills().stream()
                .map(skill -> skill.getName())
                .collect(Collectors.toSet())
        );
    }
}
