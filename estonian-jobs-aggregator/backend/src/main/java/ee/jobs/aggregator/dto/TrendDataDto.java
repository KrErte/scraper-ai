package ee.jobs.aggregator.dto;

import java.time.LocalDate;

/**
 * Trendi andmete DTO.
 * Sisaldab kuupäeva ja vastava väärtuse.
 */
public record TrendDataDto(
    LocalDate date,
    Long count
) {
    /**
     * Loo DTO andmebaasi päringu tulemusest.
     */
    public static TrendDataDto fromQueryResult(Object[] row) {
        return new TrendDataDto(
            (LocalDate) row[0],
            ((Number) row[1]).longValue()
        );
    }
}
