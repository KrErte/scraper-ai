package ee.jobs.aggregator.dto;

import java.math.BigDecimal;

/**
 * Palgastatistika DTO.
 * Sisaldab keskmist, minimaalset ja maksimaalset palka.
 */
public record SalaryStatsDto(
    String skill,
    BigDecimal avgSalary,
    BigDecimal minSalary,
    BigDecimal maxSalary,
    Long jobCount
) {
    /**
     * Loo DTO andmebaasi päringu tulemusest.
     */
    public static SalaryStatsDto fromQueryResult(String skill, Object[] row) {
        return new SalaryStatsDto(
            skill,
            row[0] != null ? BigDecimal.valueOf(((Number) row[0]).doubleValue()) : null,
            row[1] != null ? (BigDecimal) row[1] : null,
            row[2] != null ? (BigDecimal) row[2] : null,
            row[3] != null ? ((Number) row[3]).longValue() : 0L
        );
    }
}
