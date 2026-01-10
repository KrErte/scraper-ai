package ee.jobs.aggregator.dto;

import ee.jobs.aggregator.entity.SkillCategory;

/**
 * Oskuse statistika DTO.
 * Sisaldab oskuse nime, kategooriat ja tööpakkumiste arvu.
 */
public record SkillStatsDto(
    String name,
    SkillCategory category,
    Long jobCount
) {
    /**
     * Loo DTO andmebaasi päringu tulemusest.
     */
    public static SkillStatsDto fromQueryResult(Object[] row) {
        return new SkillStatsDto(
            (String) row[0],
            (SkillCategory) row[1],
            (Long) row[2]
        );
    }
}
