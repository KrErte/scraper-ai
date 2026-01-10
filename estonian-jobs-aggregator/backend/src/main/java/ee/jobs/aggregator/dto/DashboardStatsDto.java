package ee.jobs.aggregator.dto;

import java.math.BigDecimal;

/**
 * Dashboard statistika DTO.
 * Koondab üldise statistika esilehekülje jaoks.
 */
public record DashboardStatsDto(
    Long totalJobs,
    Long totalCompanies,
    BigDecimal avgSalary,
    String topSkill,
    Long newJobsToday
) {
}
